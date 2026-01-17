package com.example.project_bus.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
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

                toast("Đăng ký Google thành công!")
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

