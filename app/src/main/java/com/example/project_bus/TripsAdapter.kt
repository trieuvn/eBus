package com.example.project_bus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.project_bus.data.models.Trip

class TripsAdapter(
    private val trips: List<Trip>,
    private val onClick: (Trip) -> Unit
) : RecyclerView.Adapter<TripsAdapter.TripViewHolder>() {

    class TripViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvBusName: TextView = view.findViewById(R.id.tvBusName)
        val tvBusType: TextView = view.findViewById(R.id.tvBusType) // Mới
        val tvPrice: TextView = view.findViewById(R.id.tvPrice)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val tvSeatsLeft: TextView = view.findViewById(R.id.tvSeatsLeft) // Mới
        // Nút Book giờ là toàn bộ CardView
        val cardView: View = view
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trip, parent, false)
        return TripViewHolder(view)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        val trip = trips[position]

        // Tên nhà xe (Nếu trong DB chưa có cột operator_name thì fix cứng test trước)
        holder.tvBusName.text = trip.operatorName ?: "Perera Travels"

        // Loại xe
        holder.tvBusType.text = trip.busType ?: "A/C Sleeper (2+1)"

        // Giá tiền (Thêm chữ LKR và định dạng số)
        holder.tvPrice.text = "LKR ${trip.price.toInt()}"

        // Thời gian (Giả lập giờ đến bằng cách cộng 45p vào giờ đi cho giống mẫu)
        val startTime = trip.departureTime?.take(5) ?: "09:00"
        holder.tvTime.text = "$startTime AM - ... AM"

        // Số ghế (Giả lập hoặc lấy từ DB)
        holder.tvSeatsLeft.text = "15 Seats left"

        // Bấm vào cả thẻ để đặt vé
        holder.cardView.setOnClickListener {
            onClick(trip)
        }
    }

    override fun getItemCount() = trips.size
}