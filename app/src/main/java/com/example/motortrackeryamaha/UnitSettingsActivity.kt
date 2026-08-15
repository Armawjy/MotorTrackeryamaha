package com.example.motortrackeryamaha

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.motortrackeryamaha.databinding.ActivityUnitSettingsBinding
import com.example.motortrackeryamaha.databinding.ItemSettingsRowBinding

class UnitSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUnitSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUnitSettingsBinding.inflate(layoutInflater)
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
        setupRow(binding.rowUnitKm, R.drawable.ic_trip, "Kilometer (KM)", "Terpilih", "#4CAF50")
        setupRow(binding.rowUnitMile, R.drawable.ic_trip, "Mile (MI)", "Tidak aktif", "#757575")
        setupRow(binding.rowUnitLiter, R.drawable.ic_oil, "Liter", "Terpilih", "#4CAF50")
        setupRow(binding.rowUnitGallon, R.drawable.ic_oil, "Gallon", "Tidak aktif", "#757575")
    }

    private fun setupRow(rowBinding: ItemSettingsRowBinding, iconRes: Int, label: String, subLabel: String, colorHex: String) {
        rowBinding.ivIcon.setImageResource(iconRes)
        val color = android.graphics.Color.parseColor(colorHex)
        rowBinding.cardIconContainer.setCardBackgroundColor(color.withAlpha(30))
        rowBinding.ivIcon.imageTintList = android.content.res.ColorStateList.valueOf(color)
        rowBinding.tvLabel.text = label
        rowBinding.tvSubLabel.text = subLabel
        rowBinding.root.setOnClickListener {
            Toast.makeText(this, "Satuan $label diaktifkan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun Int.withAlpha(alpha: Int): Int {
        return (this and 0x00FFFFFF) or (alpha shl 24)
    }
}
