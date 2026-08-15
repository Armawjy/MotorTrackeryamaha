package com.example.motortrackeryamaha

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.motortrackeryamaha.databinding.ActivityAboutAppBinding
import com.example.motortrackeryamaha.databinding.ItemSettingsRowBinding

class AboutAppActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutAppBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutAppBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
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
        setupRow(binding.rowDeveloper, R.drawable.ic_person, "Pengembang", "Arma Wijaya", "#2196F3")
        setupRow(binding.rowPolicy, R.drawable.ic_info, "Kebijakan Privasi", "Versi 1.0", "#4CAF50")
        setupRow(binding.rowLicense, R.drawable.ic_settings, "Lisensi Perangkat Lunak", "Open Source", "#FFC107")
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
