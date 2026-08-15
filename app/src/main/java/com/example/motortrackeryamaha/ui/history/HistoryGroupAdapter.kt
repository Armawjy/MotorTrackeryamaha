package com.example.motortrackeryamaha.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.motortrackeryamaha.databinding.ItemHistoryGroupBinding
import java.text.SimpleDateFormat
import java.util.*

class HistoryGroupAdapter(
    private val onItemClick: (HistoryItem) -> Unit,
    private val onItemLongClick: (HistoryItem) -> Unit
) : RecyclerView.Adapter<HistoryGroupAdapter.ViewHolder>() {

    private var groups = listOf<HistoryGroup>()

    fun submitGroups(newGroups: List<HistoryGroup>) {
        groups = newGroups
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(groups[position])
    }

    override fun getItemCount() = groups.size

    inner class ViewHolder(private val binding: ItemHistoryGroupBinding) : RecyclerView.ViewHolder(binding.root) {
        private val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))

        fun bind(group: HistoryGroup) {
            binding.tvGroupDate.text = sdf.format(Date(group.date))
            
            val itemAdapter = HistoryAdapter(onItemClick, onItemLongClick)
            binding.rvGroupItems.apply {
                layoutManager = LinearLayoutManager(itemView.context)
                adapter = itemAdapter
            }
            itemAdapter.submitList(group.items)
        }
    }
}
