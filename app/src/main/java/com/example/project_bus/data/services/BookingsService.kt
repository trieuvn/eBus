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

    // --- HÀM MỚI BẮT BUỘC PHẢI CÓ ---
    suspend fun getBookingsByUserId(userId: String): List<Booking> =
        client.from(Tables.BOOKINGS)
            .select {
                filter {
                    // Lọc theo user_id chính xác
                    eq("user_id", userId)
                }
                // Sắp xếp vé mới nhất lên đầu
                order("created_at", order = Order.DESCENDING)
            }
            .decodeList<Booking>()
    // --------------------------------

    suspend fun getAllBookings(): List<Booking> =
        client.from(Tables.BOOKINGS).select().decodeList<Booking>()

    suspend fun getBookingById(id: Long): Booking? =
        client.from(Tables.BOOKINGS).select { filter { eq("id", id) } }.decodeList<Booking>().firstOrNull()

    suspend fun createBooking(booking: BookingCreate): Booking =
        client.from(Tables.BOOKINGS).insert(booking) { select() }.decodeSingle<Booking>()

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
        }.decodeSingle<Booking>()

    suspend fun deleteById(id: Long): Booking =
        client.from(Tables.BOOKINGS).delete {
            select()
            filter { eq("id", id) }
        }.decodeSingle<Booking>()
}
