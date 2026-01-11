package com.example.project_bus.ui.auth

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.project_bus.HomeActivity
import com.example.project_bus.R
import com.example.project_bus.data.services.AuthService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VerifyOtpActivity : AppCompatActivity() {

    private val authService = AuthService()

    private var expiryTimer: CountDownTimer? = null
    private var resendCooldownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.verify_otp)

        val email = intent.getStringExtra(EXTRA_EMAIL)?.trim().orEmpty()
        if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            toast("Missing email for verification")
            finish()
            return
        }

        val autoResend = intent.getBooleanExtra(EXTRA_AUTO_RESEND, false)

        val tvEmail = findViewById<TextView>(R.id.tvEmail)
        val edtOtp = findViewById<EditText>(R.id.edtOtp)
        val tvOtpExpiry = findViewById<TextView>(R.id.tvOtpExpiry)
        val btnVerify = findViewById<Button>(R.id.btnVerify)
        val btnResend = findViewById<Button>(R.id.btnResend)
        val tvResendCooldown = findViewById<TextView>(R.id.tvResendCooldown)

        tvEmail.text = email

        startOtpExpiryCountdown(tvOtpExpiry, btnVerify)

        // Supabase rate-limit: by default you can request an OTP once per 60s.
        startResendCooldownCountdown(tvResendCooldown, btnResend)

        btnVerify.setOnClickListener {
            val otp = edtOtp.text.toString().trim()
            when {
                otp.length != 6 -> toast("Please enter all 6 digits")
                else -> verifyOtp(email, otp, btnVerify)
            }
        }

        btnResend.setOnClickListener {
            resendOtp(email, btnResend, tvOtpExpiry, btnVerify, tvResendCooldown)
        }

        if (autoResend) {
            // Used when the user tried to sign in but got email_not_confirmed.
            // Automatically request a new code.
            btnResend.performClick()
        }
    }

    private fun verifyOtp(email: String, otp: String, btnVerify: Button) {
        btnVerify.isEnabled = false
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    authService.verifySignupOtp(email, otp)
                }
                toast("Verified ✅")

                val intent = Intent(this@VerifyOtpActivity, HomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                toast("Verification failed: ${friendlyAuthMessage(e)}")
                btnVerify.isEnabled = true
            }
        }
    }

    private fun resendOtp(
        email: String,
        btnResend: Button,
        tvOtpExpiry: TextView,
        btnVerify: Button,
        tvResendCooldown: TextView
    ) {
        btnResend.isEnabled = false
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    authService.resendSignupOtp(email)
                }
                toast("Code sent. Please check your inbox 📩")

                // Reset countdown
                startOtpExpiryCountdown(tvOtpExpiry, btnVerify)
                startResendCooldownCountdown(tvResendCooldown, btnResend)
            } catch (e: Exception) {
                toast("Resend failed: ${friendlyAuthMessage(e)}")
                btnResend.isEnabled = true
            }
        }
    }

    private fun startOtpExpiryCountdown(tv: TextView, btnVerify: Button) {
        expiryTimer?.cancel()
        btnVerify.isEnabled = true

        // 30s UI countdown (as requested). Actual validity depends on "Email OTP Expiration" on Supabase.
        expiryTimer = object : CountDownTimer(30_000, 1_000) {
            override fun onTick(millisUntilFinished: Long) {
                val sec = (millisUntilFinished / 1000).toInt()
                tv.text = "Code expires in: ${sec}s"
            }

            override fun onFinish() {
                tv.text = "Code expired. Tap 'Resend code'."
                // Disable verify to make UX clearer (Supabase will usually return token expired).
                btnVerify.isEnabled = false
            }
        }.start()
    }

    private fun startResendCooldownCountdown(tv: TextView, btnResend: Button) {
        resendCooldownTimer?.cancel()
        btnResend.isEnabled = false

        resendCooldownTimer = object : CountDownTimer(60_000, 1_000) {
            override fun onTick(millisUntilFinished: Long) {
                val sec = (millisUntilFinished / 1000).toInt()
                tv.text = "You can resend in: ${sec}s"
            }

            override fun onFinish() {
                tv.text = "You can resend a new code."
                btnResend.isEnabled = true
            }
        }.start()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun friendlyAuthMessage(e: Throwable): String {
        val msg = e.message ?: e.toString()
        return when {
            msg.contains("email_address_not_authorized", true) ->
                "Your project is using Supabase's default email provider, which only sends to organization members. Configure Custom SMTP."
            msg.contains("too many requests", true) || msg.contains("rate limit", true) ->
                "Too many requests. Please try again later."
            msg.contains("otp_expired", true) || msg.contains("expired", true) ->
                "Code expired. Tap 'Resend code'."
            msg.contains("invalid", true) ->
                "Invalid code. Please check and try again."
            else -> msg
        }
    }

    override fun onDestroy() {
        expiryTimer?.cancel()
        resendCooldownTimer?.cancel()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_EMAIL = "extra_email"
        const val EXTRA_AUTO_RESEND = "extra_auto_resend"
    }
}


