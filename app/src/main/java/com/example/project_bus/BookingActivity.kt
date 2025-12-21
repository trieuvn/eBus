package com.example.project_bus

import android.content.Intent
import android.os.Bundle
import android.widget.RelativeLayout
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

class BookingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking)

        // 1. Khớp với RelativeLayout (item xe thứ nhất)
        val cvBusItem = findViewById<RelativeLayout>(R.id.cvBusItem)
        cvBusItem?.setOnClickListener {
            val intent = Intent(this, SeatSelectionActivity::class.java)
            startActivity(intent)
        }

        // 2. Xử lý Bottom Navigation (Nếu người dùng muốn chuyển hướng)
        val navHome = findViewById<android.widget.ImageView>(R.id.navHome)
        navHome?.setOnClickListener {
            finish() // Quay lại HomeActivity
        }

        val navTicket = findViewById<FrameLayout>(R.id.navTicket)
        navTicket?.setOnClickListener {
            // Đang ở trang Booking/Ticket rồi nên có thể không cần làm gì hoặc reload
        }
    }
}