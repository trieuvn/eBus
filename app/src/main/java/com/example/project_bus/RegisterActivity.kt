package com.example.project_bus

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register)

        // Giả định ID nút đăng ký là btnRegister (bạn hãy thêm android:id="@+id/btnRegister" vào XML nếu chưa có)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        // Giả định ID text chuyển sang đăng nhập
        val tvSignIn = findViewById<TextView>(R.id.tvSignIn)

        btnRegister.setOnClickListener {
            // Chuyển sang màn hình thông báo đăng ký thành công
            val intent = Intent(this, SignupSuccessActivity::class.java)
            startActivity(intent)
            finish() // Đóng Activity đăng ký
        }

        tvSignIn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}