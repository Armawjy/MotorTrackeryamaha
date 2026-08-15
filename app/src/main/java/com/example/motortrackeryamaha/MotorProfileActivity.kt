package com.example.motortrackeryamaha

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.motortrackeryamaha.data.AppDatabase
import com.example.motortrackeryamaha.data.AppSettings
import com.example.motortrackeryamaha.data.Motor
import com.example.motortrackeryamaha.data.MotorOdometer
import com.example.motortrackeryamaha.data.OilChange
import com.example.motortrackeryamaha.databinding.ActivityMotorProfileBinding
import com.example.motortrackeryamaha.databinding.ItemOdometerCardBinding
import com.example.motortrackeryamaha.databinding.ItemSettingsRowBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MotorProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMotorProfileBinding
    private lateinit var db: AppDatabase
    private var currentMotor: Motor? = null
    private var currentOdometer: MotorOdometer? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            try {
                contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {}
            
            currentMotor?.let { motor ->
                lifecycleScope.launch {
                    val updatedMotor = motor.copy(photoUri = it.toString(), updatedAt = System.currentTimeMillis())
                    db.motorDao().insertProfile(updatedMotor)
                    Toast.makeText(this@MotorProfileActivity, "Foto motor diperbarui", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMotorProfileBinding.inflate(layoutInflater)
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

        observeMotorData()
        observeSettingsData()
        observeOdometerData()
        observeLastOilData()
        
        binding.cardMotorPhoto.setOnClickListener {
            pickImageLauncher.launch(arrayOf("image/*"))
        }

        binding.btnEditMotor.setOnClickListener {
            Toast.makeText(this, "Klik pada item untuk mengedit", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeMotorData() {
        lifecycleScope.launch {
            db.motorDao().getMotorProfile().collect { motor ->
                motor?.let {
                    currentMotor = it
                    updateMotorUI(it)
                }
            }
        }
    }

    private fun observeSettingsData() {
        lifecycleScope.launch {
            db.appSettingsDao().getSettings().collect { settings ->
                settings?.let {
                    // Current settings observation doesn't update specific UI rows here anymore
                    // as they are handled by Motor and Odometer observations
                }
            }
        }
    }

    private fun observeOdometerData() {
        lifecycleScope.launch {
            db.odometerDao().getOdometer().collect { odometer ->
                odometer?.let {
                    currentOdometer = it
                    updateOdometerUI(it)
                }
            }
        }
    }

    private fun observeLastOilData() {
        lifecycleScope.launch {
            db.oilChangeDao().getLastOilChange().collect { lastOil ->
                updateLastOilUI(lastOil)
            }
        }
    }

    private fun updateMotorUI(motor: Motor) {
        if (motor.photoUri != null) {
            try {
                binding.ivMotorPhoto.setImageURI(Uri.parse(motor.photoUri))
            } catch (e: Exception) {
                binding.ivMotorPhoto.setImageResource(R.mipmap.img)
            }
        } else {
            binding.ivMotorPhoto.setImageResource(R.mipmap.img)
        }

        setupRow(binding.rowBrand, R.drawable.ic_trip, "Merek", motor.merk, "#2196F3") { 
            showEditDialog("Merek", motor.merk) { newValue -> updateMotor { it.copy(merk = newValue) } } 
        }
        setupRow(binding.rowModel, R.drawable.ic_trip, "Model", motor.model, "#2196F3") {
            showEditDialog("Model", motor.model) { newValue -> updateMotor { it.copy(model = newValue) } }
        }
        setupRow(binding.rowCc, R.drawable.ic_settings, "CC", motor.engineCc, "#757575") {
            showEditDialog("CC", motor.engineCc) { newValue -> updateMotor { it.copy(engineCc = newValue) } }
        }
        setupRow(binding.rowYear, R.drawable.ic_history, "Tahun", motor.tahun, "#FFC107") {
            showEditDialog("Tahun", motor.tahun) { newValue -> updateMotor { it.copy(tahun = newValue) } }
        }
        setupRow(binding.rowPlate, R.drawable.ic_trip, "Nomor Polisi", motor.nomorPolisi, "#4CAF50") {
            showEditDialog("Nomor Polisi", motor.nomorPolisi) { newValue -> updateMotor { it.copy(nomorPolisi = newValue) } }
        }
        setupRow(binding.rowColor, R.drawable.ic_build, "Warna", motor.warna, "#9C27B0") {
            showEditDialog("Warna", motor.warna) { newValue -> updateMotor { it.copy(warna = newValue) } }
        }
        
        setupRow(binding.rowNotes, R.drawable.ic_info, "Catatan", motor.catatan.ifEmpty { "-" }, "#757575") {
            showEditDialog("Catatan", motor.catatan) { newValue -> updateMotor { it.copy(catatan = newValue) } }
        }
    }

    private fun updateOdometerUI(odo: MotorOdometer) {
        val localeId = Locale("id", "ID")
        val sdf = SimpleDateFormat("dd MMMM yyyy", localeId)
        val sdfTime = SimpleDateFormat("dd MMMM yyyy HH:mm", localeId)
        
        // Odometer Fisik
        setupOdoCard(
            binding.rowOdometerFisik,
            R.drawable.ic_speed,
            "Odometer Fisik",
            "Angka terakhir pada speedometer motor",
            if (odo.isOdometerFisikSet) String.format(localeId, "%,.0f KM", odo.odometerFisik) else "Belum diisi",
            "Ditetapkan pengguna",
            if (odo.odometerFisikDate != null) "Tanggal pencatatan: ${sdf.format(Date(odo.odometerFisikDate))}" else "",
            "#FFD600"
        ) {
            showEditDialog("Odometer Fisik", String.format(localeId, "%.0f", odo.odometerFisik)) { newValue ->
                val dValue = newValue.replace(".", "").replace(",", "").toDoubleOrNull() ?: odo.odometerFisik
                updateOdometer { it.copy(odometerFisik = dValue, isOdometerFisikSet = true, odometerFisikDate = System.currentTimeMillis()) }
            }
        }

        // Odometer Digital
        setupOdoCard(
            binding.rowOdometerDigitalCard,
            R.drawable.ic_trip,
            "Odometer Digital (GPS)",
            "Jarak yang direkam aplikasi berdasarkan GPS",
            String.format(localeId, "%,.1f KM", odo.odometerDigital),
            "AKTIF",
            if (odo.odometerDigitalSince != null) "Sejak: ${sdf.format(Date(odo.odometerDigitalSince))}" else "Belum ada rekaman",
            "#00E676"
        ) {
             showEditDialog("Odometer Digital", String.format(localeId, "%.1f", odo.odometerDigital)) { newValue ->
                val dValue = newValue.replace(",", ".").toDoubleOrNull() ?: odo.odometerDigital
                updateOdometer { it.copy(odometerDigital = dValue, odometerDigitalSince = odo.odometerDigitalSince ?: System.currentTimeMillis()) }
            }
        }

        // Odometer Estimasi
        val estimasi = odo.odometerFisik + odo.odometerDigital
        setupOdoCard(
            binding.rowOdometerEstimasi,
            R.drawable.ic_info,
            "Odometer Estimasi Saat Ini",
            "Odometer fisik + odometer digital (GPS)",
            String.format(localeId, "%,.1f KM", estimasi),
            "ESTIMASI",
            "Terakhir diperbarui: ${sdfTime.format(Date(odo.lastUpdated))}",
            "#00E5FF"
        )
    }

    private fun updateLastOilUI(oil: OilChange?) {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
        val subLabel = if (oil != null) "Ganti Oli Mesin - ${sdf.format(Date(oil.tanggal))}" else "Belum ada catatan"
        setupRow(binding.rowLastService, R.drawable.ic_oil, "Perawatan Terakhir", subLabel, "#757575") {
             // Navigate to maintenance history
             val intent = Intent(this, MaintenanceActivity::class.java)
             startActivity(intent)
        }
    }

    private fun setupOdoCard(
        cardBinding: ItemOdometerCardBinding,
        iconRes: Int,
        label: String,
        desc: String,
        value: String,
        badge: String,
        footer: String,
        colorHex: String,
        onClick: (() -> Unit)? = null
    ) {
        val color = android.graphics.Color.parseColor(colorHex)
        cardBinding.ivIcon.setImageResource(iconRes)
        cardBinding.ivIcon.imageTintList = android.content.res.ColorStateList.valueOf(color)
        cardBinding.cardIconContainer.setCardBackgroundColor(color.withAlpha(20))
        
        cardBinding.tvLabel.text = label
        cardBinding.tvDescription.text = desc
        cardBinding.tvValue.text = value
        cardBinding.tvBadge.text = badge
        cardBinding.tvBadge.setTextColor(color)
        cardBinding.tvBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(color.withAlpha(30))
        cardBinding.tvFooter.text = footer
        
        if (label.contains("Estimasi")) {
             cardBinding.tvLabel.setTextColor(color)
             cardBinding.cardOdo.strokeColor = color.withAlpha(100)
             cardBinding.cardOdo.setCardBackgroundColor(color.withAlpha(10))
        } else {
             cardBinding.tvLabel.setTextColor(android.graphics.Color.WHITE)
             cardBinding.cardOdo.strokeColor = android.graphics.Color.parseColor("#1AFFFFFF")
             cardBinding.cardOdo.setCardBackgroundColor(android.graphics.Color.parseColor("#1A1D29"))
        }
        
        cardBinding.ivArrow.visibility = if (onClick != null) View.VISIBLE else View.GONE
        cardBinding.cardOdo.setOnClickListener { onClick?.invoke() }
    }

    private fun updateOdometer(transform: (MotorOdometer) -> MotorOdometer) {
        lifecycleScope.launch {
            val odo = db.odometerDao().getOdometer().first() ?: MotorOdometer()
            val updated = transform(odo).copy(lastUpdated = System.currentTimeMillis())
            db.odometerDao().insertOdometer(updated)
            Toast.makeText(this@MotorProfileActivity, "Odometer diperbarui", Toast.LENGTH_SHORT).show()
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

    private fun showEditDialog(title: String, currentValue: String, onSave: (String) -> Unit) {
        val input = com.google.android.material.textfield.TextInputEditText(this).apply {
            setText(currentValue)
            setPadding(60, 40, 60, 40)
        }
        val container = android.widget.FrameLayout(this)
        container.addView(input)
        
        AlertDialog.Builder(this)
            .setTitle("Edit $title")
            .setView(container)
            .setPositiveButton("SIMPAN") { _, _ ->
                val newValue = input.text.toString()
                onSave(newValue)
            }
            .setNegativeButton("BATAL", null)
            .show()
    }

    private fun updateMotor(transform: (Motor) -> Motor) {
        currentMotor?.let {
            lifecycleScope.launch {
                db.motorDao().insertProfile(transform(it).copy(updatedAt = System.currentTimeMillis()))
                Toast.makeText(this@MotorProfileActivity, "Profil motor diperbarui", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateSettings(transform: (AppSettings) -> AppSettings) {
        lifecycleScope.launch {
            val settings = db.appSettingsDao().getSettings().first() ?: AppSettings()
            db.appSettingsDao().insertSettings(transform(settings))
            Toast.makeText(this@MotorProfileActivity, "Odometer digital diperbarui", Toast.LENGTH_SHORT).show()
        }
    }

    private fun Int.withAlpha(alpha: Int): Int {
        return (this and 0x00FFFFFF) or (alpha shl 24)
    }
}
