package com.example.project_bus.data.services

import com.example.project_bus.data.SupabaseProvider
import com.example.project_bus.data.Tables
import com.example.project_bus.data.models.Payment
import com.example.project_bus.data.models.PaymentCreate
import com.example.project_bus.data.models.PaymentUpdate
import io.github.jan.supabase.postgrest.from

class PaymentsService {

    private val client = SupabaseProvider.client

    suspend fun getAllPayments(): List<Payment> =
        client.from(Tables.PAYMENTS)
            .select()
            .decodeList<Payment>()

    suspend fun getPaymentById(id: String): Payment? =
        client.from(Tables.PAYMENTS)
            .select { filter { eq("id", id) } }
            .decodeList<Payment>()
            .firstOrNull()

    suspend fun createPayment(payment: PaymentCreate): Payment =
        client.from(Tables.PAYMENTS)
            .insert(payment) { select() }
            .decodeSingle<Payment>()

    suspend fun updatePayment(id: String, update: PaymentUpdate): Payment =
        client.from(Tables.PAYMENTS)
            .update({
                update.transactionRef?.let { set("transaction_ref", it) }
                update.amount?.let { set("amount", it) }
                update.paymentMethod?.let { set("payment_method", it) }
                update.paymentStatus?.let { set("payment_status", it) }
            }) {
                select()
                filter { eq("id", id) }
            }
            .decodeSingle<Payment>()

    suspend fun deleteById(id: String): Payment =
        client.from(Tables.PAYMENTS)
            .delete {
                select()
                filter { eq("id", id) }
            }
            .decodeSingle<Payment>()
}
