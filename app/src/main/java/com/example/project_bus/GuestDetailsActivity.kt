package com.example.project_bus

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class GuestDetailsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guest_details)

        val btnProceedPayment = findViewById<Button>(R.id.btnProceedPayment)

        btnProceedPayment.setOnClickListener {
            startActivity(Intent(this, PaymentActivity::class.java))
        }
    }
}