package com.example.project_bus

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class BoardingDropActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_boarding_drop)

        val btnNext = findViewById<Button>(R.id.btnNext)

        btnNext.setOnClickListener {
            startActivity(Intent(this, GuestDetailsActivity::class.java))
        }
    }
}