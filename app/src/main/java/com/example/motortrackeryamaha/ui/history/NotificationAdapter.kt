package com.example.motortrackeryamaha.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.motortrackeryamaha.R
import com.example.motortrackeryamaha.data.NotificationRecord
import com.example.motortrackeryamaha.databinding.ItemHistoryTripBinding
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    private var items = mutableListOf<NotificationRecord>()

    fun submitList(newItems: List<NotificationRecord>) {
        items = newItems.toMutableList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryTripBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    class ViewHolder(private val binding: ItemHistoryTripBinding) : RecyclerView.ViewHolder(binding.root) {
        private val sdf = SimpleDateFormat("dd MMMM HH:mm", Locale.getDefault())

        fun bind(item: NotificationRecord) {
            binding.tvTitle.text = item.title
            binding.tvSubtitle.text = item.message
            binding.tvExtraInfo.text = sdf.format(Date(item.timestamp))
            
            val icon = when(item.category) {
                "Oil" -> R.drawable.ic_oil
                "Trip" -> R.drawable.ic_trip
                else -> R.drawable.ic_notifications
            }
            binding.ivIcon.setImageResource(icon)
            
            val color = when(item.type) {
                3 -> R.color.status_danger
                2 -> R.color.status_warning
                else -> R.color.accent_blue
            }
            binding.ivIcon.imageTintList = ContextCompat.getColorStateList(itemView.context, color)
        }
    }
}
