package com.example.project_bus.data

import io.github.jan.supabase.postgrest.from
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Ping Supabase by selecting from the "instruments" table.
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
            "Unable to resolve host. Check your SUPABASE_URL or network connection."

        is SocketTimeoutException ->
            "Request timed out. Check your network connection or Supabase URL."

        is ClientRequestException -> {
            val code = t.response.status.value
            when (code) {
                401 -> "401 Unauthorized: invalid anon key or missing auth headers."
                403 -> "403 Forbidden: blocked by RLS policy."
                404 -> "404 Not Found: wrong table/endpoint name (e.g., instruments)."
                else -> "HTTP $code: ${t.response.status.description}"
            }
        }

        is ServerResponseException ->
            "Server error ${t.response.status.value}: ${t.response.status.description}"

        else -> t.message ?: t::class.simpleName.orEmpty()
    }
}


