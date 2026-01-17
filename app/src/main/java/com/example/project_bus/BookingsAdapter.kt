package com.example.project_bus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class BookingItem(
    val bookingId: Long,
    val index: Int,
    val fromLoc: String,
    val toLoc: String,
    val timeAndDay: String,
    val fullDate: String
)

class BookingsAdapter(
    private val list: List<BookingItem>,
    private val onItemClick: (Long) -> Unit
) : RecyclerView.Adapter<BookingsAdapter.BookingViewHolder>() {

    class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvHistoryIndex: TextView = itemView.findViewById(R.id.tvHistoryIndex)
        val tvHistoryOrigin: TextView = itemView.findViewById(R.id.tvHistoryOrigin)
        val tvHistoryDestination: TextView = itemView.findViewById(R.id.tvHistoryDestination)
        val tvHistoryTime: TextView = itemView.findViewById(R.id.tvHistoryTime)
        val tvHistoryDate: TextView = itemView.findViewById(R.id.tvHistoryDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_booking, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val item = list[position]
        holder.tvHistoryIndex.text = item.index.toString()
        holder.tvHistoryOrigin.text = "From: ${item.fromLoc}"
        holder.tvHistoryDestination.text = "To: ${item.toLoc}"
        holder.tvHistoryTime.text = item.timeAndDay
        holder.tvHistoryDate.text = item.fullDate

        holder.itemView.setOnClickListener {
            onItemClick(item.bookingId)
        }
    }

    override fun getItemCount(): Int = list.size
}
