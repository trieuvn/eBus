package com.example.project_bus.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Trip(
    val id: Int,
    @SerialName("route_id") val routeId: Int,
    @SerialName("operator_name") val operatorName: String? = null,
    @SerialName("start_time") val startTime: String? = null,
    @SerialName("end_time") val endTime: String? = null,
    val price: Double = 0.0,
    @SerialName("bus_type") val busType: String? = "Standard",
    @SerialName("total_seats") val totalSeats: Int = 40,
    val status: Int = 1
)
