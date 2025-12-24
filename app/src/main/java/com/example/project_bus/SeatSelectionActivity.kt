package com.example.project_bus

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SeatSelectionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seat_selection)

        // Nút "Process" hoặc "Next"
        val btnNext = findViewById<TextView>(R.id.txt_upper)

        btnNext.setOnClickListener {
            startActivity(Intent(this, BoardingDropActivity::class.java))
        }
    }
}