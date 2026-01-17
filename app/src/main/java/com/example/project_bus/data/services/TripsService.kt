package com.example.project_bus.data.services

import com.example.project_bus.data.SupabaseProvider
import com.example.project_bus.data.Tables
import com.example.project_bus.data.models.Trip
import com.example.project_bus.data.models.TripCreate
import com.example.project_bus.data.models.TripUpdate
import io.github.jan.supabase.postgrest.from

class TripsService {

    private val client = SupabaseProvider.client

    suspend fun getAllTrips(): List<Trip> =
        client.from(Tables.TRIPS)
            .select()
            .decodeList<Trip>()

    suspend fun getTripById(id: Long): Trip? =
        client.from(Tables.TRIPS)
            .select { filter { eq("id", id) } }
            .decodeList<Trip>()
            .firstOrNull()

    suspend fun createTrip(trip: TripCreate): Trip =
        client.from(Tables.TRIPS)
            .insert(trip) { select() }
            .decodeSingle<Trip>()

    suspend fun updateTrip(id: Long, update: TripUpdate): Trip =
        client.from(Tables.TRIPS)
            .update({
                update.routeId?.let { set("route_id", it) }
                update.departureTime?.let { set("departure_time", it) }
                update.arrivalTime?.let { set("arrival_time", it) }
                update.operatorName?.let { set("operator_name", it) }
                update.busType?.let { set("bus_type", it) }
                update.basePrice?.let { set("base_price", it) }
                update.status?.let { set("status", it) }
            }) {
                select()
                filter { eq("id", id) }
            }
            .decodeSingle<Trip>()

    suspend fun deleteById(id: Long): Trip =
        client.from(Tables.TRIPS)
            .delete {
                select()
                filter { eq("id", id) }
            }
            .decodeSingle<Trip>()
}




