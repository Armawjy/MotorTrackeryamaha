package com.example.motortrackeryamaha.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.motortrackeryamaha.*
import com.example.motortrackeryamaha.data.AppDatabase
import com.example.motortrackeryamaha.databinding.ActivitySettingsBinding
import com.example.motortrackeryamaha.databinding.ItemSettingsRowBinding
import kotlinx.coroutines.launch
import java.util.*

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setSupportActionBar(binding.toolbar)

        setupMenu()
        observeMotorData()
    }

    private fun observeMotorData() {
        lifecycleScope.launch {
            db.motorDao().getMotorProfile().collect { motor ->
                motor?.let {
                    binding.tvMotorNameSettings.text = "${it.merk} ${it.model}".uppercase()
                    if (it.photoUri != null) {
                        try {
                            binding.ivMotorSettings.setImageURI(Uri.parse(it.photoUri))
                        } catch (e: Exception) {
                            binding.ivMotorSettings.setImageResource(R.mipmap.img)
                        }
                    } else {
                        binding.ivMotorSettings.setImageResource(R.mipmap.img)
                    }
                }
            }
        }
    }

    private fun setupMenu() {
        // Top Card Profile
        binding.cardProfileTop.setOnClickListener {
            startActivity(Intent(this, MotorProfileActivity::class.java))
        }

        // PENGATURAN AKUN
        setupRow(binding.menuProfileAccount, R.drawable.ic_person, "Profil Akun", "Kelola informasi akun Anda", color = "#2196F3") {
            startActivity(Intent(this, ProfileAccountActivity::class.java))
        }
        setupRow(binding.menuSecurity, R.drawable.ic_security, "Keamanan", "Ubah password dan keamanan akun", color = "#4CAF50") {
            startActivity(Intent(this, SecurityPrivacyActivity::class.java))
        }
        setupRow(binding.menuNotifications, R.drawable.ic_notifications, "Notifikasi", "Atur notifikasi dan pengingat", color = "#FFC107") {
            startActivity(Intent(this, NotificationSettingsActivity::class.java))
        }
        setupRow(binding.menuLanguage, R.drawable.ic_language, "Bahasa", "Bahasa Indonesia", color = "#9C27B0") {
            startActivity(Intent(this, LanguageSettingsActivity::class.java))
        }
        
        // Special handling for Theme row (Switch)
        setupRow(binding.menuTheme, R.drawable.ic_history, "Tema", "Mode Gelap", color = "#2196F3") {
            startActivity(Intent(this, ThemeSettingsActivity::class.java))
        }
        binding.menuTheme.ivArrow.visibility = View.VISIBLE
        binding.menuTheme.switchTheme.visibility = View.GONE

        // PENGATURAN APLIKASI
        setupRow(binding.menuLocation, R.drawable.ic_location, "Pengaturan Lokasi", "Atur lokasi default dan radius aman", color = "#00BCD4") {
            startActivity(Intent(this, LocationSettingsActivity::class.java))
        }
        setupRow(binding.menuUnits, R.drawable.ic_speed, "Satuan", "Kilometer (KM) & Liter", color = "#FFC107") {
            startActivity(Intent(this, UnitSettingsActivity::class.java))
        }
        setupRow(binding.menuServiceReminder, R.drawable.ic_oil, "Pengingat Servis", "Atur jarak dan waktu pengingat servis", color = "#4CAF50") {
            startActivity(Intent(this, ServiceReminderSettingsActivity::class.java))
        }
        setupRow(binding.menuBackupSettings, R.drawable.ic_restore, "Backup & Restore Data", "Cadangkan atau pulihkan data aplikasi", color = "#F44336") {
            startActivity(Intent(this, com.example.motortrackeryamaha.ui.settings.BackupRestoreActivity::class.java))
        }
        setupRow(binding.menuDataManage, R.drawable.ic_database, "Kelola Data", "Hapus cache dan data sementara", color = "#673AB7") {
            startActivity(Intent(this, ManageDataActivity::class.java))
        }

        // TENTANG
        setupRow(binding.menuAboutApp, R.drawable.ic_info, "Tentang Aplikasi", "Informasi versi dan kebijakan", color = "#757575") {
            startActivity(Intent(this, AboutAppActivity::class.java))
        }
        setupRow(binding.menuHelpCenter, R.drawable.ic_help, "Pusat Bantuan", "FAQ dan panduan penggunaan", color = "#757575") {
            startActivity(Intent(this, HelpCenterActivity::class.java))
        }
        setupRow(binding.menuShareApp, R.drawable.ic_share, "Bagikan Aplikasi", "Bagikan MotorTracker ke teman Anda", color = "#757575") {
            shareApp()
        }

        binding.cardLogout.setOnClickListener {
            showLogoutDialog()
        }

        // Bottom Navigation
        binding.bottomNavigation.selectedItemId = R.id.nav_settings
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
                R.id.nav_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_settings -> {
                    true
                }
                else -> false
            }
        }
    }

    private fun shareApp() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "MotorTracker Yamaha Mio S 125")
            putExtra(Intent.EXTRA_TEXT, "Saya menggunakan MotorTracker Yamaha Mio S 125 untuk memantau perjalanan, perawatan, oli, servis, dan riwayat motor.")
        }
        startActivity(Intent.createChooser(shareIntent, "Bagikan via"))
    }

    private fun showLogoutDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Keluar dari aplikasi?")
            .setMessage("Apakah Anda yakin ingin keluar?")
            .setPositiveButton("KELUAR") { _, _ ->
                finishAffinity()
            }
            .setNegativeButton("BATAL", null)
            .show()
    }

    private fun setupRow(
        rowBinding: ItemSettingsRowBinding,
        iconRes: Int,
        label: String,
        subLabel: String,
        color: String? = null,
        onClick: (() -> Unit)? = null
    ) {
        rowBinding.ivIcon.setImageResource(iconRes)
        color?.let { 
            val parsedColor = android.graphics.Color.parseColor(it)
            rowBinding.cardIconContainer.setCardBackgroundColor(
                android.content.res.ColorStateList.valueOf(parsedColor).withAlpha(40)
            )
            rowBinding.ivIcon.imageTintList = android.content.res.ColorStateList.valueOf(parsedColor)
        }
        rowBinding.tvLabel.text = label
        rowBinding.tvSubLabel.text = subLabel
        
        rowBinding.root.setOnClickListener {
            if (onClick != null) {
                onClick()
            } else {
                Toast.makeText(this, "$label akan segera hadir!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun Int.withAlpha(alpha: Int): Int {
        return (this and 0x00FFFFFF) or (alpha shl 24)
    }
}
