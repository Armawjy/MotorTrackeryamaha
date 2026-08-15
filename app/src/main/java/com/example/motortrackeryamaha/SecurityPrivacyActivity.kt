package com.example.motortrackeryamaha

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.motortrackeryamaha.databinding.ActivitySecurityPrivacyBinding
import com.example.motortrackeryamaha.databinding.ItemSettingsRowBinding

class SecurityPrivacyActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySecurityPrivacyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySecurityPrivacyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupMenu()
    }

    private fun setupMenu() {
        setupRow(binding.rowSecurityStatus, R.drawable.ic_security, "Akun Aman", "Terlindungi secara maksimal", "#4CAF50")
        setupRow(binding.rowLastScan, R.drawable.ic_history, "Pemeriksaan Terakhir", "Hari ini, 08:30", "#2196F3")

        setupRow(binding.rowTwoStep, R.drawable.ic_security, "Verifikasi 2 Langkah", "Nonaktif", "#757575")
        setupRow(binding.rowDeviceLogin, R.drawable.ic_settings, "Login Perangkat", "Kelola perangkat Anda", "#2196F3")
        setupRow(binding.rowLoginActivity, R.drawable.ic_history, "Aktivitas Login", "Lihat riwayat login", "#FFC107")
        setupRow(binding.rowChangePass, R.drawable.ic_security, "Ubah Password", "Perbarui password Anda", "#F44336")
        setupRow(binding.rowSecurityQuest, R.drawable.ic_help, "Pertanyaan Keamanan", "Atur pertanyaan pemulihan", "#9C27B0")

        setupRow(binding.rowLocationPermission, R.drawable.ic_location, "Izin Lokasi", "Selalu diizinkan", "#4CAF50")
        setupRow(binding.rowNotifPermission, R.drawable.ic_notifications, "Izin Notifikasi", "Diizinkan", "#4CAF50")
        setupRow(binding.rowDataPermission, R.drawable.ic_database, "Izin Data", "Akses data lokal", "#2196F3")
        setupRow(binding.rowPrivacyPolicy, R.drawable.ic_info, "Kebijakan Privasi", "Baca selengkapnya", "#757575")
    }

    private fun setupRow(rowBinding: ItemSettingsRowBinding, iconRes: Int, label: String, subLabel: String, colorHex: String) {
        rowBinding.ivIcon.setImageResource(iconRes)
        val color = android.graphics.Color.parseColor(colorHex)
        rowBinding.cardIconContainer.setCardBackgroundColor(color.withAlpha(30))
        rowBinding.ivIcon.imageTintList = android.content.res.ColorStateList.valueOf(color)
        rowBinding.tvLabel.text = label
        rowBinding.tvSubLabel.text = subLabel
        rowBinding.root.setOnClickListener {
            Toast.makeText(this, "$label akan segera hadir!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun Int.withAlpha(alpha: Int): Int {
        return (this and 0x00FFFFFF) or (alpha shl 24)
    }
}
