
package com.example.project_bus.data.api

import com.example.project_bus.BuildConfig
import com.example.project_bus.data.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.post
import io.ktor.client.request.headers
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object PaymentApi {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val http by lazy {
        HttpClient(Android)
    }

    suspend fun processPayment(request: ProcessPaymentRequestDto): ProcessPaymentResponseDto {
        val session = SupabaseProvider.client.auth.currentSessionOrNull()
            ?: throw IllegalStateException("Missing session")

        val url = BuildConfig.SUPABASE_URL.trimEnd('/') + "/functions/v1/smart-handler"

        val raw = http.post(url) {
            headers {
                append("apikey", BuildConfig.SUPABASE_ANON_KEY)
                append("Authorization", "Bearer ${session.accessToken}")
                append("Content-Type", "application/json")
            }
            setBody(
                TextContent(
                    json.encodeToString(request),
                    ContentType.Application.Json
                )
            )
        }.bodyAsText()

        return json.decodeFromString(
            ProcessPaymentResponseDto.serializer(),
            raw
        )
    }
}
