package com.example.project_bus.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Route(
    @SerialName("id")
    val id: Int,

    @SerialName("name")
    val name: String,

    @SerialName("est_duration")
    val estDuration: Int? = null
)

@Serializable
data class RouteCreate(
    @SerialName("name")
    val name: String,

    @SerialName("est_duration")
    val estDuration: Int? = null
)

@Serializable
data class RouteUpdate(
    @SerialName("name")
    val name: String? = null,

    @SerialName("est_duration")
    val estDuration: Int? = null
)
