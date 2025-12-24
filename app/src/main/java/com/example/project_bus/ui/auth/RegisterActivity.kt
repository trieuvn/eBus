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
import com.example.project_bus.R
import com.example.project_bus.data.services.AuthService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterActivity : AppCompatActivity() {

    private val authService = AuthService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register)

        val edtFullName = findViewById<EditText>(R.id.edtFullName)
        val edtEmail = findViewById<EditText>(R.id.edtEmail)
        val edtPhone = findViewById<EditText>(R.id.edtPhone)
        val edtPassword = findViewById<EditText>(R.id.edtPassword)
        val btnSignUp = findViewById<Button>(R.id.btnSignUp)
        val txtGoLogin = findViewById<TextView>(R.id.txtGoLogin)

        btnSignUp.setOnClickListener {
            val fullName = edtFullName.text.toString().trim()
            val email = edtEmail.text.toString().trim()
            val phone = edtPhone.text.toString().trim()
            val password = edtPassword.text.toString()

            // Validate nhanh để tránh gọi API vô ích
            when {
                fullName.isBlank() -> toast("Vui lòng nhập tên")
                email.isBlank() -> toast("Vui lòng nhập email")
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> toast("Email không hợp lệ")
                email.endsWith("@test.com", true) || email.endsWith("@example.com", true) ->
                    toast("Supabase Auth không hỗ trợ domain @test.com/@example.com. Dùng email thật (gmail...)")
                password.trim().length < 6 -> toast("Mật khẩu tối thiểu 6 ký tự")
                else -> {
                    btnSignUp.isEnabled = false

                    lifecycleScope.launch {
                        try {
                            val result = withContext(Dispatchers.IO) {
                                authService.signUp(
                                    email = email,
                                    password = password,
                                    fullName = fullName,
                                    phoneNumber = phone,
                                    role = 0 // passenger mặc định
                                )
                            }

                            if (result.needsEmailConfirmation) {
                                toast("Đăng ký OK. Mở email để xác thực rồi quay lại đăng nhập.")
                            } else {
                                toast("Đăng ký OK ✅")
                            }

                            startActivity(Intent(this@RegisterActivity, SignupSuccessActivity::class.java))
                            finish()
                        } catch (e: Exception) {
                            toast("Đăng ký thất bại: ${friendlyAuthMessage(e)}")
                        } finally {
                            btnSignUp.isEnabled = true
                        }
                    }
                }
            }
        }

        txtGoLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    private fun friendlyAuthMessage(e: Throwable): String {
        val msg = (e.message ?: e.toString())
        return when {
            msg.contains("email_address_invalid", ignoreCase = true) ->
                "email_address_invalid (đừng dùng @test.com/@example.com)"
            msg.contains("email_address_not_authorized", ignoreCase = true) ->
                "Email bị chặn do project đang dùng SMTP mặc định của Supabase (chỉ gửi cho member). " +
                "Dev nhanh: tắt Confirm email. Chuẩn: cấu hình SMTP riêng."
            msg.contains("email_not_confirmed", ignoreCase = true) ->
                "Email chưa xác thực. Mở email để xác thực rồi đăng nhập."
            msg.contains("user_already_exists", ignoreCase = true) ||
                    msg.contains("already registered", ignoreCase = true) ->
                "Email đã tồn tại. Hãy đăng nhập."
            msg.contains("weak_password", ignoreCase = true) ->
                "Mật khẩu yếu. Hãy dùng mật khẩu mạnh hơn."
            else -> msg
        }
    }
}
