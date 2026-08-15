package com.example.motortrackeryamaha

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.motortrackeryamaha.data.AppDatabase
import com.example.motortrackeryamaha.data.AppSettings
import com.example.motortrackeryamaha.databinding.ActivityLocationSettingsBinding
import com.example.motortrackeryamaha.databinding.ItemSettingsRowBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class LocationSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLocationSettingsBinding
    private var currentAddressName: String = "Mencari lokasi..."
    private var lastGeocodedLocation: Location? = null
    private var lastKnownStats: LocationTrackingService.TrackingStats? = null
    private var timeUpdateJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocationSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupMenu()
        observeTrackingService()
        ensureServiceRunning()
        startTimeUpdater()
    }

    private fun ensureServiceRunning() {
        if (!LocationTrackingService.isServiceRunning) {
            val intent = Intent(this, LocationTrackingService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }

    private fun setupMenu() {
        lifecycleScope.launch {
            val settings = AppDatabase.getDatabase(this@LocationSettingsActivity).appSettingsDao().getSettings().first()
            settings?.let {
                updateSettingsUI(it)
            }
        }

        binding.btnTestGps.setOnClickListener { testGps() }
    }

    private fun updateSettingsUI(settings: AppSettings) {
        setupRow(binding.rowUpdateInterval, R.drawable.ic_history, "Interval Pembaruan", "${settings.gpsIntervalSeconds} Detik", "#FFC107") {
            showIntervalDialog(settings)
        }
        setupRow(binding.rowTrackingAccuracy, R.drawable.ic_build, "Akurasi Tracking", settings.gpsAccuracy, "#00BCD4") {
            showAccuracyDialog(settings)
        }
        setupRow(binding.rowAutoTracking, R.drawable.ic_trip, "Tracking Otomatis", if (settings.autoTrackingEnabled) "Aktif" else "Nonaktif", if (settings.autoTrackingEnabled) "#4CAF50" else "#757575") {
            toggleAutoTracking(settings)
        }
        val radiusText = if (settings.safetyRadiusMeters >= 1000) "${settings.safetyRadiusMeters / 1000.0} KM" else "${settings.safetyRadiusMeters} meter"
        setupRow(binding.rowSafetyRadius, R.drawable.ic_location, "Radius Aman", radiusText, "#F44336") {
            showRadiusDialog(settings)
        }
    }

    private fun observeTrackingService() {
        lifecycleScope.launch {
            LocationTrackingService.gpsStatus.collect { status ->
                updateGpsStatusUI(status)
            }
        }

        lifecycleScope.launch {
            LocationTrackingService.trackingStats.collect { stats ->
                lastKnownStats = stats
                updateLocationDetailsUI(stats)
                checkSafetyRadius(stats)
            }
        }
    }

    private fun updateGpsStatusUI(status: LocationTrackingService.GpsStatus) {
        val (text, colorRes) = when (status) {
            LocationTrackingService.GpsStatus.AKTIF -> "● GPS AKTIF" to R.color.status_safe
            LocationTrackingService.GpsStatus.MENCARI_LOKASI -> "● MENCARI GPS..." to R.color.status_warning
            LocationTrackingService.GpsStatus.NONAKTIF -> "○ GPS TIDAK AKTIF" to R.color.status_danger
        }
        binding.tvGpsStatus.text = text
        binding.tvGpsStatus.setTextColor(ContextCompat.getColor(this, colorRes))
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            binding.tvGpsInfo.text = "IZIN LOKASI DIPERLUKAN"
        } else {
            binding.tvGpsInfo.text = if (status == LocationTrackingService.GpsStatus.AKTIF) "Sinyal Kuat | Terhubung" else "Menunggu Sinyal Satelit..."
        }
    }

    private fun updateLocationDetailsUI(stats: LocationTrackingService.TrackingStats) {
        val lat = stats.currentLat
        val lng = stats.currentLng
        if (lat != null && lng != null) {
            val coordStr = String.format(Locale.getDefault(), "%.6f, %.6f", lat, lng)
            val accuracyStr = if (stats.accuracy != null) " (±${stats.accuracy.toInt()}m)" else ""
            val updateTime = if (stats.lastUpdateTime > 0) " | ${getRelativeTime(stats.lastUpdateTime)}" else ""
            
            setupRow(binding.rowDefaultLoc, R.drawable.ic_location, "Latitude / Longitude", coordStr + accuracyStr + updateTime, "#2196F3") {
                showCoordinatesDialog(stats)
            }

            // Only perform reverse geocoding if location moved significantly (> 50 meters)
            val currentLoc = Location("").apply { latitude = lat; longitude = lng }
            if (lastGeocodedLocation == null || lastGeocodedLocation!!.distanceTo(currentLoc) > 50) {
                lastGeocodedLocation = currentLoc
                performReverseGeocoding(lat, lng, stats)
            } else {
                updateAddressRow(currentAddressName, stats)
            }
        } else {
            setupRow(binding.rowCurrentLoc, R.drawable.ic_location, "Lokasi Saat Ini", "Mencari lokasi...", "#4CAF50")
            setupRow(binding.rowDefaultLoc, R.drawable.ic_location, "Latitude / Longitude", "Menunggu data...", "#2196F3")
        }
    }

    private fun performReverseGeocoding(lat: Double, lng: Double, stats: LocationTrackingService.TrackingStats) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(this@LocationSettingsActivity, Locale.getDefault())
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val subLoc = address.subLocality ?: ""
                    val locality = address.locality ?: ""
                    val name = if (subLoc.isNotEmpty()) "$subLoc, $locality" else locality
                    currentAddressName = if (name.isNotEmpty()) name else "Alamat tidak dikenal"
                    withContext(Dispatchers.Main) {
                        updateAddressRow(currentAddressName, stats)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    currentAddressName = "Alamat tidak tersedia"
                    updateAddressRow(currentAddressName, stats)
                }
            }
        }
    }

    private fun updateAddressRow(address: String, stats: LocationTrackingService.TrackingStats) {
        setupRow(binding.rowCurrentLoc, R.drawable.ic_location, "Lokasi Saat Ini", address, "#4CAF50") {
            showLocationDetailDialog(stats, address)
        }
    }

    private fun checkSafetyRadius(stats: LocationTrackingService.TrackingStats) {
        lifecycleScope.launch {
            val settings = AppDatabase.getDatabase(this@LocationSettingsActivity).appSettingsDao().getSettings().first()
            if (settings != null && settings.referenceLat != null && settings.referenceLng != null && stats.currentLat != null && stats.currentLng != null) {
                val results = FloatArray(1)
                Location.distanceBetween(settings.referenceLat, settings.referenceLng, stats.currentLat, stats.currentLng, results)
                val distance = results[0]
                val isSafe = distance <= settings.safetyRadiusMeters
                val statusText = if (isSafe) "🟢 AMAN (${String.format(Locale.getDefault(), "%.0f", distance)}m)" else "🔴 DI LUAR RADIUS (${String.format(Locale.getDefault(), "%.1f", distance/1000.0)} KM)"
                binding.rowSafetyRadius.tvSubLabel.text = "Status: $statusText"
            }
        }
    }

    private fun startTimeUpdater() {
        timeUpdateJob?.cancel()
        timeUpdateJob = lifecycleScope.launch {
            while (true) {
                lastKnownStats?.let { updateLocationDetailsUI(it) }
                delay(10000) // Update relative time every 10 seconds
            }
        }
    }

    private fun getRelativeTime(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        return when {
            diff < 10000 -> "Baru saja"
            diff < 60000 -> "${diff / 1000} detik lalu"
            diff < 3600000 -> "${diff / 60000} menit lalu"
            else -> SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
        }
    }

    private fun testGps() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Izin lokasi diperlukan untuk menguji GPS", Toast.LENGTH_SHORT).show()
            return
        }
        lastKnownStats?.let { stats ->
            showLocationDetailDialog(stats, currentAddressName, "Hasil Pengujian GPS")
        } ?: run {
            Toast.makeText(this, "Sinyal GPS belum didapat. Mencoba mencari...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLocationDetailDialog(stats: LocationTrackingService.TrackingStats, address: String, title: String = "Detail Lokasi Real-time") {
        val lastUpdate = if (stats.lastUpdateTime > 0) SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(stats.lastUpdateTime)) else "Belum diperbarui"
        val message = """
            📍 $address
            
            Latitude: ${stats.currentLat ?: "-"}
            Longitude: ${stats.currentLng ?: "-"}
            Akurasi: ${if (stats.accuracy != null) "±${stats.accuracy.toInt()} meter" else "-"}
            Waktu: $lastUpdate
        """.trimIndent()
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setNeutralButton("SET LOKASI AMAN") { _, _ -> setReferenceLocation(stats) }
            .show()
    }

    private fun setReferenceLocation(stats: LocationTrackingService.TrackingStats) {
        if (stats.currentLat == null || stats.currentLng == null) return
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@LocationSettingsActivity)
            val settings = db.appSettingsDao().getSettings().first()
            settings?.let {
                db.appSettingsDao().updateSettings(it.copy(referenceLat = stats.currentLat, referenceLng = stats.currentLng))
                Toast.makeText(this@LocationSettingsActivity, "Lokasi referensi berhasil disimpan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showCoordinatesDialog(stats: LocationTrackingService.TrackingStats) {
        val lat = stats.currentLat ?: 0.0
        val lng = stats.currentLng ?: 0.0
        AlertDialog.Builder(this).setTitle("Koordinat GPS").setMessage("Lat: $lat\nLng: $lng").setPositiveButton("SALIN KOORDINAT") { _, _ ->
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("GPS Coordinates", "$lat, $lng"))
            Toast.makeText(this, "Koordinat disalin ke clipboard", Toast.LENGTH_SHORT).show()
        }.setNegativeButton("OK", null).show()
    }

    private fun showIntervalDialog(settings: AppSettings) {
        val options = arrayOf("1 detik", "3 detik", "5 detik", "10 detik", "15 detik", "30 detik")
        val values = intArrayOf(1, 3, 5, 10, 15, 30)
        val selectedIdx = values.indexOf(settings.gpsIntervalSeconds).coerceAtLeast(2)
        AlertDialog.Builder(this).setTitle("Pilih Interval Pembaruan").setSingleChoiceItems(options, selectedIdx) { dialog, which ->
            saveSetting { it.copy(gpsIntervalSeconds = values[which]) }
            dialog.dismiss()
        }.show()
    }

    private fun showAccuracyDialog(settings: AppSettings) {
        val options = arrayOf("Rendah (Hemat Baterai)", "Sedang (Seimbang)", "Tinggi (Presisi)")
        val values = arrayOf("Rendah", "Sedang", "Tinggi")
        val selectedIdx = values.indexOf(settings.gpsAccuracy).coerceAtLeast(2)
        AlertDialog.Builder(this).setTitle("Pilih Akurasi Tracking").setSingleChoiceItems(options, selectedIdx) { dialog, which ->
            saveSetting { it.copy(gpsAccuracy = values[which]) }
            dialog.dismiss()
        }.show()
    }

    private fun showRadiusDialog(settings: AppSettings) {
        val options = arrayOf("100 meter", "250 meter", "500 meter", "1 KM", "2 KM", "5 KM", "10 KM")
        val values = intArrayOf(100, 250, 500, 1000, 2000, 5000, 10000)
        val selectedIdx = values.indexOf(settings.safetyRadiusMeters).coerceAtLeast(3)
        AlertDialog.Builder(this).setTitle("Pilih Radius Aman").setSingleChoiceItems(options, selectedIdx) { dialog, which ->
            saveSetting { it.copy(safetyRadiusMeters = values[which]) }
            dialog.dismiss()
        }.show()
    }

    private fun toggleAutoTracking(settings: AppSettings) {
        val enable = !settings.autoTrackingEnabled
        saveSetting { it.copy(autoTrackingEnabled = enable) }
        Toast.makeText(this, "Tracking otomatis " + (if (enable) "diaktifkan" else "dimatikan"), Toast.LENGTH_SHORT).show()
    }

    private fun saveSetting(transform: (AppSettings) -> AppSettings) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@LocationSettingsActivity)
            val settings = db.appSettingsDao().getSettings().first()
            settings?.let {
                val newSettings = transform(it)
                db.appSettingsDao().updateSettings(newSettings)
                updateSettingsUI(newSettings)
            }
        }
    }

    private fun setupRow(rowBinding: ItemSettingsRowBinding, iconRes: Int, label: String, subLabel: String, colorHex: String, onClick: (() -> Unit)? = null) {
        rowBinding.ivIcon.setImageResource(iconRes)
        val color = android.graphics.Color.parseColor(colorHex)
        rowBinding.cardIconContainer.setCardBackgroundColor(color.withAlpha(30))
        rowBinding.ivIcon.imageTintList = android.content.res.ColorStateList.valueOf(color)
        rowBinding.tvLabel.text = label
        rowBinding.tvSubLabel.text = subLabel
        rowBinding.root.setOnClickListener { onClick?.invoke() }
    }

    private fun Int.withAlpha(alpha: Int): Int = (this and 0x00FFFFFF) or (alpha shl 24)

    override fun onDestroy() {
        super.onDestroy()
        timeUpdateJob?.cancel()
    }
}
