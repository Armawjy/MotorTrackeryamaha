package com.example.motortrackeryamaha.ui.history

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.motortrackeryamaha.data.AppDatabase
import com.example.motortrackeryamaha.databinding.ActivityTrackingBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TripDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTrackingBinding
    private lateinit var db: AppDatabase

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
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        val tripId = intent.getIntExtra("TRIP_ID", -1)
        if (tripId != -1) {
            loadTripData(tripId)
        }
        
        // Hide action button in detail mode
        binding.btnActionTrip.visibility = android.view.View.GONE
    }

    private fun loadTripData(tripId: Int) {
        lifecycleScope.launch {
            db.tripDao().getTripWithPoints(tripId).collect { tripWithPoints ->
                tripWithPoints?.let {
                    val trip = it.trip
                    binding.tvCurrentDistance.text = String.format("%.1f KM", trip.jarak)
                    binding.tvDuration.text = formatDuration(trip.durasi)
                    binding.tvAvgSpeed.text = String.format("%.1f KM/Jam", trip.avgSpeed)
                    binding.tvMaxSpeed.text = String.format("%.1f KM/Jam", trip.maxSpeed)
                    
                    binding.tvStartPoint.text = trip.titikAwal
                    binding.tvEndPoint.text = trip.titikAkhir
                    
                    val sdf = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID"))
                    binding.tvStartTime.text = sdf.format(Date(trip.tanggal))
                    
                    if (trip.status == "COMPLETED") {
                        binding.tvEndTime.text = sdf.format(Date(trip.endTime))
                    } else {
                        binding.tvEndTime.text = "Perjalanan masih aktif..."
                    }
                    
                    binding.tvGpsStatus.text = "PERJALANAN SELESAI"
                    binding.tvGpsStatus.setTextColor(android.graphics.Color.GRAY)
                }
            }
        }
    }

    private fun formatDuration(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        val hours = (millis / (1000 * 60 * 60)) % 24
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}
