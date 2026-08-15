package com.example.motortrackeryamaha

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.motortrackeryamaha.data.AppDatabase
import com.example.motortrackeryamaha.data.AppSettings
import com.example.motortrackeryamaha.data.Motor
import com.example.motortrackeryamaha.data.OilChange
import com.example.motortrackeryamaha.databinding.ActivityMainBinding
import com.example.motortrackeryamaha.ui.settings.SettingsActivity
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db: AppDatabase

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            startTrackingService()
        } else {
            Toast.makeText(this, "Permission lokasi diperlukan untuk tracking perjalanan.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        db = AppDatabase.getDatabase(this)
        
        setupListeners()
        observeData()
        observeTrackingService()
        ensureInitialData()
        checkPermissions()
        startAutoTrackingIfEnabled()
    }

    private fun startAutoTrackingIfEnabled() {
        lifecycleScope.launch {
            val settings = db.appSettingsDao().getSettings().first()
            if (settings?.autoTrackingEnabled == true) {
                startTrackingService()
            }
        }
    }

    private fun setupListeners() {
        binding.btnStartTrip.setOnClickListener {
            handleStartTripClick()
        }

        binding.ivNotifications.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        // Shortcut Menus
        binding.menuTrip.setOnClickListener { startActivity(Intent(this, TrackingActivity::class.java)) }
        binding.menuOil.setOnClickListener { startActivity(Intent(this, MaintenanceActivity::class.java)) }
        binding.menuService.setOnClickListener { startActivity(Intent(this, HistoryActivity::class.java)) }
        binding.menuHistory.setOnClickListener { startActivity(Intent(this, HistoryActivity::class.java)) }
        binding.menuStats.setOnClickListener { startActivity(Intent(this, StatisticsActivity::class.java)) }
        binding.menuMotor.setOnClickListener { startActivity(Intent(this, MotorProfileActivity::class.java)) }

        binding.btnSeeStats.setOnClickListener {
            startActivity(Intent(this, StatisticsActivity::class.java))
        }

        binding.bottomNavigation.selectedItemId = R.id.nav_home
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_trip -> {
                    startActivity(Intent(this, TrackingActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_maintenance -> {
                    startActivity(Intent(this, MaintenanceActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun handleStartTripClick() {
        if (LocationTrackingService.trackingStats.value.isTripActive) {
            Toast.makeText(this, "Perjalanan sudah aktif.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, TrackingActivity::class.java))
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        } else {
            startTrackingService()
            startActivity(Intent(this, TrackingActivity::class.java))
        }
    }

    private fun startTrackingService() {
        val intent = Intent(this, LocationTrackingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
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
                updateTrackingStatsUI(stats)
            }
        }
    }

    private fun updateGpsStatusUI(status: LocationTrackingService.GpsStatus) {
        // In the absence of a dedicated GPS status view in the current layout,
        // we can use a Toast or log, or update the main button subtext.
        // Based on activity_main.xml relative layout, there's no obvious indicator.
    }

    private var currentAppSettings: AppSettings? = null

    private fun updateTrackingStatsUI(stats: LocationTrackingService.TrackingStats) {
        if (stats.isTripActive) {
            binding.btnStartTrip.text = "● PERJALANAN AKTIF"
            binding.btnStartTrip.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.status_safe))
            
            // Real-time odometer update on dashboard
            currentAppSettings?.let { settings ->
                val activeDistanceKm = stats.currentDistanceMeters / 1000.0
                val totalOdometer = settings.totalTrackedDistance + activeDistanceKm
                binding.tvTotalDistance.text = String.format(Locale.getDefault(), "%,.1f KM", totalOdometer).replace(",", ".")
                
                // Also update oil status real-time
                val totalOilDist = settings.distanceSinceOilChange + activeDistanceKm
                val intervalKm = settings.oilIntervalKm
                val progressPercent = ((totalOilDist / intervalKm) * 100).toInt().coerceIn(0, 100)
                
                binding.tvOilKmLabel.text = String.format(Locale.getDefault(), "%,.1f / %,.0f KM", totalOilDist, intervalKm).replace(",", ".")
                binding.oilProgressBar.progress = progressPercent
                binding.tvOilPercent.text = "$progressPercent%"
                
                val remainingKm = (intervalKm - totalOilDist).coerceAtLeast(0.0)
                binding.tvOilRemainingKm.text = String.format(Locale.getDefault(), "%,.1f KM", remainingKm).replace(",", ".")
            }
        } else {
            binding.btnStartTrip.text = "▶ MULAI PERJALANAN"
            binding.btnStartTrip.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.accent_blue))
            
            // If trip just finished, dashboard will refresh via observeData() collect
        }
    }

    private fun ensureInitialData() {
        lifecycleScope.launch {
            db.motorDao().getMotorProfile().collect { profile ->
                if (profile == null) {
                    db.motorDao().insertProfile(Motor(
                        merk = "Yamaha",
                        model = "Mio S 125",
                        engineCc = "125 cc",
                        tahun = "2022",
                        warna = "Merah Hitam",
                        kilometerAwal = 20663.0
                    ))
                }
            }
        }
        lifecycleScope.launch {
            db.appSettingsDao().getSettings().collect { settings ->
                if (settings == null) {
                    db.appSettingsDao().insertSettings(AppSettings())
                }
            }
        }
        lifecycleScope.launch {
            db.userDao().getUserProfile().collect { user ->
                if (user == null) {
                    db.userDao().insertProfile(com.example.motortrackeryamaha.data.UserProfile())
                }
            }
        }
        lifecycleScope.launch {
            db.odometerDao().getOdometer().collect { odo ->
                if (odo == null) {
                    db.odometerDao().insertOdometer(com.example.motortrackeryamaha.data.MotorOdometer())
                }
            }
        }
    }

    private fun observeData() {
        lifecycleScope.launch {
            db.motorDao().getMotorProfile().collect { motor ->
                motor?.let {
                    binding.tvMotorNameHeader.text = "${it.merk} ${it.model}".uppercase()
                    if (it.photoUri != null) {
                        binding.ivMotorDashboard.setImageURI(android.net.Uri.parse(it.photoUri))
                    } else {
                        binding.ivMotorDashboard.setImageResource(R.mipmap.img)
                    }
                }
            }
        }

        lifecycleScope.launch {
            combine(
                db.appSettingsDao().getSettings(),
                db.oilChangeDao().getLastOilChange()
            ) { settings, lastOil ->
                Pair(settings, lastOil)
            }.collect { (settings, lastOil) ->
                updateDashboard(settings, lastOil)
            }
        }
    }

    private fun updateDashboard(settings: AppSettings?, lastOil: OilChange?) {
        if (settings == null) return
        currentAppSettings = settings

        lifecycleScope.launch {
            val tripCount = db.tripDao().getTripCount().first()
            val oilCount = db.oilChangeDao().getOilChangeCount().first()
            val serviceCount = db.serviceDao().getServiceCount().first()
            
            val oilCost = db.oilChangeDao().getTotalOilCost().first() ?: 0.0
            val serviceCost = db.serviceDao().getTotalServiceCost().first() ?: 0.0
            val compCost = db.componentReplacementDao().getTotalReplacementCost().first() ?: 0.0
            val engineCost = db.engineRepairDao().getTotalRepairCost().first() ?: 0.0
            val totalMaintenanceCost = oilCost + serviceCost + compCost + engineCost

            // Summary Section
            binding.summaryDistance.tvLabel.text = "📍 Jarak Terpantau"
            binding.summaryDistance.tvValue.text = String.format(Locale.getDefault(), "%,.1f KM", settings.totalTrackedDistance).replace(",", ".")
            
            binding.summaryOil.tvLabel.text = "🛢️ Ganti Oli"
            binding.summaryOil.tvValue.text = "$oilCount kali"
            
            binding.summaryService.tvLabel.text = "🔧 Servis"
            binding.summaryService.tvValue.text = "$serviceCount kali"
            
            binding.summaryCost.tvLabel.text = "💰 Total Perawatan"
            binding.summaryCost.tvValue.text = "Rp " + String.format(Locale.getDefault(), "%,.0f", totalMaintenanceCost).replace(",", ".")
        }

        val totalDist = settings.totalTrackedDistance
        binding.tvTotalDistance.text = String.format(Locale.getDefault(), "%,.1f KM", totalDist).replace(",", ".")
        
        if (settings.trackingStartDate > 0) {
            val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
            binding.tvTrackingSince.text = "Sejak ${sdf.format(Date(settings.trackingStartDate))} ℹ️"
        } else {
            binding.tvTrackingSince.text = "Belum ada perjalanan direkam"
        }

        // 2. Status Oli
        val distSinceOil = settings.distanceSinceOilChange
        val intervalKm = settings.oilIntervalKm
        val intervalMonths = settings.oilIntervalMonths
        
        val progressPercent = ((distSinceOil / intervalKm) * 100).toInt().coerceIn(0, 100)
        binding.tvOilKmLabel.text = String.format(Locale.getDefault(), "%,.1f / %,.0f KM", distSinceOil, intervalKm).replace(",", ".")
        binding.oilProgressBar.progress = progressPercent

        val remainingKm = (intervalKm - distSinceOil).coerceAtLeast(0.0)
        binding.tvOilRemainingKm.text = String.format(Locale.getDefault(), "%,.1f KM", remainingKm).replace(",", ".")

        val lastDate = settings.lastOilChangeDate
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = lastDate
        calendar.add(Calendar.MONTH, intervalMonths)
        val limitDate = calendar.timeInMillis
        
        val now = System.currentTimeMillis()
        val diffMillis = limitDate - now
        val diffDays = (diffMillis / (1000 * 60 * 60 * 24)).toInt()
        
        binding.tvOilRemainingDays.text = if (diffDays > 0) "$diffDays HARI" else "WAKTUNYA GANTI"

        // 3. Logika Warna & Status
        val isTimeUp = now >= limitDate
        val isDistOver = distSinceOil >= intervalKm

        when {
            isDistOver || isTimeUp -> {
                val statusText = if (isTimeUp && !isDistOver) "● WAKTU GANTI OLI" else "● GANTI OLI SEKARANG"
                binding.tvOilStatusBadge.text = statusText
                updateStatusStyle(R.color.status_danger)
            }
            progressPercent >= 90 -> {
                binding.tvOilStatusBadge.text = "● SEGERA GANTI OLI"
                updateStatusStyle(R.color.status_orange)
            }
            progressPercent >= 75 -> {
                binding.tvOilStatusBadge.text = "● PERHATIAN"
                updateStatusStyle(R.color.status_warning)
            }
            else -> {
                binding.tvOilStatusBadge.text = "● AMAN"
                updateStatusStyle(R.color.status_safe)
            }
        }
    }

    private fun updateStatusStyle(colorRes: Int) {
        val color = ContextCompat.getColor(this, colorRes)
        binding.tvOilStatusBadge.setTextColor(color)
        binding.tvOilStatusBadge.backgroundTintList = ColorStateList.valueOf(color.withAlpha(40))
        binding.oilProgressBar.setIndicatorColor(color)
    }
    
    private fun Int.withAlpha(alpha: Int): Int {
        return (this and 0x00FFFFFF) or (alpha shl 24)
    }
}
