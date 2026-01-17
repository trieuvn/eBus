package com.example.project_bus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.project_bus.data.model.Trip
import java.text.SimpleDateFormat
import java.util.Locale

class TripsAdapter(
    private val tripList: List<Trip>,
    private val onTripClick: (Trip) -> Unit
) : RecyclerView.Adapter<TripsAdapter.TripViewHolder>() {

    class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvOperator: TextView = itemView.findViewById(R.id.tvOperatorName)
        val tvPrice: TextView = itemView.findViewById(R.id.tvTripPrice)
        val tvBusType: TextView = itemView.findViewById(R.id.tvBusType)
        val tvSeats: TextView = itemView.findViewById(R.id.tvSeatsAvailable)
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

        // 1. Set Operator Name & Price
        holder.tvOperator.text = trip.operatorName ?: "Unknown Bus"
        holder.tvPrice.text = "LKR ${trip.price}"

        // 2. Set Bus Type & Seats
        holder.tvBusType.text = trip.busType ?: "Standard"
        val seatsLeft = trip.totalSeats // Hoặc tính toán seatsAvailable nếu có logic
        holder.tvSeats.text = "$seatsLeft Seats left"

        // 3. Format Time (Giả sử startTime là string "HH:mm:ss" hoặc "HH:mm")
        // Nếu DB lưu kiểu khác, bạn cần sửa logic này.
        val start = formatTime(trip.startTime)
        val end = formatTime(trip.endTime)
        
        holder.tvStartTime.text = start
        holder.tvEndTime.text = end
        
        // Tính duration giả định (bạn nên tính chính xác từ object Trip)
        holder.tvDuration.text = calculateDuration(start, end)

        // 4. Handle Click
        holder.itemView.setOnClickListener {
            onTripClick(trip)
        }
    }

    override fun getItemCount(): Int = tripList.size

    // Helper: Cắt chuỗi lấy giờ:phút (Ví dụ: "08:00:00" -> "08:00")
    private fun formatTime(time: String?): String {
        if (time.isNullOrEmpty()) return "--:--"
        return try {
            time.substring(0, 5) 
        } catch (e: Exception) {
            time
        }
    }

    // Helper: Tính khoảng thời gian đơn giản
    private fun calculateDuration(start: String, end: String): String {
        // Logic tính toán đơn giản, thực tế nên dùng Date/Time library
        return "Direct" 
    }
}
