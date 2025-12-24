package com.example.project_bus

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SignupSuccessActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup_success)

        // ID lấy từ signup_success.xml
        val tvSignIn = findViewById<TextView>(R.id.tvSignIn)

//        tvSignIn.setOnClickListener {
//            val intent = Intent(this, LoginActivity::class.java)
//            // Xóa stack để người dùng không back lại màn hình success
//            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//            startActivity(intent)
//        }
    }
}