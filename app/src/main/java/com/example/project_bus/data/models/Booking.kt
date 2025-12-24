package com.example.project_bus.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Booking(
    @SerialName("id")
    val id: Long,

    @SerialName("user_id")
    val userId: String,

    @SerialName("trip_id")
    val tripId: Long,

    @SerialName("pickup_stop_id")
    val pickupStopId: Int? = null,

    @SerialName("dropoff_stop_id")
    val dropoffStopId: Int? = null,

    @SerialName("contact_name")
    val contactName: String? = null,

    @SerialName("contact_mobile")
    val contactMobile: String? = null,

    @SerialName("contact_email")
    val contactEmail: String? = null,

    @SerialName("total_amount")
    val totalAmount: Double? = null,

    @SerialName("booking_status")
    val bookingStatus: Int? = null,

    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
data class BookingCreate(
    @SerialName("user_id")
    val userId: String,

    @SerialName("trip_id")
    val tripId: Long,

    @SerialName("pickup_stop_id")
    val pickupStopId: Int? = null,

    @SerialName("dropoff_stop_id")
    val dropoffStopId: Int? = null,

    @SerialName("contact_name")
    val contactName: String? = null,

    @SerialName("contact_mobile")
    val contactMobile: String? = null,

    @SerialName("contact_email")
    val contactEmail: String? = null,

    @SerialName("total_amount")
    val totalAmount: Double? = null,

    @SerialName("booking_status")
    val bookingStatus: Int? = null
)

@Serializable
data class BookingUpdate(
    @SerialName("pickup_stop_id")
    val pickupStopId: Int? = null,

    @SerialName("dropoff_stop_id")
    val dropoffStopId: Int? = null,

    @SerialName("contact_name")
    val contactName: String? = null,

    @SerialName("contact_mobile")
    val contactMobile: String? = null,

    @SerialName("contact_email")
    val contactEmail: String? = null,

    @SerialName("total_amount")
    val totalAmount: Double? = null,

    @SerialName("booking_status")
    val bookingStatus: Int? = null
)
