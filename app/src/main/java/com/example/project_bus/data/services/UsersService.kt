package com.example.project_bus.data.services

import com.example.project_bus.data.SupabaseProvider
import com.example.project_bus.data.Tables
import com.example.project_bus.data.models.AppUser
import com.example.project_bus.data.models.AppUserCreate
import com.example.project_bus.data.models.AppUserUpdate
import io.github.jan.supabase.postgrest.from

class UsersService {

    private val client = SupabaseProvider.client

    suspend fun getAllUsers(): List<AppUser> =
        client.from(Tables.USERS)
            .select()
            .decodeList<AppUser>()

    suspend fun getUserById(id: String): AppUser? =
        client.from(Tables.USERS)
            .select { filter { eq("id", id) } }
            .decodeList<AppUser>()
            .firstOrNull()

    suspend fun createUser(user: AppUserCreate): AppUser =
        client.from(Tables.USERS)
            .insert(user) { select() }
            .decodeSingle<AppUser>()

    suspend fun updateUser(id: String, update: AppUserUpdate): AppUser =
        client.from(Tables.USERS)
            .update({
                update.email?.let { set("email", it) }
                update.fullName?.let { set("full_name", it) }
                update.phoneNumber?.let { set("phone_number", it) }
                update.role?.let { set("role", it) }
            }) {
                select()
                filter { eq("id", id) }
            }
            .decodeSingle<AppUser>()

    suspend fun deleteById(id: String): AppUser =
        client.from(Tables.USERS)
            .delete {
                select()
                filter { eq("id", id) }
            }
            .decodeSingle<AppUser>()
}
