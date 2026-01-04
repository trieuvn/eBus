package com.example.project_bus.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Payment(
    @SerialName("id")
    val id: String,

    @SerialName("booking_id")
    val bookingId: Long,

    @SerialName("transaction_ref")
    val transactionRef: String? = null,

    @SerialName("amount")
    val amount: Int? = null,

    @SerialName("payment_method")
    val paymentMethod: String? = null,

    @SerialName("payment_status")
    val paymentStatus: Int? = null,

    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
data class PaymentCreate(
    @SerialName("booking_id")
    val bookingId: Long,

    @SerialName("transaction_ref")
    val transactionRef: String? = null,

    @SerialName("amount")
    val amount: Int? = null,

    @SerialName("payment_method")
    val paymentMethod: String? = null,

    @SerialName("payment_status")
    val paymentStatus: Int? = null
)

@Serializable
data class PaymentUpdate(
    @SerialName("transaction_ref")
    val transactionRef: String? = null,

    @SerialName("amount")
    val amount: Int? = null,

    @SerialName("payment_method")
    val paymentMethod: String? = null,

    @SerialName("payment_status")
    val paymentStatus: Int? = null
)


