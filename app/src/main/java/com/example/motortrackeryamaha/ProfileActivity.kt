package com.example.motortrackeryamaha

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.example.motortrackeryamaha.data.AppDatabase
import com.example.motortrackeryamaha.data.Motor
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var etBrand: TextInputEditText
    private lateinit var etModel: TextInputEditText
    private lateinit var etCc: TextInputEditText
    private lateinit var etYear: TextInputEditText
    private lateinit var etPlateNumber: TextInputEditText
    private lateinit var etNotes: TextInputEditText
    private lateinit var btnSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        db = AppDatabase.getDatabase(this)
        
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        initViews()
        loadProfile()
        
        btnSave.setOnClickListener {
            saveProfile()
        }
    }

    private fun initViews() {
        etBrand = findViewById(R.id.etBrand)
        etModel = findViewById(R.id.etModel)
        etCc = findViewById(R.id.etCc)
        etYear = findViewById(R.id.etYear)
        etPlateNumber = findViewById(R.id.etPlateNumber)
        etNotes = findViewById(R.id.etProfileNotes)
        btnSave = findViewById(R.id.btnSaveProfile)
    }

    private fun loadProfile() {
        lifecycleScope.launch {
            val profile = db.motorDao().getMotorProfile().first()
            profile?.let {
                etBrand.setText(it.merk)
                etModel.setText(it.model)
                etCc.setText(it.engineCc)
                etYear.setText(it.tahun)
                etPlateNumber.setText(it.nomorPolisi)
                etNotes.setText(it.catatan)
            }
        }
    }

    private fun saveProfile() {
        lifecycleScope.launch {
            val existing = db.motorDao().getMotorProfile().first()
            val newProfile = Motor(
                id = existing?.id ?: 0,
                merk = etBrand.text.toString(),
                model = etModel.text.toString(),
                engineCc = etCc.text.toString(),
                tahun = etYear.text.toString(),
                nomorPolisi = etPlateNumber.text.toString(),
                catatan = etNotes.text.toString(),
                updatedAt = System.currentTimeMillis()
            )
            db.motorDao().insertProfile(newProfile)
            finish()
        }
    }
}
