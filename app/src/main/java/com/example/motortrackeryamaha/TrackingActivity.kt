package com.example.motortrackeryamaha

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.motortrackeryamaha.data.AppDatabase
import com.example.motortrackeryamaha.databinding.ActivityTrackingBinding
import com.example.motortrackeryamaha.ui.history.HistoryAdapter
import com.example.motortrackeryamaha.ui.history.HistoryItem
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TrackingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTrackingBinding
    private lateinit var db: AppDatabase
    private lateinit var historyAdapter: HistoryAdapter
    
    private val handler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            val stats = LocationTrackingService.trackingStats.value
            if (stats.isTripActive) {
                val elapsed = System.currentTimeMillis() - stats.startTime
                updateDurationText(elapsed)
                handler.postDelayed(this, 1000)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            startTrackingService()
        } else {
            Toast.makeText(this, "Izin lokasi diperlukan untuk pelacakan perjalanan", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrackingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = AppDatabase.getDatabase(this)
        
        setSupportActionBar(binding.toolbar)
        
        binding.btnActionTrip.setOnClickListener {
            handleActionClick()
        }

        setupRecentTrips()
        setupBottomNavigation()
        observeTrackingService()
    }

    private fun handleActionClick() {
        val stats = LocationTrackingService.trackingStats.value
        if (stats.isTripActive) {
            showStopConfirmationDialog()
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            } else {
                startTrackingService()
            }
        }
    }

    private fun showStopConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Selesaikan Perjalanan?")
            .setMessage("Apakah Anda yakin ingin menyelesaikan perjalanan ini?")
            .setPositiveButton("SELESAIKAN") { _, _ ->
                val intent = Intent(this, LocationTrackingService::class.java).apply {
                    action = LocationTrackingService.ACTION_STOP_TRIP
                }
                startService(intent)
                Toast.makeText(this, "Mengakhiri perjalanan...", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("BATAL", null)
            .show()
    }

    private fun startTrackingService() {
        val intent = Intent(this, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_START_TRIP
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
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
                updateStatsUI(stats)
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.nav_trip
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_trip -> true
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
                    startActivity(Intent(this, com.example.motortrackeryamaha.ui.settings.SettingsActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupRecentTrips() {
        historyAdapter = HistoryAdapter(
            onItemClick = { },
            onItemLongClick = { }
        )
        binding.rvRecentTrips.adapter = historyAdapter
        
        lifecycleScope.launch {
            db.tripDao().getAllTrips().collect { trips ->
                val recentTrips = trips.take(3).map { HistoryItem.TripItem(it) }
                historyAdapter.submitList(recentTrips)
            }
        }

        binding.tvSeeAllTrips.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    private fun updateGpsStatusUI(status: LocationTrackingService.GpsStatus) {
        val stats = LocationTrackingService.trackingStats.value
        val isTripActive = stats.isTripActive
        
        when (status) {
            LocationTrackingService.GpsStatus.AKTIF -> {
                binding.tvGpsStatus.text = if (isTripActive) "● PERJALANAN AKTIF" else "○ GPS SIAP"
                binding.tvGpsStatus.setTextColor(ContextCompat.getColor(this, R.color.status_safe))
            }
            LocationTrackingService.GpsStatus.MENCARI_LOKASI -> {
                binding.tvGpsStatus.text = if (isTripActive) "● ${stats.statusText.uppercase()}" else "● MENCARI GPS"
                binding.tvGpsStatus.setTextColor(ContextCompat.getColor(this, R.color.status_warning))
            }
            LocationTrackingService.GpsStatus.NONAKTIF -> {
                binding.tvGpsStatus.text = "○ GPS MATI"
                binding.tvGpsStatus.setTextColor(ContextCompat.getColor(this, R.color.status_danger))
            }
        }

        // Handle satelite text
        if (status == LocationTrackingService.GpsStatus.AKTIF) {
            binding.tvSatelites.text = "SINYAL KUAT"
        } else {
            binding.tvSatelites.text = "SINYAL LEMAH"
        }
    }

    private fun updateStatsUI(stats: LocationTrackingService.TrackingStats) {
        val distKm = stats.currentDistanceMeters / 1000.0
        val distStr = String.format(Locale.getDefault(), "%,.1f KM", distKm).replace(",", ".")
        binding.tvCurrentDistance.text = distStr
        binding.footerTotalDist.text = distStr

        // Speedometer update
        binding.speedometer.setSpeed(stats.currentSpeedKmH.toFloat())

        // Correct Speed mapping
        val avgStr = String.format(Locale.getDefault(), "%.0f KM/Jam", stats.avgSpeedKmH)
        val maxStr = String.format(Locale.getDefault(), "%.0f KM/Jam", stats.maxSpeedKmH)
        
        binding.tvAvgSpeed.text = avgStr
        binding.footerAvgSpeed.text = avgStr
        binding.tvMaxSpeed.text = maxStr
        binding.footerMaxSpeed.text = maxStr

        // Points and Times
        val sdf = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID"))
        
        if (stats.isTripActive) {
            binding.tvStartPoint.text = stats.startPointName
            if (stats.startTimeActual > 0) {
                binding.tvStartTime.text = sdf.format(Date(stats.startTimeActual))
            }
        } else {
            // IF NOT ACTIVE, show current location where user is
            binding.tvStartPoint.text = stats.currentPointName
            binding.tvStartTime.text = sdf.format(Date()) // Real-time date and time
        }

        if (stats.endPointName.isNotEmpty()) {
            binding.tvEndPoint.text = stats.endPointName
        } else {
            binding.tvEndPoint.text = if (stats.isTripActive) "Sedang berjalan..." else "-"
        }

        if (stats.endTimeActual > 0) {
            binding.tvEndTime.text = sdf.format(Date(stats.endTimeActual))
        } else {
            binding.tvEndTime.text = if (stats.isTripActive) "Perjalanan masih aktif..." else "-"
        }

        if (stats.isTripActive) {
            binding.btnActionTrip.text = "■ SELESAI PERJALANAN"
            binding.btnActionTrip.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.status_danger))
            
            if (stats.startTime > 0) {
                handler.removeCallbacks(timerRunnable)
                handler.post(timerRunnable)
            }
            
            binding.tvGpsStatus.text = "● ${stats.statusText.uppercase()}"
        } else {
            binding.btnActionTrip.text = "▶ MULAI PERJALANAN"
            binding.btnActionTrip.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.accent_blue))
            handler.removeCallbacks(timerRunnable)
            
            if (stats.tripId == null && stats.startTime == 0L) {
                binding.tvDuration.text = "00:00:00"
                binding.footerTotalTime.text = "00:00:00"
            }
        }
    }

    private fun updateDurationText(elapsed: Long) {
        val seconds = (elapsed / 1000) % 60
        val minutes = (elapsed / (1000 * 60)) % 60
        val hours = (elapsed / (1000 * 60 * 60)) % 24
        val timeStr = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        binding.tvDuration.text = timeStr
        binding.footerTotalTime.text = timeStr
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timerRunnable)
    }
}
