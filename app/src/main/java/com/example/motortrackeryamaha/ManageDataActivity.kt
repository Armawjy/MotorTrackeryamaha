package com.example.motortrackeryamaha

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.motortrackeryamaha.databinding.ActivityManageDataBinding
import com.example.motortrackeryamaha.databinding.ItemSettingsRowBinding

class ManageDataActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageDataBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageDataBinding.inflate(layoutInflater)
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
        setupRow(binding.rowStorageSize, R.drawable.ic_database, "Ukuran Database", "2.4 MB", "#2196F3")
        setupRow(binding.rowTripCount, R.drawable.ic_trip, "Jumlah Perjalanan", "15", "#4CAF50")
        setupRow(binding.rowMaintenanceCount, R.drawable.ic_oil, "Jumlah Perawatan", "8", "#FFC107")

        setupRow(binding.rowClearCache, R.drawable.ic_restore, "Hapus Cache", "Bersihkan file sementara", "#FFC107") {
            showConfirmDialog("Hapus Cache", "Apakah Anda yakin ingin menghapus cache aplikasi?") {
                Toast.makeText(this, "Cache berhasil dihapus", Toast.LENGTH_SHORT).show()
            }
        }
        setupRow(binding.rowClearTrips, R.drawable.ic_trip, "Hapus Data Perjalanan", "Hapus semua riwayat rute", "#F44336") {
            showConfirmDialog("Hapus Perjalanan", "Semua riwayat perjalanan akan dihapus secara permanen. Lanjutkan?") {
                Toast.makeText(this, "Data perjalanan dikosongkan", Toast.LENGTH_SHORT).show()
            }
        }
        setupRow(binding.rowResetData, R.drawable.ic_logout, "Reset Data", "Hapus seluruh data aplikasi", "#F44336") {
            showConfirmDialog("Reset Seluruh Data", "TINDAKAN INI TIDAK DAPAT DIBATALKAN. Seluruh data aplikasi akan dihapus.") {
                Toast.makeText(this, "Aplikasi berhasil direset", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRow(rowBinding: ItemSettingsRowBinding, iconRes: Int, label: String, subLabel: String, colorHex: String, onClick: (() -> Unit)? = null) {
        rowBinding.ivIcon.setImageResource(iconRes)
        val color = android.graphics.Color.parseColor(colorHex)
        rowBinding.cardIconContainer.setCardBackgroundColor(color.withAlpha(30))
        rowBinding.ivIcon.imageTintList = android.content.res.ColorStateList.valueOf(color)
        rowBinding.tvLabel.text = label
        rowBinding.tvSubLabel.text = subLabel
        rowBinding.root.setOnClickListener { 
            if (onClick != null) onClick()
            else Toast.makeText(this, "$label detail", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showConfirmDialog(title: String, message: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("YA, LANJUTKAN") { _, _ -> onConfirm() }
            .setNegativeButton("BATAL", null)
            .show()
    }

    private fun Int.withAlpha(alpha: Int): Int {
        return (this and 0x00FFFFFF) or (alpha shl 24)
    }
}
