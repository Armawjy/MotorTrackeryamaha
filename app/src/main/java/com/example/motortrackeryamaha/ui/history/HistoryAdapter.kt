package com.example.motortrackeryamaha.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.motortrackeryamaha.R
import com.example.motortrackeryamaha.databinding.ItemHistoryTripBinding
import java.text.SimpleDateFormat
import java.util.*

sealed class HistoryItem {
    data class TripItem(val trip: com.example.motortrackeryamaha.data.Trip) : HistoryItem()
    data class OilItem(val oil: com.example.motortrackeryamaha.data.OilChange) : HistoryItem()
    data class ServiceItem(val service: com.example.motortrackeryamaha.data.Service) : HistoryItem()
    data class ComponentItem(val comp: com.example.motortrackeryamaha.data.ComponentReplacement) : HistoryItem()
    data class EngineItem(val repair: com.example.motortrackeryamaha.data.EngineRepair) : HistoryItem()
}

class HistoryAdapter(
    private val onItemClick: (HistoryItem) -> Unit = { },
    private val onItemLongClick: (HistoryItem) -> Unit = { }
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    private var items = mutableListOf<HistoryItem>()

    fun submitList(newItems: List<HistoryItem>) {
        items = newItems.toMutableList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryTripBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
        holder.itemView.setOnClickListener { onItemClick(item) }
        holder.itemView.setOnLongClickListener { 
            onItemLongClick(item)
            true
        }
    }

    override fun getItemCount() = items.size

    class ViewHolder(private val binding: ItemHistoryTripBinding) : RecyclerView.ViewHolder(binding.root) {
        private val sdfTime = SimpleDateFormat("HH:mm", Locale("id", "ID"))

        fun bind(item: HistoryItem) {
            when (item) {
                is HistoryItem.TripItem -> {
                    binding.tvTitle.text = "Perjalanan"
                    binding.tvSubtitle.text = String.format(Locale.getDefault(), "%s → %s", item.trip.titikAwal.ifEmpty { "Trip" }, item.trip.titikAkhir.ifEmpty { "Trip" })
                    binding.tvExtraInfo.text = formatDuration(item.trip.durasi)
                    
                    // Specific style for reference image
                    val smallText = String.format(Locale.getDefault(), "📍 %.1f KM", item.trip.jarak)
                    // In a real app we'd use a Spannable or a separate view, 
                    // for now let's just update subtitle
                    binding.tvSubtitle.text = "${binding.tvSubtitle.text}\n$smallText"
                    
                    binding.ivIcon.setImageResource(R.drawable.ic_trip)
                    binding.ivIcon.imageTintList = ContextCompat.getColorStateList(itemView.context, R.color.accent_blue)
                }
                is HistoryItem.OilItem -> {
                    binding.tvTitle.text = "Ganti Oli Mesin"
                    binding.tvSubtitle.text = "Ganti oli mesin secara berkala\n⚙️ ${String.format(Locale.getDefault(), "%,.0f", item.oil.jarakSejakGanti)} KM"
                    binding.tvExtraInfo.text = "Rp " + String.format(Locale.getDefault(), "%,.0f", item.oil.biaya) + "\n" + sdfTime.format(Date(item.oil.tanggal))
                    binding.ivIcon.setImageResource(R.drawable.ic_oil)
                    binding.ivIcon.imageTintList = ContextCompat.getColorStateList(itemView.context, R.color.status_warning)
                }
                is HistoryItem.ServiceItem -> {
                    binding.tvTitle.text = item.service.jenisService
                    binding.tvSubtitle.text = "Pembersihan dan pengecekan\n⚙️ ${String.format(Locale.getDefault(), "%,.0f", item.service.kilometer)} KM"
                    binding.tvExtraInfo.text = "Rp " + String.format(Locale.getDefault(), "%,.0f", item.service.biaya) + "\n" + sdfTime.format(Date(item.service.tanggal))
                    binding.ivIcon.setImageResource(R.drawable.ic_build)
                    binding.ivIcon.imageTintList = ContextCompat.getColorStateList(itemView.context, R.color.status_safe)
                }
                // Handle others similarly...
                else -> {
                    binding.tvTitle.text = "Perawatan"
                    binding.ivIcon.setImageResource(R.drawable.ic_settings)
                }
            }
        }

        private fun formatDuration(millis: Long): String {
            val seconds = (millis / 1000) % 60
            val minutes = (millis / (1000 * 60)) % 60
            val hours = (millis / (1000 * 60 * 60)) % 24
            return if (hours > 0) String.format("%02d:%02d:%02d", hours, minutes, seconds)
                   else String.format("%02d:%02d", minutes, seconds)
        }
    }
}
