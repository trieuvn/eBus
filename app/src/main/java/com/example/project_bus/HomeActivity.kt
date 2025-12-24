package com.example.project_bus

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout // Import đúng loại View trong XML
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton // Import đúng loại Button

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // 1. Khai báo đúng loại AppCompatButton như trong XML
        val btnSearch = findViewById<AppCompatButton>(R.id.btnSearch)
        btnSearch.setOnClickListener {
            startActivity(Intent(this, BookingActivity::class.java))
        }

        // 2. Khai báo đúng loại LinearLayout cho navTicket
        // Lưu ý: ID 'navTicket' trong XML hiện tại đang nằm ở phần Ticket mẫu, không phải ở BottomNav
        val navTicket = findViewById<LinearLayout>(R.id.navTicket)
        navTicket?.setOnClickListener {
            startActivity(Intent(this, BookingActivity::class.java))
        }
    }
}