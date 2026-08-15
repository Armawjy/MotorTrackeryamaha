package com.example.motortrackeryamaha

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.motortrackeryamaha.databinding.ActivityServiceReminderSettingsBinding
import com.example.motortrackeryamaha.databinding.ItemSettingsRowBinding

class ServiceReminderSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityServiceReminderSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityServiceReminderSettingsBinding.inflate(layoutInflater)
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
        setupRow(binding.rowRemindOilDist, R.drawable.ic_oil, "Ganti Oli", "754 KM tersisa", "#FFC107")
        setupRow(binding.rowRemindCvtDist, R.drawable.ic_build, "Servis CVT", "1.200 KM tersisa", "#2196F3")
        setupRow(binding.rowRemindFilterDist, R.drawable.ic_settings, "Filter Udara", "2.500 KM tersisa", "#4CAF50")

        setupRow(binding.rowRemindMonthly, R.drawable.ic_history, "Bulanan", "Aktif", "#4CAF50")
        setupRow(binding.rowRemindQuarterly, R.drawable.ic_history, "3 Bulanan", "Aktif", "#4CAF50")
    }

    private fun setupRow(rowBinding: ItemSettingsRowBinding, iconRes: Int, label: String, subLabel: String, colorHex: String) {
        rowBinding.ivIcon.setImageResource(iconRes)
        val color = android.graphics.Color.parseColor(colorHex)
        rowBinding.cardIconContainer.setCardBackgroundColor(color.withAlpha(30))
        rowBinding.ivIcon.imageTintList = android.content.res.ColorStateList.valueOf(color)
        rowBinding.tvLabel.text = label
        rowBinding.tvSubLabel.text = subLabel
        rowBinding.root.setOnClickListener {
            Toast.makeText(this, "Pengaturan $label", Toast.LENGTH_SHORT).show()
        }
    }

    private fun Int.withAlpha(alpha: Int): Int {
        return (this and 0x00FFFFFF) or (alpha shl 24)
    }
}
