package com.example.project_bus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Model dữ liệu cho danh sách vé
data class BookingItem(
    val index: Int,
    val fromLoc: String,
    val toLoc: String,
    val time: String,
    val date: String
)

class BookingsAdapter(private val list: List<BookingItem>) :
    RecyclerView.Adapter<BookingsAdapter.BookingViewHolder>() {

    class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvIndex: TextView = itemView.findViewById(R.id.tvIndex)
        val tvFrom: TextView = itemView.findViewById(R.id.tvFrom)
        val tvTo: TextView = itemView.findViewById(R.id.tvTo)
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_booking, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val item = list[position]
        holder.tvIndex.text = item.index.toString()
        holder.tvFrom.text = item.fromLoc
        holder.tvTo.text = item.toLoc
        holder.tvTime.text = item.time
        holder.tvDate.text = item.date
    }

    override fun getItemCount(): Int = list.size
}
