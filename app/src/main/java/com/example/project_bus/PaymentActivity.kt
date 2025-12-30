package com.example.project_bus

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class PaymentActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        val btnPayNow = findViewById<Button>(R.id.btnPayNow)

        btnPayNow.setOnClickListener {
            // Logic thanh toán...
            startActivity(Intent(this, PaymentSuccessActivity::class.java))
            finish()
        }
    }
}