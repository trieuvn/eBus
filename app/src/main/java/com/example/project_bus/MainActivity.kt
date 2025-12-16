package com.example.project_bus

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.project_bus.data.Instrument
import com.example.project_bus.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Test query
        lifecycleScope.launch {
            try {
                val rows = withContext(Dispatchers.IO) {
                    SupabaseProvider.client
                        .from("instruments")
                        .select()
                        .decodeList<Instrument>()
                }
                Log.d("SUPABASE_TEST", "OK rows=${rows.size}")
            } catch (e: Exception) {
                Log.e("SUPABASE_TEST", "FAIL ${e.message}", e)
            }
        }
    }
}
