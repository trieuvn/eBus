package com.example.project_bus.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.project_bus.R
import com.example.project_bus.data.models.Trip

class TripsAdapter(
    private val tripList: List<Trip>,
    private val onTripClick: (Trip) -> Unit
) : RecyclerView.Adapter<TripsAdapter.TripViewHolder>() {

    class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvOperator: TextView = itemView.findViewById(R.id.tvOperatorName)
        val tvPrice: TextView = itemView.findViewById(R.id.tvTripPrice)
        val tvBusType: TextView = itemView.findViewById(R.id.tvBusType)
        val tvSeatsLeft: TextView = itemView.findViewById(R.id.tvSeatsAvailable)
        val tvStartTime: TextView = itemView.findViewById(R.id.tvStartTime)
        val tvEndTime: TextView = itemView.findViewById(R.id.tvEndTime)
        val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trip, parent, false)
        return TripViewHolder(view)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        val trip = tripList[position]

        holder.tvOperator.text = trip.operatorName ?: "Unknown"
        holder.tvPrice.text = "LKR ${trip.price}"
        holder.tvBusType.text = trip.busType ?: "Standard"
        holder.tvSeatsLeft.text = "${trip.totalSeats} Seats Left"

        val start = formatTime(trip.departureTime)
        val end = formatTime(trip.arrivalTime)

        // Dòng thời gian nằm đúng chỗ (không còn hiện 2025-)
        holder.tvStartTime.text = "$start  —  $end"
        holder.tvEndTime.text = "" // tvEndTime đang GONE trong layout

        // Bạn có thể thay bằng tính phút nếu muốn; hiện tại giữ "Direct" như cũ
        holder.tvDuration.text = "Direct"

        holder.itemView.setOnClickListener { onTripClick(trip) }
    }

    override fun getItemCount(): Int = tripList.size

    /**
     * Fix lỗi DB trả về dạng "2025-01-16 09:00:00" hoặc "2025-01-16T09:00:00"
     * mà code cũ substring(0,5) => "2025-"
     */
    private fun formatTime(raw: String?): String {
        if (raw.isNullOrBlank()) return "--:--"

        val candidate = when {
            raw.contains("T") -> raw.substringAfter("T")   // ISO: 2025-01-16T09:00:00
            raw.contains(" ") -> raw.substringAfter(" ")   // SQL: 2025-01-16 09:00:00
            else -> raw                                     // 09:00:00 hoặc 09:00
        }.trim()

        // Lấy HH:mm
        return if (candidate.length >= 5 && candidate[2] == ':') {
            candidate.substring(0, 5)
        } else {
            "--:--"
        }
    }
}
