package com.example.project_bus.data

import kotlinx.serialization.Serializable

@Serializable
data class Instrument(
    val id: Long,
    val name: String
)

@Serializable
data class InstrumentInsert(
    val name: String
)
