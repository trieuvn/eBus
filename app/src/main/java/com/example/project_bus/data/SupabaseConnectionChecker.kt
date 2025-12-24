package com.example.project_bus.data

import io.github.jan.supabase.postgrest.from
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Ping Supabase bằng cách SELECT bảng "instruments".
 */
object SupabaseConnectionChecker {

    suspend fun ping(): Result<Int> = runCatching {
        val rows = SupabaseProvider.client
            .from("instruments")
            .select()
            .decodeList<Instrument>()
        rows.size
    }

    fun humanMessage(t: Throwable): String = when (t) {
        is UnknownHostException ->
            "Không resolve được host. Kiểm tra SUPABASE_URL hoặc mạng."

        is SocketTimeoutException ->
            "Timeout. Kiểm tra mạng hoặc Supabase URL."

        is ClientRequestException -> {
            val code = t.response.status.value
            when (code) {
                401 -> "401 Unauthorized: anon key sai / thiếu header."
                403 -> "403 Forbidden: bị RLS policy chặn."
                404 -> "404 Not Found: sai tên bảng/endpoint (vd: instruments)."
                else -> "HTTP $code: ${t.response.status.description}"
            }
        }

        is ServerResponseException ->
            "Server lỗi ${t.response.status.value}: ${t.response.status.description}"

        else -> t.message ?: t::class.simpleName.orEmpty()
    }
}
