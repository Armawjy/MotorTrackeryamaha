package com.example.motortrackeryamaha

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.motortrackeryamaha.data.AppDatabase
import com.example.motortrackeryamaha.databinding.ActivityMaintenanceBinding
import com.example.motortrackeryamaha.ui.history.HistoryAdapter
import com.example.motortrackeryamaha.ui.history.HistoryItem
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MaintenanceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMaintenanceBinding
    private lateinit var db: AppDatabase
    private lateinit var historyAdapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMaintenanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = AppDatabase.getDatabase(this)

        setSupportActionBar(binding.toolbar)

        setupRecyclerView()
        setupBottomNavigation()
        observeData()

        binding.btnCatatPerawatan.setOnClickListener {
            showMaintenanceOptions()
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.nav_maintenance
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_trip -> {
                    startActivity(Intent(this, TrackingActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_maintenance -> true
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

    private fun showMaintenanceOptions() {
        val options = arrayOf("🛢️ Ganti Oli", "🔧 Servis CVT", "🔥 Ganti Busi", "🌬️ Filter Udara", "⚙️ V-Belt / Roller", "🛑 Cek Rem")
        AlertDialog.Builder(this)
            .setTitle("Catat Perawatan")
            .setItems(options) { _, which ->
                when(which) {
                    0 -> startActivity(Intent(this, AddOilChangeActivity::class.java))
                    else -> showAddServiceDialog(options[which])
                }
            }
            .show()
    }

    private fun showAddServiceDialog(type: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_maintenance, null)
        val etType = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etType)
        val etCost = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etCost)
        val etBengkel = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etBengkel)
        
        etType.setText(type.substring(3)) // Remove emoji
        
        AlertDialog.Builder(this)
            .setTitle("Tambah $type")
            .setView(dialogView)
            .setPositiveButton("SIMPAN") { _, _ ->
                val serviceType = etType.text.toString()
                val cost = etCost.text.toString().toDoubleOrNull() ?: 0.0
                val bengkel = etBengkel.text.toString()
                
                lifecycleScope.launch {
                    val settings = db.appSettingsDao().getSettings().first()
                    db.serviceDao().insertService(com.example.motortrackeryamaha.data.Service(
                        tanggal = System.currentTimeMillis(),
                        jenisService = serviceType,
                        kilometer = settings?.totalTrackedDistance ?: 0.0,
                        biaya = cost,
                        bengkel = bengkel,
                        catatan = ""
                    ))
                }
            }
            .setNegativeButton("BATAL", null)
            .show()
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(
            onItemClick = { },
            onItemLongClick = { }
        )
        binding.rvMaintenanceHistory.apply {
            layoutManager = LinearLayoutManager(this@MaintenanceActivity)
            adapter = historyAdapter
        }
        
        // Schedule RV will be handled with a separate adapter in a real scenario
        // For now, I'll focus on the data observation
    }

    private fun observeData() {
        lifecycleScope.launch {
            combine(
                db.appSettingsDao().getSettings(),
                db.oilChangeDao().getAllOilChanges(),
                db.serviceDao().getAllServices()
            ) { settings, oils, services ->
                Triple(settings, oils, services)
            }.collect { (settings, oils, services) ->
                updateUI(settings, oils, services)
            }
        }
    }

    private fun updateUI(
        settings: com.example.motortrackeryamaha.data.AppSettings?,
        oils: List<com.example.motortrackeryamaha.data.OilChange>,
        services: List<com.example.motortrackeryamaha.data.Service>
    ) {
        if (settings == null) return
        
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        
        // Summary Stats
        binding.tvTotalService.text = services.size.toString()
        binding.tvTotalOil.text = oils.size.toString()
        
        // Logic for next servicekm and days would go here
        
        // Update History List (last 3)
        val historyItems = mutableListOf<HistoryItem>()
        historyItems.addAll(oils.take(3).map { HistoryItem.OilItem(it) })
        historyItems.addAll(services.take(3).map { HistoryItem.ServiceItem(it) })
        
        val sortedHistory = historyItems.sortedByDescending { 
            when(it) {
                is HistoryItem.OilItem -> it.oil.tanggal
                is HistoryItem.ServiceItem -> it.service.tanggal
                else -> 0L
            }
        }.take(3)
        
        historyAdapter.submitList(sortedHistory)
    }
}
