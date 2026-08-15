package com.example.motortrackeryamaha

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.example.motortrackeryamaha.data.AppDatabase
import com.example.motortrackeryamaha.data.OilChange
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AddOilChangeActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var etOilBrand: TextInputEditText
    private lateinit var etOilType: TextInputEditText
    private lateinit var etCost: TextInputEditText
    private lateinit var etIntervalKm: TextInputEditText
    private lateinit var etIntervalMonths: TextInputEditText
    private lateinit var etWorkshop: TextInputEditText
    private lateinit var etNotes: TextInputEditText
    private lateinit var btnSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_oil_change)

        db = AppDatabase.getDatabase(this)
        
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        initViews()
        
        btnSave.setOnClickListener {
            saveOilChange()
        }
    }

    private fun initViews() {
        etOilBrand = findViewById(R.id.etOilBrand)
        etOilType = findViewById(R.id.etOilType)
        etCost = findViewById(R.id.etCost)
        etIntervalKm = findViewById(R.id.etIntervalKm)
        etIntervalMonths = findViewById(R.id.etIntervalMonths)
        etWorkshop = findViewById(R.id.etWorkshop)
        etNotes = findViewById(R.id.etNotes)
        btnSave = findViewById(R.id.btnSaveOil)
    }

    private fun saveOilChange() {
        val brand = etOilBrand.text.toString()
        val type = etOilType.text.toString()
        val cost = etCost.text.toString().toDoubleOrNull() ?: 0.0
        val intervalKm = etIntervalKm.text.toString().toDoubleOrNull() ?: 2000.0
        val intervalMonths = etIntervalMonths.text.toString().toIntOrNull() ?: 2
        val workshop = etWorkshop.text.toString()
        val notes = etNotes.text.toString()

        lifecycleScope.launch {
            val settings = db.appSettingsDao().getSettings().first()
            val currentTotalDist = settings?.totalTrackedDistance ?: 0.0
            val distSinceLast = settings?.distanceSinceOilChange ?: 0.0
            
            val oilChange = OilChange(
                tanggal = System.currentTimeMillis(),
                kilometerSaatGanti = currentTotalDist,
                jarakSejakGanti = distSinceLast,
                merkOli = brand,
                jenisOli = type,
                volume = "0.8L",
                biaya = cost,
                bengkel = workshop,
                catatan = notes
            )
            
            db.oilChangeDao().insertOilChange(oilChange)
            
            // LOGIKA RESET: Reset distanceSinceOilChange to 0
            settings?.let {
                db.appSettingsDao().updateSettings(it.copy(
                    distanceSinceOilChange = 0.0,
                    lastOilChangeDate = System.currentTimeMillis(),
                    oilIntervalKm = intervalKm,
                    oilIntervalMonths = intervalMonths
                ))
            }
            
            Toast.makeText(this@AddOilChangeActivity, "Ganti Oli Berhasil Dicatat & Status Direset!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
