package com.example.project_bus

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.project_bus.data.SupabaseProvider
import com.example.project_bus.ui.auth.LoginActivity
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks

/**
 * Receives Supabase Auth deeplinks (signup confirmation, OTP, OAuth).
 *
 * Deeplink pattern in Supabase Dashboard must match: scheme://host
 * Example used here: com.example.project_bus://login
 */
class AuthCallbackActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleAuthIntent(intent)
    }

    // Fix for the compile error:
    // - override signature must be (intent: Intent) not (intent: Intent?)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthIntent(intent)
    }

    private fun handleAuthIntent(intent: Intent) {
        try {
            // Parse the session/token from the deeplink and import it into the Auth plugin
            SupabaseProvider.client.handleDeeplinks(intent)
        } catch (e: Exception) {
            // If it's not a Supabase deeplink or parsing fails, we still continue to Login screen
        }

        val isSignedIn = SupabaseProvider.client.auth.currentUserOrNull() != null

        val next = if (isSignedIn) {
            Toast.makeText(this, "Email verified. You're signed in.", Toast.LENGTH_SHORT).show()
            Intent(this, HomeActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        } else {
            Toast.makeText(this, "Email verified. You can sign in now.", Toast.LENGTH_SHORT).show()
            Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        startActivity(next)
        finish()
    }
}
