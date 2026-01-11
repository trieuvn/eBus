package com.example.project_bus.ui.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.project_bus.HomeActivity
import com.example.project_bus.R
import com.example.project_bus.data.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Receives Supabase Auth deeplink callbacks.
 *
 * When the user clicks the email confirmation link, Supabase verifies the email
 * and redirects to our deeplink (scheme://host). We then let supabase-kt parse
 * the URL fragment/query and import the session.
 */
class AuthCallbackActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth_callback)

        lifecycleScope.launch {
            val data: Uri? = intent?.data

            // Let supabase-kt handle parsing + session importing from deeplinks.
            try {
                withContext(Dispatchers.IO) {
                    SupabaseProvider.client.handleDeeplinks(intent)
                }
            } catch (e: Exception) {
                toast("Could not process the verification link: ${e.message ?: e}")
            }

            // If a session was imported, go straight to Home.
            val user = SupabaseProvider.client.auth.currentUserOrNull()
            if (user != null) {
                toast("Email verified. You're signed in ✅")
                startActivity(Intent(this@AuthCallbackActivity, HomeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
                return@launch
            }

            // No session found (some setups don't include a session in the redirect).
            // Still successful verification in most cases, so guide the user to sign in.
            if (data != null) {
                toast("Email verified. Please sign in to continue.")
            } else {
                toast("Please sign in to continue.")
            }
            startActivity(Intent(this@AuthCallbackActivity, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}

