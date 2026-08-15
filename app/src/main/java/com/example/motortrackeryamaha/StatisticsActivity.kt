package com.example.motortrackeryamaha

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.motortrackeryamaha.data.AppDatabase
import com.example.motortrackeryamaha.databinding.ActivityStatisticsBinding
import com.example.motortrackeryamaha.databinding.ItemStatCardBinding
import com.example.motortrackeryamaha.databinding.ItemInfoRowBinding
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class StatisticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatisticsBinding
    private lateinit var db: AppDatabase
    private var currentPeriod = 3 // 0: Week, 1: Month, 2: Year, 3: All

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = AppDatabase.getDatabase(this)

        setSupportActionBar(binding.toolbar)

        setupTabs()
        loadStatistics()
    }

    private fun setupTabs() {
        binding.periodTabLayout.selectTab(binding.periodTabLayout.getTabAt(currentPeriod))
        binding.periodTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentPeriod = tab?.position ?: 3
                loadStatistics()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun loadStatistics() {
        val startTime = when (currentPeriod) {
            0 -> Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }.timeInMillis
            1 -> Calendar.getInstance().apply { add(Calendar.MONTH, -1) }.timeInMillis
            2 -> Calendar.getInstance().apply { add(Calendar.YEAR, -1) }.timeInMillis
            else -> 0L
        }

        val periodLabel = when (currentPeriod) {
            0 -> "Statistik Minggu Ini"
            1 -> "Statistik Bulan Ini"
            2 -> "Statistik Tahun Ini"
            else -> "Statistik Keseluruhan"
        }
        binding.tvPeriodTitle.text = periodLabel

        lifecycleScope.launch {
            // Summary Data
            val trips = db.tripDao().getAllTrips().first().filter { it.tanggal >= startTime }
            val tripCount = trips.size
            val totalDistance = trips.sumOf { it.jarak }
            
            val oilChanges = db.oilChangeDao().getAllOilChanges().first().filter { it.tanggal >= startTime }
            val oilCount = oilChanges.size
            val oilCost = oilChanges.sumOf { it.biaya }
            
            val services = db.serviceDao().getAllServices().first().filter { it.tanggal >= startTime }
            val serviceCount = services.size
            val serviceCost = services.sumOf { it.biaya }
            
            val replacements = db.componentReplacementDao().getAllReplacements().first().filter { it.tanggal >= startTime }
            val compCount = replacements.size
            val compCost = replacements.sumOf { it.biaya }
            
            val engineRepairs = db.engineRepairDao().getAllRepairs().first().filter { it.tanggal >= startTime }
            val engineCost = engineRepairs.sumOf { it.biaya }

            val totalCost = oilCost + serviceCost + compCost + engineCost

            // Update Cards
            updateStatCard(ItemStatCardBinding.bind(binding.cardDistance.root), "JARAK TERPANTAU", String.format(Locale.getDefault(), "%,.1f KM", totalDistance).replace(",", "."))
            updateStatCard(ItemStatCardBinding.bind(binding.cardTrips.root), "JUMLAH PERJALANAN", "$tripCount")
            updateStatCard(ItemStatCardBinding.bind(binding.cardOil.root), "TOTAL GANTI OLI", "$oilCount")
            updateStatCard(ItemStatCardBinding.bind(binding.cardService.root), "TOTAL SERVIS", "$serviceCount")
            updateStatCard(ItemStatCardBinding.bind(binding.cardCost.root), "TOTAL BIAYA", "Rp " + String.format(Locale.getDefault(), "%,.0f", totalCost).replace(",", "."))
            updateStatCard(ItemStatCardBinding.bind(binding.cardComp.root), "KOMPONEN DIGANTI", "$compCount")

            // Update Breakdown
            updateInfoRow(ItemInfoRowBinding.bind(binding.rowOilCost.root), "Oli", "Rp " + String.format(Locale.getDefault(), "%,.0f", oilCost).replace(",", "."))
            updateInfoRow(ItemInfoRowBinding.bind(binding.rowServiceCost.root), "Servis", "Rp " + String.format(Locale.getDefault(), "%,.0f", serviceCost).replace(",", "."))
            updateInfoRow(ItemInfoRowBinding.bind(binding.rowCompCost.root), "Komponen", "Rp " + String.format(Locale.getDefault(), "%,.0f", compCost).replace(",", "."))
            updateInfoRow(ItemInfoRowBinding.bind(binding.rowEngineCost.root), "Mesin", "Rp " + String.format(Locale.getDefault(), "%,.0f", engineCost).replace(",", "."))
            updateInfoRow(ItemInfoRowBinding.bind(binding.rowTotalCost.root), "TOTAL", "Rp " + String.format(Locale.getDefault(), "%,.0f", totalCost).replace(",", "."))

            // Chart Data (Last 7 days distance)
            updateChart(trips)
        }
    }

    private fun updateStatCard(binding: ItemStatCardBinding, label: String, value: String) {
        binding.tvStatLabel.text = label
        binding.tvStatValue.text = value
    }

    private fun updateInfoRow(binding: ItemInfoRowBinding, label: String, value: String) {
        binding.tvLabel.text = label
        binding.tvValue.text = value
    }

    private fun updateChart(trips: List<com.example.motortrackeryamaha.data.Trip>) {
        val last7Days = mutableListOf<Double>()
        val labels = mutableListOf<String>()
        val sdf = SimpleDateFormat("EEE", Locale.getDefault())

        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance().apply { 
                add(Calendar.DAY_OF_YEAR, -i)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            val date = cal.timeInMillis
            val nextDate = date + (24 * 60 * 60 * 1000)
            
            val dayDistance = trips.filter { it.tanggal in date until nextDate }.sumOf { it.jarak }
            last7Days.add(dayDistance)
            labels.add(sdf.format(Date(date)))
        }
        
        binding.distanceChart.setData(last7Days, labels)
    }
}

