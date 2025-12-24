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
import com.example.project_bus.MainActivity
import com.example.project_bus.R
import com.example.project_bus.data.services.AuthService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private val authService = AuthService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

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
                    btnSignIn.isEnabled = false

                    lifecycleScope.launch {
                        try {
                            val profile = withContext(Dispatchers.IO) {
                                authService.signIn(email, password)
                            }

                            // profile có thể null nếu bạn chưa tạo trigger/policy cho bảng User
                            toast("Đăng nhập OK ✅ ${profile?.fullName ?: ""}".trim())

                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                            finish()
                        } catch (e: Exception) {
                            toast("Đăng nhập thất bại: ${friendlyAuthMessage(e)}")
                        } finally {
                            btnSignIn.isEnabled = true
                        }
                    }
                }
            }
        }

        txtGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    private fun friendlyAuthMessage(e: Throwable): String {
        val msg = (e.message ?: e.toString())
        return when {
            msg.contains("email_not_confirmed", ignoreCase = true) ->
                "Email chưa xác thực. Mở email để xác thực trước."
            msg.contains("invalid_credentials", ignoreCase = true) ||
                    msg.contains("Invalid login credentials", ignoreCase = true) ->
                "Sai email hoặc mật khẩu."
            msg.contains("user_banned", ignoreCase = true) ->
                "Tài khoản đang bị khóa."
            else -> msg
        }
    }
}
