package com.example.project_bus.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BookingPassenger(
    @SerialName("id")
    val id: Long,

    @SerialName("booking_id")
    val bookingId: Long,

    @SerialName("seat_number")
    val seatNumber: String? = null,

    @SerialName("full_name")
    val fullName: String? = null
)

@Serializable
data class BookingPassengerCreate(
    @SerialName("booking_id")
    val bookingId: Long,

    @SerialName("seat_number")
    val seatNumber: String? = null,

    @SerialName("full_name")
    val fullName: String? = null
)

@Serializable
data class BookingPassengerUpdate(
    @SerialName("seat_number")
    val seatNumber: String? = null,

    @SerialName("full_name")
    val fullName: String? = null
)


