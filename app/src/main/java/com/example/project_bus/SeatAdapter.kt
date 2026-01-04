package com.example.project_bus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

// Model ghế
data class Seat(
    val id: String,
    var status: Int // 0: Available, 1: Booked, 2: Selected, -1: Aisle (Lối đi)
)

class SeatAdapter(
    private val seats: List<Seat>,
    private val onSeatClick: (Seat) -> Unit
) : RecyclerView.Adapter<SeatAdapter.SeatViewHolder>() {

    inner class SeatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val viewSeat: View = itemView.findViewById(R.id.viewSeat)

        fun bind(seat: Seat) {
            // Xử lý Lối đi (Ẩn View)
            if (seat.status == -1) {
                viewSeat.visibility = View.INVISIBLE
                viewSeat.setOnClickListener(null)
                return
            } else {
                viewSeat.visibility = View.VISIBLE
            }

            // Đổi màu
            val bgRes = when (seat.status) {
                1 -> R.drawable.bg_seat_booked
                2 -> R.drawable.bg_seat_selected
                else -> R.drawable.bg_seat_available
            }
            viewSeat.setBackgroundResource(bgRes)

            // Sự kiện Click
            viewSeat.setOnClickListener {
                if (seat.status != 1) { // Không cho chọn ghế đã Booked
                    onSeatClick(seat)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_seat, parent, false)
        return SeatViewHolder(view)
    }

    override fun onBindViewHolder(holder: SeatViewHolder, position: Int) {
        holder.bind(seats[position])
    }

    override fun getItemCount() = seats.size
}

