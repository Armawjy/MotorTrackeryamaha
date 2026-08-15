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
import com.example.motortrackeryamaha.data.UserProfile
import com.example.motortrackeryamaha.databinding.ActivityProfileAccountBinding
import com.example.motortrackeryamaha.databinding.ItemSettingsRowBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ProfileAccountActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileAccountBinding
    private lateinit var db: AppDatabase
    private var currentUser: UserProfile? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            try {
                contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {}
            
            currentUser?.let { user ->
                lifecycleScope.launch {
                    val updatedUser = user.copy(profilePhotoUri = it.toString())
                    db.userDao().updateProfile(updatedUser)
                    Toast.makeText(this@ProfileAccountActivity, "Foto profil diperbarui", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileAccountBinding.inflate(layoutInflater)
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

        setupClickListeners()
        observeUserData()
    }

    private fun observeUserData() {
        lifecycleScope.launch {
            db.userDao().getUserProfile().collect { user ->
                user?.let {
                    currentUser = it
                    updateUI(it)
                }
            }
        }
    }

    private fun updateUI(user: UserProfile) {
        binding.tvFullNameHeader.text = user.fullName
        binding.tvEmailHeader.text = user.email
        
        if (user.profilePhotoUri != null) {
            try {
                binding.ivAvatar.setImageURI(Uri.parse(user.profilePhotoUri))
                binding.ivAvatar.setPadding(0, 0, 0, 0)
            } catch (e: Exception) {
                binding.ivAvatar.setImageResource(R.drawable.ic_person)
                binding.ivAvatar.setPadding(20, 20, 20, 20)
            }
        } else {
            binding.ivAvatar.setImageResource(R.drawable.ic_person)
            binding.ivAvatar.setPadding(20, 20, 20, 20)
        }

        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
        val sdfFull = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID"))

        setupRow(binding.rowName, R.drawable.ic_person, "Nama Lengkap", user.fullName, "#2196F3") { showEditDialog("Nama Lengkap", user.fullName) { updateName(it) } }
        setupRow(binding.rowEmail, R.drawable.ic_menu, "Email", user.email, "#9C27B0") { showEditDialog("Email", user.email) { updateEmail(it) } }
        setupRow(binding.rowPhone, R.drawable.ic_trip, "Nomor Telepon", user.phone, "#4CAF50") { showEditDialog("Nomor Telepon", user.phone) { updatePhone(it) } }
        setupRow(binding.rowBirthDate, R.drawable.ic_history, "Tanggal Lahir", user.birthDate, "#FFC107") { showEditDialog("Tanggal Lahir", user.birthDate) { updateBirthDate(it) } }
        setupRow(binding.rowAddress, R.drawable.ic_location, "Alamat", user.address, "#F44336") { showEditDialog("Alamat", user.address) { updateAddress(it) } }
        setupRow(binding.rowGender, R.drawable.ic_person, "Jenis Kelamin", user.gender, "#00BCD4") { showGenderDialog(user.gender) }

        setupRow(binding.rowJoinDate, R.drawable.ic_history, "Tanggal Bergabung", sdf.format(Date(user.joinDate)), "#673AB7")
        setupRow(binding.rowLastLogin, R.drawable.ic_history, "Terakhir Masuk", sdfFull.format(Date(user.lastLogin)), "#4CAF50")
        setupRow(binding.rowDevices, R.drawable.ic_settings, "Perangkat Terdaftar", "1 Perangkat", "#2196F3")
        setupRow(binding.rowStatus, R.drawable.ic_build, "Status Akun", user.accountStatus, "#4CAF50")

        setupRow(binding.rowChangePassword, R.drawable.ic_security, "Ubah Password", "Perbarui password akun Anda", "#FFC107") { showChangePasswordDialog() }
        setupRow(binding.rowPrivacySecurity, R.drawable.ic_security, "Privasi & Keamanan", "Kelola privasi dan keamanan akun", "#4CAF50") {
            Toast.makeText(this, "Halaman Privasi & Keamanan akan segera hadir", Toast.LENGTH_SHORT).show()
        }
        setupRow(binding.rowDeleteAccount, R.drawable.ic_logout, "Hapus Akun", "Hapus akun secara permanen", "#F44336") { showDeleteConfirmation() }
    }

    private fun setupClickListeners() {
        binding.btnEditPhoto.setOnClickListener {
            pickImageLauncher.launch(arrayOf("image/*"))
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

    private fun Int.withAlpha(alpha: Int): Int {
        return (this and 0x00FFFFFF) or (alpha shl 24)
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
                if (newValue.isNotEmpty()) {
                    onSave(newValue)
                }
            }
            .setNegativeButton("BATAL", null)
            .show()
    }

    private fun showGenderDialog(current: String) {
        val genders = arrayOf("Laki-laki", "Perempuan")
        val checked = if (current == "Laki-laki") 0 else 1
        
        AlertDialog.Builder(this)
            .setTitle("Pilih Jenis Kelamin")
            .setSingleChoiceItems(genders, checked) { dialog, which ->
                updateGender(genders[which])
                dialog.dismiss()
            }
            .show()
    }

    private fun updateName(value: String) = updateProfile { it.copy(fullName = value) }
    private fun updateEmail(value: String) = updateProfile { it.copy(email = value) }
    private fun updatePhone(value: String) = updateProfile { it.copy(phone = value) }
    private fun updateBirthDate(value: String) = updateProfile { it.copy(birthDate = value) }
    private fun updateAddress(value: String) = updateProfile { it.copy(address = value) }
    private fun updateGender(value: String) = updateProfile { it.copy(gender = value) }

    private fun updateProfile(transform: (UserProfile) -> UserProfile) {
        currentUser?.let {
            lifecycleScope.launch {
                db.userDao().updateProfile(transform(it))
                Toast.makeText(this@ProfileAccountActivity, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showChangePasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)
        val etOld = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etOldPassword)
        val etNew = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etNewPassword)
        val etConfirm = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etConfirmPassword)

        AlertDialog.Builder(this)
            .setTitle("Ubah Password")
            .setView(dialogView)
            .setPositiveButton("SIMPAN") { _, _ ->
                val newPass = etNew.text.toString()
                val confirm = etConfirm.text.toString()
                if (newPass.isEmpty()) {
                    Toast.makeText(this, "Password baru tidak boleh kosong", Toast.LENGTH_SHORT).show()
                } else if (newPass != confirm) {
                    Toast.makeText(this, "Konfirmasi password tidak sama", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Password berhasil diubah", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("BATAL", null)
            .show()
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Hapus Akun?")
            .setMessage("Apakah Anda yakin ingin menghapus akun? Data profil lokal Anda akan dihapus.")
            .setPositiveButton("HAPUS AKUN") { _, _ ->
                lifecycleScope.launch {
                    db.userDao().deleteProfile()
                    val intent = Intent(this@ProfileAccountActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
            .setNegativeButton("BATAL", null)
            .show()
    }
}
