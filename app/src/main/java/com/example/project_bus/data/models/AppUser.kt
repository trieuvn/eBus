package com.example.project_bus.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppUser(
    @SerialName("id")
    val id: String,

    @SerialName("email")
    val email: String,

    @SerialName("password")
    val password: String? = null,

    @SerialName("full_name")
    val fullName: String? = null,

    @SerialName("phone_number")
    val phoneNumber: String? = null,

    @SerialName("role")
    val role: Int? = null,

    @SerialName("authid")
    val authId: String? = null
)

@Serializable
data class AppUserCreate(
    @SerialName("id")
    val id: String,

    @SerialName("email")
    val email: String,

    @SerialName("full_name")
    val fullName: String? = null,

    @SerialName("phone_number")
    val phoneNumber: String? = null,

    @SerialName("role")
    val role: Int? = 0,

    @SerialName("authid")
    val authId: String? = null
)

@Serializable
data class AppUserUpdate(
    @SerialName("email")
    val email: String? = null,

    @SerialName("full_name")
    val fullName: String? = null,

    @SerialName("phone_number")
    val phoneNumber: String? = null,

    @SerialName("role")
    val role: Int? = null
)
