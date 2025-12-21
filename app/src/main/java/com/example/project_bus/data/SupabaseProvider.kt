package com.example.project_bus.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseProvider {

    // Dán "Project URL" trong Supabase (Connect dialog / Settings -> API)
    // Ví dụ đúng format: https://<project_ref>.supabase.co
    private const val SUPABASE_URL = "https://arhfvsserkteuunptzpo.supabase.co"

    // Dán "anon/public key" (hoặc publishable key mới) từ Supabase
    private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFyaGZ2c3Nlcmt0ZXV1bnB0enBvIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjU3ODEzMDAsImV4cCI6MjA4MTM1NzMwMH0.gIY84-nuxbAjSP_lpfQDBi6MsG6N-vXbUKjWM7OIsQs"

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_KEY
        ) {
            install(Postgrest)
        }
    }
}
