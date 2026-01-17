package com.example.project_bus.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaymentDetailsDto(
    @SerialName("card_holder_name") val cardHolderName: String,
    @SerialName("card_number") val cardNumber: String,
    @SerialName("expiration_month") val expirationMonth: String,
    @SerialName("expiration_year") val expirationYear: String,
    @SerialName("cvv") val cvv: String
)

@Serializable
data class TransactionInfoDto(
    @SerialName("amount") val amount: Double,
    @SerialName("currency") val currency: String
)

@Serializable
data class ProcessPaymentRequestDto(
    @SerialName("payment_details") val paymentDetails: PaymentDetailsDto,
    @SerialName("transaction_info") val transactionInfo: TransactionInfoDto,
    // Optional: để Edge Function có thể ghi Payments + update Bookings
    @SerialName("booking_id") val bookingId: Long? = null
)

@Serializable
data class ProcessPaymentResponseDto(
    @SerialName("status") val status: String,
    @SerialName("transaction_id") val transactionId: String? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("timestamp") val timestamp: String? = null
)