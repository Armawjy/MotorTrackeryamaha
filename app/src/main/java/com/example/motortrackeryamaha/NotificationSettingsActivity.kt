package com.example.motortrackeryamaha

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.motortrackeryamaha.databinding.ActivityNotificationSettingsBinding
import com.example.motortrackeryamaha.databinding.ItemSettingsRowBinding

class NotificationSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationSettingsBinding.inflate(layoutInflater)
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
        setupRow(binding.rowNotifApp, R.drawable.ic_notifications, "Notifikasi Aplikasi", "Aktif", "#4CAF50")
        setupRow(binding.rowNotifTrip, R.drawable.ic_trip, "Notifikasi Perjalanan", "Aktif", "#2196F3")
        setupRow(binding.rowNotifMaintenance, R.drawable.ic_oil, "Notifikasi Perawatan", "Aktif", "#FFC107")
        setupRow(binding.rowNotifGps, R.drawable.ic_location, "Notifikasi GPS", "Aktif", "#F44336")

        setupRow(binding.rowRemindOil, R.drawable.ic_oil, "Pengingat Ganti Oli", "Aktif", "#FFC107")
        setupRow(binding.rowRemindService, R.drawable.ic_build, "Pengingat Servis", "Aktif", "#4CAF50")
        setupRow(binding.rowRemindRadius, R.drawable.ic_location, "Pengingat Radius Aman", "Aktif", "#2196F3")
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
