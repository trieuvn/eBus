package com.example.project_bus.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Trip(
    val id: Long,
    @SerialName("route_id") val routeId: Int,
    @SerialName("operator_name") val operatorName: String? = null,
    @SerialName("departure_time") val departureTime: String? = null,
    @SerialName("arrival_time") val arrivalTime: String? = null,
    @SerialName("price") val price: Double = 0.0, 
    @SerialName("bus_type") val busType: String? = "Standard",
    @SerialName("total_seats") val totalSeats: Int = 40,
    val status: Int = 1
)

@Serializable
data class TripCreate(
    @SerialName("route_id") val routeId: Int,
    @SerialName("operator_name") val operatorName: String? = null,
    @SerialName("departure_time") val departureTime: String? = null,
    @SerialName("arrival_time") val arrivalTime: String? = null,
    @SerialName("price") val price: Double? = null,
    @SerialName("bus_type") val busType: String? = "Standard",
    @SerialName("total_seats") val totalSeats: Int = 40,
    val status: Int = 1
)

@Serializable
data class TripUpdate(
    @SerialName("route_id") val routeId: Int? = null,
    @SerialName("operator_name") val operatorName: String? = null,
    @SerialName("departure_time") val departureTime: String? = null,
    @SerialName("arrival_time") val arrivalTime: String? = null,
    @SerialName("price") val price: Double? = null,
    @SerialName("bus_type") val busType: String? = null,
    val status: Int? = null
)
