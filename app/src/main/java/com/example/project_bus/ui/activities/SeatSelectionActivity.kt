package com.example.project_bus.ui.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.project_bus.R

class SeatSelectionActivity : AppCompatActivity() {

    private val selectedSeats = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seat_selection)

        // Load booked seats from Supabase
        // Max 5 seats per booking
    }

    private fun toggleSeat(seatId: String) {
        if (selectedSeats.size >= 5 && !selectedSeats.contains(seatId)) {
            Toast.makeText(this, "Maximum 5 seats allowed", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedSeats.contains(seatId)) {
            selectedSeats.remove(seatId)
        } else {
            selectedSeats.add(seatId)
        }
    }
}
