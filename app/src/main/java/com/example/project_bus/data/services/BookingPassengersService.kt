package com.example.project_bus.data.services

import com.example.project_bus.data.SupabaseProvider
import com.example.project_bus.data.Tables
import com.example.project_bus.data.models.BookingPassenger
import com.example.project_bus.data.models.BookingPassengerCreate
import com.example.project_bus.data.models.BookingPassengerUpdate
import io.github.jan.supabase.postgrest.from

class BookingPassengersService {

    private val client = SupabaseProvider.client

    suspend fun getAllPassengers(): List<BookingPassenger> =
        client.from(Tables.BOOKING_PASSENGERS)
            .select()
            .decodeList<BookingPassenger>()

    suspend fun getPassengerById(id: Long): BookingPassenger? =
        client.from(Tables.BOOKING_PASSENGERS)
            .select { filter { eq("id", id) } }
            .decodeList<BookingPassenger>()
            .firstOrNull()

    // --- NEW: Lấy list hành khách theo danh sách ID booking (dùng cho SeatSelection) ---
    suspend fun getPassengersByBookingIds(bookingIds: List<Long>): List<BookingPassenger> =
        client.from(Tables.BOOKING_PASSENGERS)
            .select { filter { isIn("booking_id", bookingIds) } }
            .decodeList()

    suspend fun createPassenger(passenger: BookingPassengerCreate): BookingPassenger =
        client.from(Tables.BOOKING_PASSENGERS)
            .insert(passenger) { select() }
            .decodeSingle<BookingPassenger>()

    suspend fun updatePassenger(id: Long, update: BookingPassengerUpdate): BookingPassenger =
        client.from(Tables.BOOKING_PASSENGERS)
            .update({
                update.seatNumber?.let { set("seat_number", it) }
                update.fullName?.let { set("full_name", it) }
            }) {
                select()
                filter { eq("id", id) }
            }
            .decodeSingle<BookingPassenger>()

    suspend fun deleteById(id: Long): BookingPassenger =
        client.from(Tables.BOOKING_PASSENGERS)
            .delete { select(); filter { eq("id", id) } }
            .decodeSingle<BookingPassenger>()

    suspend fun getPassengersByBookingId(bookingId: Long): List<BookingPassenger> =
        client.from(Tables.BOOKING_PASSENGERS)
            .select { filter { eq("booking_id", bookingId) } }
            .decodeList<BookingPassenger>()
}
