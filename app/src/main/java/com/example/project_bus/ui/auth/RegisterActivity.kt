package com.example.project_bus.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.lifecycleScope
import com.example.project_bus.BuildConfig
import com.example.project_bus.HomeActivity
import com.example.project_bus.R
import com.example.project_bus.data.SupabaseProvider
import com.example.project_bus.data.services.AuthService
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
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
        val btnGoogleSignUp = findViewById<View>(R.id.btnGoogleSignUp)

        val cbTerms = findViewById<android.widget.CheckBox>(R.id.cbTerms)

        btnSignUp.setOnClickListener {
            val fullName = edtFullName.text.toString().trim()
            val email = edtEmail.text.toString().trim()
            val phone = edtPhone.text.toString().trim()
            val password = edtPassword.text.toString()
            val isTermsChecked = cbTerms.isChecked

            // XSS / Sanitize Logic (Simple check)
            if (fullName.contains("<script>", true) || fullName.contains("javascript:", true)) {
                // System should sanitize or block. We block.
                toast("Invalid input detected.")
                return@setOnClickListener
            }
            
            // Validation Logic
            when {
                // 1. All fields empty (Name, Email, Password required)
                fullName.isBlank() && email.isBlank() && password.isBlank() -> toast("All fields are required.")

                // 2. Name Empty
                fullName.isBlank() -> toast("Name field cannot be empty.")
                
                // 3. Name Alphabetic Check
                !fullName.matches(Regex("^[a-zA-Z\\s]+$")) -> toast("Name should only contain alphabetic characters.")
                
                // 4. Name Length Limit
                fullName.length > 500 -> toast("Name exceeds character limit.")

                // 5. Email Empty
                email.isBlank() -> toast("Email address is required.")

                // 6. Invalid Email
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> toast("Please enter a valid email address.")

                // 7. Password Empty
                password.isBlank() -> toast("Password field is required.")

                // 8. Password Short (< 8 chars)
                password.length < 8 -> toast("Password must be at least 8 characters.")

                // 9. Password Complexity (Mix of letters, numbers, symbols)
                // Regex: min 1 letter, 1 number, 1 special char is usually implied by "mix", 
                // but user said "mix of letters, numbers, and symbols".
                // Let's interpret strictly: needs Letter AND Number AND (Symbol OR just mix?).
                // Common strict regex: ^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,}$
                // But specifically for "password123", it has letters and numbers but maybe no symbols?
                // Request: "Password not strong enough. Use a mix of letters, numbers, and symbols."
                // So "password123" fails -> means we NEED symbols.
                !password.matches(Regex("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&.,:;^\\-_+=\\[\\]{}()|/<>~]).+$")) -> 
                    toast("Password not strong enough. Use a mix of letters, numbers, and symbols.")

                // 10. Terms of Service
                !isTermsChecked -> toast("You must agree to the terms of service to continue.")

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
                                    role = 0 // passenger default
                                )
                            }

                            if (result.needsEmailConfirmation) {
                              // Success case -> Redirect to Verify Email (or Dashboard logic but here we show Verify screen)
                              // User request says "Redirect to 'Verify Email' or Dashboard."
                              // Current logic goes into SignupSuccessActivity which acts as Verify Email prompt.
                                toast("Sign-up successful. Please check your email.")
                            } else {
                                // toast("Sign-up successful ✅") -- Removed
                            }

                            startActivity(Intent(this@RegisterActivity, SignupSuccessActivity::class.java).putExtra("email", email))
                            finish()
                        } catch (e: Exception) {
                            val msg = friendlyAuthMessage(e)
                            toast(msg)
                        } finally {
                            btnSignUp.isEnabled = true
                        }
                    }
                }
            }
        }

        // Đăng ký bằng Google
        btnGoogleSignUp.setOnClickListener {
            signUpWithGoogle()
        }

        txtGoLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }


    }

    /**
     * Đăng ký/Đăng nhập bằng Google sử dụng CredentialManager API.
     * Flow giống như đăng nhập - nếu user chưa có sẽ tự động tạo mới.
     */
    private fun signUpWithGoogle() {
        lifecycleScope.launch {
            try {
                val credentialManager = CredentialManager.create(this@RegisterActivity)

                // Cấu hình GetGoogleIdOption
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                    .setFilterByAuthorizedAccounts(false)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                // Lấy credential từ Google
                val result = credentialManager.getCredential(
                    request = request,
                    context = this@RegisterActivity
                )

                // Parse Google ID Token
                val credential = result.credential
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                Log.d("RegisterActivity", "Got Google ID Token successfully")

                // Gửi ID Token đến Supabase để xác thực
                withContext(Dispatchers.IO) {
                    SupabaseProvider.client.auth.signInWith(IDToken) {
                        this.provider = Google
                        this.idToken = idToken
                    }

                    // Xử lý tạo user nếu là lần đầu đăng ký
                    authService.handleOAuthSuccess()
                }

                // toast("Đăng ký Google thành công!") -- Removed
                navigateToHome()

            } catch (e: GetCredentialCancellationException) {
                Log.d("RegisterActivity", "Google Sign-Up cancelled by user")
                toast("Đăng ký bị huỷ")
            } catch (e: NoCredentialException) {
                Log.e("RegisterActivity", "No credential available", e)
                toast("Không tìm thấy tài khoản Google. Vui lòng thêm tài khoản Google vào thiết bị.")
            } catch (e: GetCredentialException) {
                Log.e("RegisterActivity", "GetCredentialException", e)
                toast("Lỗi đăng ký Google: ${e.message}")
            } catch (e: Exception) {
                Log.e("RegisterActivity", "Google Sign-Up error", e)
                toast("Lỗi đăng ký Google: ${e.message}")
            }
        }
    }

    private fun navigateToHome() {
        val intent = Intent(this@RegisterActivity, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    private fun friendlyAuthMessage(e: Throwable): String {
        val msg = (e.message ?: e.toString())
        return when {
            msg.contains("email_address_invalid", ignoreCase = true) ->
                "Invalid email address."
            msg.contains("email_not_confirmed", ignoreCase = true) ->
                "Email not verified. Please check your inbox and click the verification link, then sign in."
            msg.contains("user_already_exists", ignoreCase = true) ||
                    msg.contains("already registered", ignoreCase = true) ->
                "This email is already in use."
            msg.contains("weak_password", ignoreCase = true) ->
                "Password not strong enough. Use a mix of letters, numbers, and symbols."
            else -> msg
        }
    }
}

