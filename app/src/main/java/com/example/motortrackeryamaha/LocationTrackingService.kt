package com.example.motortrackeryamaha

import android.app.*
import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.motortrackeryamaha.data.AppDatabase
import com.example.motortrackeryamaha.data.Trip
import com.example.motortrackeryamaha.data.TripPoint
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.util.*

class LocationTrackingService : Service() {

    companion object {
        private const val TAG = "LocationTrackingService"
        
        private val _trackingStats = MutableStateFlow(TrackingStats())
        val trackingStats: StateFlow<TrackingStats> = _trackingStats.asStateFlow()
        
        private val _gpsStatus = MutableStateFlow(GpsStatus.NONAKTIF)
        val gpsStatus: StateFlow<GpsStatus> = _gpsStatus.asStateFlow()

        var isServiceRunning = false
            private set
            
        const val ACTION_STOP_TRIP = "ACTION_STOP_TRIP"
        const val ACTION_START_TRIP = "ACTION_START_TRIP"
    }

    data class TrackingStats(
        val isTripActive: Boolean = false,
        val currentDistanceMeters: Double = 0.0,
        val currentSpeedKmH: Double = 0.0,
        val avgSpeedKmH: Double = 0.0,
        val maxSpeedKmH: Double = 0.0,
        val startTime: Long = 0,
        val tripId: Int? = null,
        val statusText: String = "IDLE",
        val currentLat: Double? = null,
        val currentLng: Double? = null,
        val accuracy: Float? = null,
        val lastUpdateTime: Long = 0,
        val startTimeActual: Long = 0,
        val endTimeActual: Long = 0,
        val startPointName: String = "Mencari lokasi...",
        val currentPointName: String = "Mencari lokasi...",
        val endPointName: String = ""
    )

    enum class GpsStatus {
        AKTIF, MENCARI_LOKASI, NONAKTIF
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var db: AppDatabase
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var settingsJob: Job? = null

    private var currentTripId: Int? = null
    private var lastValidLocation: Location? = null
    private var totalDistanceMeters = 0.0
    private var startTime: Long = 0
    private var maxSpeedMps: Float = 0f
    private var speedSumMps: Double = 0.0
    private var speedCount: Int = 0
    private var startPointName: String = "Mendeteksi..."

    // Auto-detection logic
    private var consecutiveMovingUpdates = 0
    private val AUTO_START_THRESHOLD = 3 // 3 consecutive updates > 5 km/h
    private var lastMoveTime: Long = 0
    private val AUTO_STOP_TIMEOUT_MILLIS = 10 * 60 * 1000L // 10 minutes
    
    private var hasSentSafetyNotification = false
    private var currentPointName: String = "Mencari lokasi..."
    private var lastGeocodeTime: Long = 0

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.getDatabase(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupLocationCallback()
        observeSettings()
    }

    private fun observeSettings() {
        settingsJob?.cancel()
        settingsJob = serviceScope.launch {
            db.appSettingsDao().getSettings().collect { settings ->
                if (isServiceRunning) {
                    requestLocationUpdates()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isServiceRunning) {
            startForegroundService()
            requestLocationUpdates()
            isServiceRunning = true
            _gpsStatus.value = GpsStatus.MENCARI_LOKASI
            
            serviceScope.launch {
                val activeTrip = db.tripDao().getActiveTrip()
                if (activeTrip != null) {
                    currentTripId = activeTrip.id
                    startTime = activeTrip.tanggal
                    totalDistanceMeters = activeTrip.jarak * 1000.0
                    maxSpeedMps = (activeTrip.maxSpeed / 3.6).toFloat()
                    startPointName = activeTrip.titikAwal
                    updateStatsUI("Melanjutkan Perjalanan")
                }
            }
        }

        when (intent?.action) {
            ACTION_STOP_TRIP -> handleManualStop()
            ACTION_START_TRIP -> handleManualStart()
        }
        
        return START_STICKY
    }

    private fun handleManualStop() {
        serviceScope.launch {
            Log.d(TAG, "Manual Stop Requested")
            val settings = db.appSettingsDao().getSettings().first()
            settings?.let {
                db.appSettingsDao().updateSettings(it.copy(manualStop = true))
            }
            
            val lastLoc = lastValidLocation
            if (lastLoc != null) {
                stopAndSaveTrip(lastLoc, "Selesai Manual")
            } else {
                currentTripId = null
                _trackingStats.value = TrackingStats(statusText = "IDLE")
            }
        }
    }

    private fun handleManualStart() {
        serviceScope.launch {
            Log.d(TAG, "Manual Start Requested")
            val settings = db.appSettingsDao().getSettings().first()
            settings?.let {
                db.appSettingsDao().updateSettings(it.copy(manualStop = false))
            }
            
            val activeTrip = db.tripDao().getActiveTrip()
            if (activeTrip == null) {
                lastValidLocation?.let {
                    startNewTrip(it)
                } ?: run {
                    _trackingStats.value = _trackingStats.value.copy(statusText = "Menunggu GPS...")
                }
            }
        }
    }

    private fun startForegroundService() {
        val channelId = "tracking_channel"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Location Tracking", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("MotorTracker")
            .setContentText("Layanan GPS Aktif")
            .setSmallIcon(R.drawable.ic_trip)
            .build()

        startForeground(1, notification)
    }

    private fun requestLocationUpdates() {
        serviceScope.launch {
            val settings = db.appSettingsDao().getSettings().first()
            val interval = (settings?.gpsIntervalSeconds ?: 5) * 1000L
            val accuracy = when (settings?.gpsAccuracy ?: "Tinggi") {
                "Rendah" -> Priority.PRIORITY_BALANCED_POWER_ACCURACY
                "Sedang" -> Priority.PRIORITY_HIGH_ACCURACY
                else -> Priority.PRIORITY_HIGH_ACCURACY
            }
            
            val request = LocationRequest.Builder(accuracy, interval)
                .setMinUpdateIntervalMillis(interval / 2)
                .build()

            try {
                fusedLocationClient.removeLocationUpdates(locationCallback)
                fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
            } catch (e: SecurityException) {
                Log.e(TAG, "Location permission missing", e)
                _gpsStatus.value = GpsStatus.NONAKTIF
            }
        }
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                if (location.accuracy > 50) {
                    Log.d(TAG, "Location ignored due to poor accuracy: ${location.accuracy}")
                    return
                }

                _gpsStatus.value = GpsStatus.AKTIF
                processLocation(location)
                checkSafetyRadius(location)
            }

            override fun onLocationAvailability(availability: LocationAvailability) {
                _gpsStatus.value = if (availability.isLocationAvailable) GpsStatus.AKTIF else GpsStatus.MENCARI_LOKASI
            }
        }
    }

    private fun checkSafetyRadius(location: Location) {
        serviceScope.launch {
            val settings = db.appSettingsDao().getSettings().first()
            if (settings != null && settings.referenceLat != null && settings.referenceLng != null) {
                val results = FloatArray(1)
                Location.distanceBetween(settings.referenceLat!!, settings.referenceLng!!, location.latitude, location.longitude, results)
                val distance = results[0]
                
                if (distance > settings.safetyRadiusMeters) {
                    if (!hasSentSafetyNotification) {
                        updateNotification("⚠️ Keluar dari radius aman! Jarak: ${String.format(Locale.getDefault(), "%.1f", distance/1000.0)} KM")
                        hasSentSafetyNotification = true
                    }
                } else {
                    hasSentSafetyNotification = false
                }
            }
        }
    }

    private fun processLocation(location: Location) {
        val speedKmH = if (location.hasSpeed()) location.speed * 3.6 else 0.0

        // Update current address periodically
        val now = System.currentTimeMillis()
        if (now - lastGeocodeTime > 30000) { // Every 30 seconds
            serviceScope.launch {
                val newAddress = performGeocoding(location)
                if (newAddress != null) {
                    currentPointName = newAddress
                    lastGeocodeTime = now
                    if (currentTripId == null) {
                         updateIdleStats(location, speedKmH)
                    }
                }
            }
        }

        if (currentTripId == null) {
            serviceScope.launch {
                val settings = db.appSettingsDao().getSettings().first()
                val isManualStop = settings?.manualStop ?: false

                if (!isManualStop) {
                    if (speedKmH > 5.0) {
                        consecutiveMovingUpdates++
                        if (consecutiveMovingUpdates >= AUTO_START_THRESHOLD) {
                            startNewTrip(location)
                        }
                    } else {
                        consecutiveMovingUpdates = 0
                    }
                }
                
                if (currentTripId == null) {
                    updateIdleStats(location, speedKmH)
                }
            }
        } else {
            if (lastValidLocation != null) {
                val distance = lastValidLocation!!.distanceTo(location).toDouble()
                if (distance > 3.0) {
                    totalDistanceMeters += distance
                    if (location.speed > maxSpeedMps) maxSpeedMps = location.speed
                    speedSumMps += location.speed
                    speedCount++
                    updateNotification("Perjalanan aktif: ${String.format(Locale.getDefault(), "%.1f", totalDistanceMeters / 1000.0)} KM")
                }
            }
            
            recordPoint(location)

            if (speedKmH > 2.0) {
                lastMoveTime = System.currentTimeMillis()
            }

            val timeSinceLastMove = System.currentTimeMillis() - lastMoveTime
            if (timeSinceLastMove >= AUTO_STOP_TIMEOUT_MILLIS) {
                stopAndSaveTrip(location, "Selesai Otomatis")
            } else {
                updateStatsUI(if (speedKmH <= 2.0) "Berhenti Sementara" else "Perjalanan Aktif", speedKmH)
            }
        }
        lastValidLocation = location
    }

    private fun updateIdleStats(location: Location, speedKmH: Double) {
        serviceScope.launch {
            val settings = db.appSettingsDao().getSettings().first()
            val isManualStop = settings?.manualStop ?: false
            _trackingStats.value = TrackingStats(
                isTripActive = false,
                currentSpeedKmH = speedKmH,
                statusText = if (isManualStop) "IDLE (Manual Stop)" else "Mendeteksi...",
                currentLat = location.latitude,
                currentLng = location.longitude,
                accuracy = location.accuracy,
                lastUpdateTime = System.currentTimeMillis(),
                currentPointName = currentPointName
            )
        }
    }

    private fun updateStatsUI(status: String, currentSpeed: Double = 0.0) {
        val avgSpeed = (if (speedCount > 0) speedSumMps / speedCount.toDouble() else 0.0) * 3.6
        _trackingStats.value = TrackingStats(
            isTripActive = true,
            currentDistanceMeters = totalDistanceMeters,
            currentSpeedKmH = currentSpeed,
            avgSpeedKmH = avgSpeed,
            maxSpeedKmH = maxSpeedMps * 3.6.toDouble(),
            startTime = startTime,
            startTimeActual = startTime,
            tripId = currentTripId,
            statusText = status,
            currentLat = lastValidLocation?.latitude,
            currentLng = lastValidLocation?.longitude,
            accuracy = lastValidLocation?.accuracy,
            lastUpdateTime = System.currentTimeMillis(),
            startPointName = startPointName,
            currentPointName = currentPointName
        )
    }

    private fun startNewTrip(location: Location) {
        if (currentTripId != null) return

        Log.d(TAG, "Starting new trip")
        startTime = System.currentTimeMillis()
        totalDistanceMeters = 0.0
        maxSpeedMps = location.speed
        speedSumMps = location.speed.toDouble()
        speedCount = 1
        lastValidLocation = location
        consecutiveMovingUpdates = 0
        lastMoveTime = System.currentTimeMillis()
        startPointName = "Mendeteksi..."

        serviceScope.launch {
            startPointName = performGeocoding(location) ?: "Titik Awal"

            val trip = Trip(
                tanggal = startTime,
                jarak = 0.0,
                durasi = 0,
                titikAwal = startPointName,
                startLat = location.latitude,
                startLng = location.longitude,
                status = "ACTIVE"
            )
            val id = db.tripDao().insertTripWithId(trip).toInt()
            currentTripId = id
            
            savePoint(id, location)
            updateNotification("Perjalanan aktif: 0,0 KM")
            updateStatsUI("Perjalanan Aktif", location.speed * 3.6)
        }
    }

    private suspend fun performGeocoding(location: Location): String? = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(this@LocationTrackingService, Locale.getDefault())
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val subLoc = address.subLocality ?: ""
                val locality = address.locality ?: ""
                return@withContext if (subLoc.isNotEmpty()) "$subLoc, $locality" else locality
            }
        } catch (e: Exception) {
            Log.e(TAG, "Geocoding failed", e)
        }
        null
    }

    private fun recordPoint(location: Location) {
        currentTripId?.let { id ->
            savePoint(id, location)
        }
    }

    private suspend fun updateTotalOdometer(distanceKm: Double) {
        val settings = db.appSettingsDao().getSettings().first()
        settings?.let {
            val isFirstTrip = it.totalTrackedDistance == 0.0
            val newTotalDist = it.totalTrackedDistance + distanceKm
            val newOilDist = it.distanceSinceOilChange + distanceKm
            val newStartDate = if (isFirstTrip && distanceKm > 0) System.currentTimeMillis() else it.trackingStartDate
            
            db.appSettingsDao().updateSettings(it.copy(
                totalTrackedDistance = newTotalDist,
                distanceSinceOilChange = newOilDist,
                trackingStartDate = newStartDate
            ))
        }

        // Update MotorOdometer
        val odometer = db.odometerDao().getOdometer().first() ?: com.example.motortrackeryamaha.data.MotorOdometer()
        val newDigital = odometer.odometerDigital + distanceKm
        val newDigitalSince = odometer.odometerDigitalSince ?: System.currentTimeMillis()
        db.odometerDao().insertOdometer(odometer.copy(
            odometerDigital = newDigital,
            odometerDigitalSince = newDigitalSince,
            lastUpdated = System.currentTimeMillis()
        ))
    }

    private fun savePoint(tripId: Int, location: Location) {
        serviceScope.launch {
            val point = TripPoint(
                tripId = tripId,
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = location.accuracy,
                speed = location.speed,
                bearing = location.bearing,
                altitude = location.altitude,
                timestamp = location.time
            )
            db.tripPointDao().insertPoint(point)
        }
    }

    private fun stopAndSaveTrip(location: Location, endReason: String) {
        val tripId = currentTripId ?: return
        Log.d(TAG, "Stopping trip: $tripId ($endReason)")
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        val avgSpeed = (if (speedCount > 0) speedSumMps / speedCount.toDouble() else 0.0) * 3.6
        val maxSpeed = maxSpeedMps * 3.6

        serviceScope.launch {
            val endPointName = performGeocoding(location) ?: endReason

            db.tripDao().updateTripStatus(
                id = tripId,
                status = "COMPLETED",
                endLat = location.latitude,
                endLng = location.longitude,
                endPoint = endPointName,
                endTime = endTime,
                distance = totalDistanceMeters / 1000.0,
                duration = duration,
                avg = avgSpeed,
                max = maxSpeed
            )
            
            // NEW LOGIC: Update odometer ONCE at the end
            updateTotalOdometer(totalDistanceMeters / 1000.0)

            _trackingStats.value = TrackingStats(
                isTripActive = false,
                currentDistanceMeters = totalDistanceMeters,
                avgSpeedKmH = avgSpeed,
                maxSpeedKmH = maxSpeed.toDouble(),
                startTime = startTime,
                startTimeActual = startTime,
                endTimeActual = endTime,
                statusText = "PERJALANAN SELESAI",
                tripId = tripId,
                startPointName = startPointName,
                endPointName = endPointName
            )
            
            currentTripId = null
            lastValidLocation = null
            totalDistanceMeters = 0.0
            speedSumMps = 0.0
            speedCount = 0
            maxSpeedMps = 0f
            
            updateNotification("Mendeteksi pergerakan motor...")
        }
    }

    private fun updateNotification(text: String) {
        val notification = NotificationCompat.Builder(this, "tracking_channel")
            .setContentTitle("MotorTracker")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_trip)
            .setOngoing(true)
            .build()
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(1, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "Service Destroyed")
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        settingsJob?.cancel()
        serviceScope.cancel()
        isServiceRunning = false
        _gpsStatus.value = GpsStatus.NONAKTIF
    }
}
