package com.example.motortrackeryamaha

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.motortrackeryamaha.databinding.ActivityHelpCenterBinding

class HelpCenterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHelpCenterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHelpCenterBinding.inflate(layoutInflater)
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

        setupFaq()
        
        binding.btnContactSupport.setOnClickListener {
            Toast.makeText(this, "Menghubungi dukungan...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupFaq() {
        val faqs = listOf(
            FaqItem("Bagaimana memulai perjalanan?", "Klik tombol 'MULAI PERJALANAN' di halaman beranda. Pastikan izin lokasi aktif."),
            FaqItem("Bagaimana menghentikan perjalanan?", "Klik tombol 'SELESAI PERJALANAN' saat pelacakan aktif."),
            FaqItem("Bagaimana GPS bekerja?", "Aplikasi menggunakan sensor GPS internal ponsel untuk melacak koordinat secara offline."),
            FaqItem("Bagaimana mencatat ganti oli?", "Buka menu Perawatan, lalu klik tombol '+' dan pilih 'Ganti Oli'."),
            FaqItem("Bagaimana melakukan backup?", "Buka Pengaturan > Backup & Restore > Buat Backup."),
            FaqItem("Bagaimana restore data?", "Buka Pengaturan > Backup & Restore > Pilih File Backup.")
        )
        
        binding.rvFaq.apply {
            layoutManager = LinearLayoutManager(this@HelpCenterActivity)
            adapter = FaqAdapter(faqs)
        }
    }

    data class FaqItem(val question: String, val answer: String, var isExpanded: Boolean = false)

    class FaqAdapter(private val items: List<FaqItem>) : RecyclerView.Adapter<FaqAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvQuestion: TextView = view.findViewById(android.R.id.text1)
            val tvAnswer: TextView = view.findViewById(android.R.id.text2)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvQuestion.text = item.question
            holder.tvQuestion.setTextColor(android.graphics.Color.WHITE)
            holder.tvAnswer.text = if (item.isExpanded) item.answer else ""
            holder.tvAnswer.setTextColor(android.graphics.Color.LTGRAY)
            
            holder.itemView.setOnClickListener {
                item.isExpanded = !item.isExpanded
                notifyItemChanged(position)
            }
        }

        override fun getItemCount() = items.size
    }
}
