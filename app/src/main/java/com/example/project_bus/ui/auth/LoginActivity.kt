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

class LoginActivity : AppCompatActivity() {

    private val authService = AuthService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        val edtEmail = findViewById<EditText>(R.id.edtEmail)
        val edtPassword = findViewById<EditText>(R.id.edtPassword)
        val btnSignIn = findViewById<Button>(R.id.btnSignIn)
        val txtGoRegister = findViewById<TextView>(R.id.txtGoRegister)
        val btnGoogleSignIn = findViewById<View>(R.id.btnGoogleSignIn)

        // Đăng nhập bằng Email/Password
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

                            toast("Đăng nhập thành công!")
                            navigateToHome()

                        } catch (e: Exception) {
                            toast("Lỗi: ${friendlyAuthMessage(e)}")
                            btnSignIn.isEnabled = true
                        }
                    }
                }
            }
        }

        // Đăng nhập bằng Google
        btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }

        // Chuyển sang trang đăng ký
        txtGoRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * Đăng nhập bằng Google sử dụng CredentialManager API.
     * Sau khi lấy được ID Token từ Google, gửi đến Supabase để xác thực.
     */
    private fun signInWithGoogle() {
        lifecycleScope.launch {
            try {
                val credentialManager = CredentialManager.create(this@LoginActivity)

                // Cấu hình GetGoogleIdOption
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                    .setFilterByAuthorizedAccounts(false) // Cho phép chọn bất kỳ tài khoản nào
                    .setAutoSelectEnabled(false) // Không tự động chọn
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                // Lấy credential từ Google
                val result = credentialManager.getCredential(
                    request = request,
                    context = this@LoginActivity
                )

                // Parse Google ID Token
                val credential = result.credential
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                Log.d("LoginActivity", "Got Google ID Token successfully")

                // Gửi ID Token đến Supabase để xác thực
                withContext(Dispatchers.IO) {
                    SupabaseProvider.client.auth.signInWith(IDToken) {
                        this.provider = Google
                        this.idToken = idToken
                    }

                    // Xử lý tạo user nếu là lần đầu đăng nhập
                    authService.handleOAuthSuccess()
                }

                toast("Đăng nhập Google thành công!")
                navigateToHome()

            } catch (e: GetCredentialCancellationException) {
                Log.d("LoginActivity", "Google Sign-In cancelled by user")
                toast("Đăng nhập bị huỷ")
            } catch (e: NoCredentialException) {
                Log.e("LoginActivity", "No credential available", e)
                toast("Không tìm thấy tài khoản Google. Vui lòng thêm tài khoản Google vào thiết bị.")
            } catch (e: GetCredentialException) {
                Log.e("LoginActivity", "GetCredentialException", e)
                toast("Lỗi đăng nhập Google: ${e.message}")
            } catch (e: Exception) {
                Log.e("LoginActivity", "Google Sign-In error", e)
                toast("Lỗi đăng nhập Google: ${e.message}")
            }
        }
    }

    private fun navigateToHome() {
        val intent = Intent(this@LoginActivity, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
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
