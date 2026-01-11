package com.example.project_bus.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
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
        val btnGoogleSignIn = findViewById<LinearLayout>(R.id.btnGoogleSignIn)

        btnSignIn.setOnClickListener {
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString()

            when {
                email.isBlank() -> toast("Please enter your email")
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> toast("Invalid email address")
                password.isBlank() -> toast("Please enter your password")
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
                            toast("Signed in successfully!")

                            // Chuyển sang HomeActivity
                            val intent = Intent(this@LoginActivity, HomeActivity::class.java)
                            // Xóa hết các màn hình cũ (Login, Main) khỏi bộ nhớ để user không bấm Back quay lại được
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                            // -------------------------------

                        } catch (e: Exception) {
                            toast("Error: ${friendlyAuthMessage(e)}")
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

        // Google OAuth sign-in
        btnGoogleSignIn.setOnClickListener {
            btnGoogleSignIn.isEnabled = false
            lifecycleScope.launch {
                try {
                    authService.signInWithGoogle()
                    toast("Continue with Google in your browser...")
                    // Sau khi login xong, Supabase sẽ redirect về deeplink và AuthCallbackActivity sẽ tự đưa về Home.
                } catch (e: Exception) {
                    toast("Error: ${friendlyAuthMessage(e)}")
                } finally {
                    btnGoogleSignIn.isEnabled = true
                }
            }
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun friendlyAuthMessage(e: Throwable): String {
        val msg = e.message ?: e.toString()
        return when {
            msg.contains("invalid_credentials", true) -> "Incorrect email or password."
            msg.contains("email_not_confirmed", true) -> "Email not verified. Please check your inbox and click the verification link."
            else -> msg
        }
    }
}

