package com.example.project_bus.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RouteStop(
    @SerialName("id")
    val id: Int,

    @SerialName("route_id")
    val routeId: Int,

    @SerialName("location_name")
    val locationName: String,

    @SerialName("stop_type")
    val stopType: Int? = null,

    @SerialName("stop_order")
    val stopOrder: Int? = null
)

@Serializable
data class RouteStopCreate(
    @SerialName("route_id")
    val routeId: Int,

    @SerialName("location_name")
    val locationName: String,

    @SerialName("stop_type")
    val stopType: Int? = null,

    @SerialName("stop_order")
    val stopOrder: Int? = null
)

@Serializable
data class RouteStopUpdate(
    @SerialName("route_id")
    val routeId: Int? = null,

    @SerialName("location_name")
    val locationName: String? = null,

    @SerialName("stop_type")
    val stopType: Int? = null,

    @SerialName("stop_order")
    val stopOrder: Int? = null
)
