package com.example.project_bus.data.services

import com.example.project_bus.data.SupabaseProvider
import com.example.project_bus.data.Tables
import com.example.project_bus.data.models.RouteStop
import com.example.project_bus.data.models.RouteStopCreate
import com.example.project_bus.data.models.RouteStopUpdate
import io.github.jan.supabase.postgrest.from

class RoutesStopService {

    private val client = SupabaseProvider.client

    suspend fun getAllStops(): List<RouteStop> =
        client.from(Tables.ROUTES_STOP)
            .select()
            .decodeList<RouteStop>()

    suspend fun getStopById(id: Int): RouteStop? =
        client.from(Tables.ROUTES_STOP)
            .select { filter { eq("id", id) } }
            .decodeList<RouteStop>()
            .firstOrNull()

    suspend fun createStop(stop: RouteStopCreate): RouteStop =
        client.from(Tables.ROUTES_STOP)
            .insert(stop) { select() }
            .decodeSingle<RouteStop>()

    suspend fun updateStop(id: Int, update: RouteStopUpdate): RouteStop =
        client.from(Tables.ROUTES_STOP)
            .update({
                update.routeId?.let { set("route_id", it) }
                update.locationName?.let { set("location_name", it) }
                update.stopType?.let { set("stop_type", it) }
                update.stopOrder?.let { set("stop_order", it) }
            }) {
                select()
                filter { eq("id", id) }
            }
            .decodeSingle<RouteStop>()

    suspend fun deleteById(id: Int): RouteStop =
        client.from(Tables.ROUTES_STOP)
            .delete {
                select()
                filter { eq("id", id) }
            }
            .decodeSingle<RouteStop>()
}
