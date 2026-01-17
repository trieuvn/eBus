package com.example.project_bus.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Trip(
    // DB dùng id kiểu bigint, và giá nằm ở cột base_price
    val id: Long,
    @SerialName("route_id") val routeId: Int,
    @SerialName("operator_name") val operatorName: String? = null,
    // DB đang lưu giờ chạy theo departure_time / arrival_time
    @SerialName("departure_time") val startTime: String? = null,
    @SerialName("arrival_time") val endTime: String? = null,
    @SerialName("base_price") val price: Double = 0.0,
    @SerialName("bus_type") val busType: String? = "Standard",
    @SerialName("total_seats") val totalSeats: Int = 40,
    val status: Int = 1
)


