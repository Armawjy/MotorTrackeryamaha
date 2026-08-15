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
import com.example.motortrackeryamaha.data.*
import com.example.motortrackeryamaha.databinding.ActivityHistoryBinding
import com.example.motortrackeryamaha.ui.history.HistoryAdapter
import com.example.motortrackeryamaha.ui.history.HistoryItem
import com.example.motortrackeryamaha.ui.history.HistoryGroup
import com.example.motortrackeryamaha.ui.history.HistoryGroupAdapter
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.*

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var db: AppDatabase
    private lateinit var groupAdapter: HistoryGroupAdapter
    private val filterFlow = MutableStateFlow(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
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
        setupTabs()
        setupListeners()
        setupBottomNavigation()
        observeHistory()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.nav_history
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
                R.id.nav_maintenance -> {
                    startActivity(Intent(this, MaintenanceActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_history -> true
                R.id.nav_settings -> {
                    startActivity(Intent(this, com.example.motortrackeryamaha.ui.settings.SettingsActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupListeners() {
        binding.btnLoadMore.setOnClickListener {
            showDatePickerDialog()
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = android.app.DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }
                scrollToDate(selectedCal.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun scrollToDate(timestamp: Long) {
        android.widget.Toast.makeText(this, "Mencari riwayat sebelum tanggal terpilih...", android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun setupRecyclerView() {
        groupAdapter = HistoryGroupAdapter(
            onItemClick = { item -> 
                if (item is HistoryItem.TripItem) {
                    val intent = Intent(this, com.example.motortrackeryamaha.ui.history.TripDetailActivity::class.java)
                    intent.putExtra("TRIP_ID", item.trip.id)
                    startActivity(intent)
                }
            },
            onItemLongClick = { item -> showDeleteDialog(item) }
        )
        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(this@HistoryActivity)
            adapter = groupAdapter
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                filterFlow.value = tab?.position ?: 0
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private data class HistoryRawData(
        val trips: List<Trip>,
        val oils: List<OilChange>,
        val services: List<Service>,
        val comps: List<ComponentReplacement>,
        val engines: List<EngineRepair>
    )

    private fun observeHistory() {
        val dataFlow = combine(
            db.tripDao().getAllTrips(),
            db.oilChangeDao().getAllOilChanges(),
            db.serviceDao().getAllServices(),
            db.componentReplacementDao().getAllReplacements(),
            db.engineRepairDao().getAllRepairs()
        ) { trips, oils, services, comps, engines ->
            HistoryRawData(trips, oils, services, comps, engines)
        }

        lifecycleScope.launch {
            combine(dataFlow, filterFlow) { data, filter ->
                val allItems = mutableListOf<HistoryItem>()
                if (filter == 0 || filter == 1) allItems.addAll(data.trips.map { HistoryItem.TripItem(it) })
                if (filter == 0 || filter == 2) allItems.addAll(data.oils.map { HistoryItem.OilItem(it) })
                if (filter == 0 || filter == 3) allItems.addAll(data.services.map { HistoryItem.ServiceItem(it) })
                if (filter == 0 || filter == 4) {
                    allItems.addAll(data.comps.map { HistoryItem.ComponentItem(it) })
                    allItems.addAll(data.engines.map { HistoryItem.EngineItem(it) })
                }
                
                val groups = allItems.sortedByDescending { getItemTimestamp(it) }
                    .groupBy { 
                        val cal = Calendar.getInstance().apply { timeInMillis = getItemTimestamp(it) }
                        cal.set(Calendar.HOUR_OF_DAY, 0)
                        cal.set(Calendar.MINUTE, 0)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        cal.timeInMillis
                    }
                    .map { HistoryGroup(it.key, it.value) }
                
                HistoryDataState(
                    groups = groups,
                    tripCount = data.trips.size,
                    oilCount = data.oils.size,
                    serviceCount = data.services.size,
                    maintCount = data.comps.size + data.engines.size
                )
            }.collect { state ->
                groupAdapter.submitGroups(state.groups)
                updateSummary(state.tripCount, state.oilCount, state.serviceCount, state.maintCount)
            }
        }
    }

    private data class HistoryDataState(
        val groups: List<HistoryGroup>,
        val tripCount: Int,
        val oilCount: Int,
        val serviceCount: Int,
        val maintCount: Int
    )

    private fun updateSummary(trips: Int, oils: Int, services: Int, maint: Int) {
        binding.tvTotalTrips.text = trips.toString()
        binding.tvTotalOils.text = oils.toString()
        binding.tvTotalServices.text = services.toString()
        binding.tvTotalMaintenances.text = maint.toString()
    }

    private fun getItemTimestamp(item: HistoryItem): Long {
        return when(item) {
            is HistoryItem.TripItem -> item.trip.tanggal
            is HistoryItem.OilItem -> item.oil.tanggal
            is HistoryItem.ServiceItem -> item.service.tanggal
            is HistoryItem.ComponentItem -> item.comp.tanggal
            is HistoryItem.EngineItem -> item.repair.tanggal
        }
    }

    private fun showDeleteDialog(item: HistoryItem) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Riwayat")
            .setMessage("Yakin ingin menghapus riwayat ini?")
            .setPositiveButton("HAPUS") { _, _ ->
                lifecycleScope.launch {
                    when(item) {
                        is HistoryItem.TripItem -> {
                            db.tripPointDao().deletePointsForTrip(item.trip.id)
                            db.tripDao().deleteTrip(item.trip)
                        }
                        is HistoryItem.OilItem -> db.oilChangeDao().deleteOilChange(item.oil)
                        is HistoryItem.ServiceItem -> db.serviceDao().deleteService(item.service)
                        is HistoryItem.ComponentItem -> db.componentReplacementDao().deleteReplacement(item.comp)
                        is HistoryItem.EngineItem -> db.engineRepairDao().deleteRepair(item.repair)
                    }
                }
            }
            .setNegativeButton("BATAL", null)
            .show()
    }
}
