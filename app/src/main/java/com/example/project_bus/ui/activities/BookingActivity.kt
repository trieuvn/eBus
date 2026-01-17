package com.example.project_bus.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.project_bus.R

class BookingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking)

        // Flow:
        // Receive FROM, TO, DATE
        // Load trips by route + date
        // Display RecyclerView
    }
}
