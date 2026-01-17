package com.example.project_bus

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.project_bus.data.services.BookingPassengersService
import com.example.project_bus.data.services.BookingsService
import com.example.project_bus.data.services.RoutesService
import com.example.project_bus.data.services.TripsService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TicketDetailActivity : AppCompatActivity() {

    private val bookingsService = BookingsService()
    private val passengersService = BookingPassengersService()
    private val tripsService = TripsService()
    private val routesService = RoutesService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ticket_detail)

        val btnBack = findViewById<android.view.View>(R.id.btnDetailBack)
        btnBack.setOnClickListener { finish() }

        val bookingId = intent.getLongExtra("BOOKING_ID", -1L)
        if (bookingId <= 0L) {
            Toast.makeText(this, "Invalid Booking ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Bind Views
        val tvRouteFrom = findViewById<TextView>(R.id.tvDetailRouteFrom)
        val tvRouteTo = findViewById<TextView>(R.id.tvDetailRouteTo)
        val tvDate = findViewById<TextView>(R.id.tvDetailDate)
        val tvTime = findViewById<TextView>(R.id.tvDetailTime)
        val tvOperator = findViewById<TextView>(R.id.tvDetailOperator)
        val tvBusType = findViewById<TextView>(R.id.tvDetailBusType)
        val tvPassengers = findViewById<TextView>(R.id.tvDetailPassengerList)
        val tvPrice = findViewById<TextView>(R.id.tvDetailPrice)
        val tvStatus = findViewById<TextView>(R.id.tvDetailStatus)
        val tvContact = findViewById<TextView>(R.id.tvDetailContactInfo)

        lifecycleScope.launch {
            try {
                // 1. Lấy thông tin Booking
                val booking = withContext(Dispatchers.IO) { bookingsService.getBookingById(bookingId) }
                if (booking == null) {
                    Toast.makeText(this@TicketDetailActivity, "Booking not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }

                // 2. Lấy thông tin Trip -> Route
                val trip = withContext(Dispatchers.IO) { tripsService.getTripById(booking.tripId) }
                var routeFrom = "Start"
                var routeTo = "End"
                
                if (trip != null) {
                    val r = withContext(Dispatchers.IO) { routesService.getRouteById(trip.routeId) }
                    val rawName = r?.name ?: "Unknown Route"
                    if (rawName.contains("->")) {
                        val parts = rawName.split("->")
                        routeFrom = parts.getOrNull(0)?.trim() ?: "Start"
                        routeTo = parts.getOrNull(1)?.trim() ?: "End"
                    } else {
                        routeFrom = rawName
                        routeTo = ""
                    }
                }

                // 3. Lấy danh sách hành khách
                val passengers = withContext(Dispatchers.IO) { passengersService.getPassengersByBookingId(bookingId) }

                // 4. Update UI
                tvRouteFrom.text = routeFrom
                tvRouteTo.text = routeTo
                tvDate.text = booking.createdAt?.take(10) ?: "Date N/A"
                tvTime.text = trip?.departureTime?.take(5) ?: "Time N/A"
                
                tvOperator.text = trip?.operatorName ?: "Standard Bus"
                tvBusType.text = trip?.busType ?: "AC / Seater"
                
                val passengerText = StringBuilder()
                passengers.forEachIndexed { index, p ->
                    // Sử dụng \n để tạo ký tự xuống dòng trong chuỗi Kotlin
                    passengerText.append("${index + 1}. ${p.fullName ?: "Guest"} (Seat ${p.seatNumber ?: "-"})\n")
                }
                tvPassengers.text = passengerText.toString().trim()

                tvPrice.text = "LKR ${booking.totalAmount?.toInt() ?: 0}"
                
                val statusStr = if (booking.bookingStatus == 1) "CONFIRMED" else "PENDING"
                tvStatus.text = statusStr
                tvStatus.setTextColor(if (booking.bookingStatus == 1) 0xFF4CAF50.toInt() else 0xFFFF9800.toInt())

                tvContact.text = "Contact: ${booking.contactName ?: "N/A"} (${booking.contactMobile ?: "N/A"})"

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@TicketDetailActivity, "Error loading ticket: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
