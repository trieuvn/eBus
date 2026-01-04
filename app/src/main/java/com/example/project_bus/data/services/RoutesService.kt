package com.example.project_bus.data.services

import com.example.project_bus.data.SupabaseProvider
import com.example.project_bus.data.Tables
import com.example.project_bus.data.models.Route
import com.example.project_bus.data.models.RouteCreate
import com.example.project_bus.data.models.RouteUpdate
import io.github.jan.supabase.postgrest.from

class RoutesService {

    private val client = SupabaseProvider.client

    suspend fun getAllRoutes(): List<Route> =
        client.from(Tables.ROUTES)
            .select()
            .decodeList<Route>()

    suspend fun getRouteById(id: Int): Route? =
        client.from(Tables.ROUTES)
            .select { filter { eq("id", id) } }
            .decodeList<Route>()
            .firstOrNull()

    suspend fun createRoute(route: RouteCreate): Route =
        client.from(Tables.ROUTES)
            .insert(route) { select() }
            .decodeSingle<Route>()

    suspend fun updateRoute(id: Int, update: RouteUpdate): Route =
        client.from(Tables.ROUTES)
            .update({
                update.name?.let { set("name", it) }
                update.estDuration?.let { set("est_duration", it) }
            }) {
                select()
                filter { eq("id", id) }
            }
            .decodeSingle<Route>()

    suspend fun deleteById(id: Int): Route =
        client.from(Tables.ROUTES)
            .delete {
                select()
                filter { eq("id", id) }
            }
            .decodeSingle<Route>()
}


