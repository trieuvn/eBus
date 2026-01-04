package com.example.project_bus.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Trip(
    @SerialName("id")
    val id: Long,

    @SerialName("route_id")
    val routeId: Int,

    @SerialName("departure_time")
    val departureTime: String? = null,

    @SerialName("arrival_time")
    val arrivalTime: String? = null,

    @SerialName("operator_name")
    val operatorName: String? = null,

    @SerialName("bus_type")
    val busType: String? = null,

    @SerialName("base_price")
    val basePrice: Int? = null,

    @SerialName("status")
    val status: Int? = null
) {
    // --- QUAN TRỌNG: Thêm đoạn này để lấy giá tiền ---
    val price: Double
        get() = basePrice?.toDouble() ?: 0.0
}

// ... Giữ nguyên các class TripCreate, TripUpdate bên dưới ...
@Serializable
data class TripCreate(
    @SerialName("route_id") val routeId: Int,
    @SerialName("departure_time") val departureTime: String? = null,
    @SerialName("arrival_time") val arrivalTime: String? = null,
    @SerialName("operator_name") val operatorName: String? = null,
    @SerialName("bus_type") val busType: String? = null,
    @SerialName("base_price") val basePrice: Int? = null,
    @SerialName("status") val status: Int? = null
)

@Serializable
data class TripUpdate(
    @SerialName("route_id") val routeId: Int? = null,
    @SerialName("departure_time") val departureTime: String? = null,
    @SerialName("arrival_time") val arrivalTime: String? = null,
    @SerialName("operator_name") val operatorName: String? = null,
    @SerialName("bus_type") val busType: String? = null,
    @SerialName("base_price") val basePrice: Int? = null,
    @SerialName("status") val status: Int? = null
)

