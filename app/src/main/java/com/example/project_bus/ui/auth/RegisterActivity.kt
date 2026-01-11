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
        val btnGoogleSignUp = findViewById<LinearLayout>(R.id.btnGoogleSignUp)

        btnSignUp.setOnClickListener {
            val fullName = edtFullName.text.toString().trim()
            val email = edtEmail.text.toString().trim()
            val phone = edtPhone.text.toString().trim()
            val password = edtPassword.text.toString()

            // Validate nhanh để tránh gọi API vô ích
            when {
                fullName.isBlank() -> toast("Please enter your full name")
                email.isBlank() -> toast("Please enter your email")
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> toast("Invalid email address")
                email.endsWith("@test.com", true) || email.endsWith("@example.com", true) ->
                    toast("Please use a real email address (e.g., Gmail).")
                password.trim().length < 6 -> toast("Password must be at least 6 characters")
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
                                toast("Sign-up successful. Please check your email and click the verification link, then come back to sign in.")
                            } else {
                                toast("Sign-up successful ✅")
                            }

                            startActivity(Intent(this@RegisterActivity, SignupSuccessActivity::class.java).putExtra("email", email))
                            finish()
                        } catch (e: Exception) {
                            toast("Sign-up failed: ${friendlyAuthMessage(e)}")
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

        // Google OAuth sign-up / sign-in
        btnGoogleSignUp.setOnClickListener {
            btnGoogleSignUp.isEnabled = false
            lifecycleScope.launch {
                try {
                    authService.signInWithGoogle()
                    toast("Continue with Google in your browser...")
                    // AuthCallbackActivity sẽ tự đưa về Home sau khi callback.
                } catch (e: Exception) {
                    toast("Error: ${friendlyAuthMessage(e)}")
                } finally {
                    btnGoogleSignUp.isEnabled = true
                }
            }
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    private fun friendlyAuthMessage(e: Throwable): String {
        val msg = (e.message ?: e.toString())
        return when {
            msg.contains("email_address_invalid", ignoreCase = true) ->
                "Invalid email address (avoid @test.com/@example.com)."
            msg.contains("email_address_not_authorized", ignoreCase = true) ->
                "Email delivery is blocked because the project is using Supabase's default SMTP (it only sends to project members). For development you can temporarily disable \"Confirm email\", or configure a custom SMTP for production."
            msg.contains("email_not_confirmed", ignoreCase = true) ->
                "Email not verified. Please check your inbox and click the verification link, then sign in."
            msg.contains("user_already_exists", ignoreCase = true) ||
                    msg.contains("already registered", ignoreCase = true) ->
                "This email is already registered. Please sign in."
            msg.contains("weak_password", ignoreCase = true) ->
                "Weak password. Please use a stronger password."
            else -> msg
        }
    }
}


