package com.example.project_bus.data

import com.example.project_bus.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseProvider {

    /**
     * Ưu tiên lấy từ BuildConfig (đọc từ local.properties trong app/build.gradle.kts).
     * Nếu bạn chưa set local.properties thì sẽ fallback sang hằng số bên dưới.
     */
    private const val FALLBACK_SUPABASE_URL = "https://xsovgekjiuqrvvkgctck.supabase.co"
    private const val FALLBACK_SUPABASE_ANON_KEY =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inhzb3ZnZWtqaXVxcnZ2a2djdGNrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjU4NTAxMDEsImV4cCI6MjA4MTQyNjEwMX0.pQKUa7H7qZJWm4WHZDgpe8YriAAaffPfbPAAUqgTbTc"

    private val supabaseUrl: String =
        BuildConfig.SUPABASE_URL.takeIf { it.isNotBlank() } ?: FALLBACK_SUPABASE_URL

    private val supabaseAnonKey: String =
        BuildConfig.SUPABASE_ANON_KEY.takeIf { it.isNotBlank() } ?: FALLBACK_SUPABASE_ANON_KEY

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = supabaseUrl,
            supabaseKey = supabaseAnonKey
        ) {
            // Auth dùng cho đăng ký/đăng nhập + tự gắn JWT vào các request PostgREST
            install(Auth)

            // Database REST API
            install(Postgrest)
        }
    }
}
