package com.example.project_bus.data.services

import com.example.project_bus.data.SupabaseProvider
import com.example.project_bus.data.Tables
import com.example.project_bus.data.models.Booking
import com.example.project_bus.data.models.BookingCreate
import com.example.project_bus.data.models.BookingUpdate
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

class BookingsService {

    private val client = SupabaseProvider.client

    suspend fun getBookingsByUserId(userId: String): List<Booking> =
        client.from(Tables.BOOKINGS)
            .select {
                filter { eq("user_id", userId) }
                order("created_at", order = Order.DESCENDING)
            }
            .decodeList()

    suspend fun getBookingsByTripId(tripId: Long): List<Booking> =
        client.from(Tables.BOOKINGS)
            .select {
                filter {
                    eq("trip_id", tripId)
                }
            }
            .decodeList()

    suspend fun getBookingById(id: Long): Booking? =
        client.from(Tables.BOOKINGS)
            .select { filter { eq("id", id) } }
            .decodeList<Booking>()
            .firstOrNull()

    // Hàm quan trọng để tạo Booking và trả về ID
    suspend fun createBooking(booking: BookingCreate): Booking =
        client.from(Tables.BOOKINGS)
            .insert(booking) { select() } // select() là bắt buộc để trả về dữ liệu
            .decodeSingle()

    suspend fun updateBooking(id: Long, update: BookingUpdate): Booking =
        client.from(Tables.BOOKINGS).update({
            update.pickupStopId?.let { set("pickup_stop_id", it) }
            update.dropoffStopId?.let { set("dropoff_stop_id", it) }
            update.contactName?.let { set("contact_name", it) }
            update.contactMobile?.let { set("contact_mobile", it) }
            update.contactEmail?.let { set("contact_email", it) }
            update.totalAmount?.let { set("total_amount", it) }
            update.bookingStatus?.let { set("booking_status", it) }
        }) {
            select()
            filter { eq("id", id) }
        }.decodeSingle()

    suspend fun deleteById(id: Long): Booking =
        client.from(Tables.BOOKINGS)
            .delete { select(); filter { eq("id", id) } }
            .decodeSingle()
}