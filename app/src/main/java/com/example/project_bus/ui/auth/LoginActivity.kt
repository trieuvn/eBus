package com.example.project_bus.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.project_bus.HomeActivity // <--- Quan trọng: Import HomeActivity
import com.example.project_bus.R
import com.example.project_bus.data.services.AuthService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private val authService = AuthService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login) // Đảm bảo tên layout đúng là activity_login hoặc login

        val edtEmail = findViewById<EditText>(R.id.edtEmail)
        val edtPassword = findViewById<EditText>(R.id.edtPassword)
        val btnSignIn = findViewById<Button>(R.id.btnSignIn)
        val txtGoRegister = findViewById<TextView>(R.id.txtGoRegister)

        btnSignIn.setOnClickListener {
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString()

            when {
                email.isBlank() -> toast("Vui lòng nhập email")
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> toast("Email không hợp lệ")
                password.isBlank() -> toast("Vui lòng nhập mật khẩu")
                else -> {
                    // Disable nút để tránh bấm nhiều lần
                    btnSignIn.isEnabled = false

                    lifecycleScope.launch {
                        try {
                            // Gọi hàm đăng nhập
                            val profile = withContext(Dispatchers.IO) {
                                authService.signIn(email, password)
                            }

                            // --- KHU VỰC QUAN TRỌNG NHẤT ---
                            toast("Đăng nhập thành công!")

                            // Chuyển sang HomeActivity
                            val intent = Intent(this@LoginActivity, HomeActivity::class.java)
                            // Xóa hết các màn hình cũ (Login, Main) khỏi bộ nhớ để user không bấm Back quay lại được
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                            // -------------------------------

                        } catch (e: Exception) {
                            toast("Lỗi: ${friendlyAuthMessage(e)}")
                            btnSignIn.isEnabled = true // Mở lại nút nếu lỗi
                        }
                    }
                }
            }
        }

        // Chuyển sang trang đăng ký
        txtGoRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun friendlyAuthMessage(e: Throwable): String {
        val msg = e.message ?: e.toString()
        return when {
            msg.contains("invalid_credentials", true) -> "Sai email hoặc mật khẩu"
            msg.contains("email_not_confirmed", true) -> "Email chưa xác thực"
            else -> msg
        }
    }
}