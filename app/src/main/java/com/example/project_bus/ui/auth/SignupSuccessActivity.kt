package com.example.project_bus.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.project_bus.R
import com.example.project_bus.data.services.AuthService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SignupSuccessActivity : AppCompatActivity() {

    private val authService = AuthService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup_success)

        val email = intent.getStringExtra("email")?.trim().orEmpty()

        val tvResend = findViewById<TextView>(R.id.tvResendEmail)
        val tvSignIn = findViewById<TextView>(R.id.tvSignIn)

        tvResend.setOnClickListener {
            if (email.isBlank()) {
                Toast.makeText(
                    this,
                    "Missing email address. Please go back and sign up again.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        authService.resendSignupConfirmation(email)
                    }
                    Toast.makeText(
                        this@SignupSuccessActivity,
                        "Verification email sent. Please check your inbox (and spam).",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Exception) {
                    Toast.makeText(
                        this@SignupSuccessActivity,
                        "Failed to resend verification email: ${e.message ?: e}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        tvSignIn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}

