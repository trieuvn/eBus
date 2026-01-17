package com.example.project_bus.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.project_bus.R
import com.example.project_bus.data.models.Trip
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TripsAdapter(
    private val tripList: List<Trip>,
    private val dateStr: String,
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

        // LOGIC TĂNG GIÁ
        val isWeekend = isWeekend(dateStr)
        val finalPrice = if (isWeekend) trip.price * 1.1 else trip.price

        val priceStr = if (finalPrice % 1.0 == 0.0) {
            "%.0f".format(Locale.US, finalPrice) 
        } else {
            "%.2f".format(Locale.US, finalPrice)
        }
        
        if (isWeekend) {
            holder.tvPrice.text = "LKR $priceStr (+10%)"
            holder.tvPrice.setTextColor(android.graphics.Color.parseColor("#D50000"))
        } else {
            holder.tvPrice.text = "LKR $priceStr"
            holder.tvPrice.setTextColor(android.graphics.Color.parseColor("#FF9800"))
        }

        holder.tvBusType.text = trip.busType ?: "Standard"
        holder.tvSeatsLeft.text = "${trip.totalSeats} Seats Left"

        val start = formatTime(trip.departureTime)
        val end = formatTime(trip.arrivalTime)

        holder.tvStartTime.text = "$start  —  $end"
        holder.tvEndTime.text = ""
        holder.tvDuration.text = "Direct"

        holder.itemView.setOnClickListener { onTripClick(trip) }
    }

    override fun getItemCount(): Int = tripList.size

    private fun formatTime(raw: String?): String {
        if (raw.isNullOrBlank()) return "--:--"
        val candidate = when {
            raw.contains("T") -> raw.substringAfter("T")   
            raw.contains(" ") -> raw.substringAfter(" ")   
            else -> raw                                     
        }.trim()

        return if (candidate.length >= 5 && candidate[2] == ':') {
            candidate.substring(0, 5)
        } else {
            "--:--"
        }
    }

    private fun isWeekend(date: String): Boolean {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val d = sdf.parse(date) ?: return false
            val cal = Calendar.getInstance()
            cal.time = d
            val day = cal.get(Calendar.DAY_OF_WEEK)
            day == Calendar.SATURDAY || day == Calendar.SUNDAY
        } catch (e: Exception) {
            false
        }
    }
}