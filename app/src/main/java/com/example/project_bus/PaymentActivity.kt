package com.example.project_bus

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.project_bus.data.SupabaseProvider
import com.example.project_bus.data.models.BookingCreate
import com.example.project_bus.data.models.BookingPassengerCreate
import com.example.project_bus.data.models.PaymentCreate
import com.example.project_bus.data.services.BookingPassengersService
import com.example.project_bus.data.services.BookingsService
import com.example.project_bus.data.services.PaymentsService
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * NOTE: This screen is a UI demo for card input.
 * In production, do NOT handle raw card details yourself.
 * Use a PCI-compliant gateway (e.g., Stripe PaymentSheet / Google Pay).
 */
class PaymentActivity : AppCompatActivity() {

    private val bookingsService = BookingsService()
    private val bookingPassengersService = BookingPassengersService()
    private val paymentsService = PaymentsService()

    // Simple statuses for demo/testing
    private val BOOKING_STATUS_CONFIRMED = 1
    private val PAYMENT_STATUS_SUCCESS = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        // Header card info
        val tvBusName = findViewById<TextView>(R.id.tvBusName)
        val tvBusType = findViewById<TextView>(R.id.tvBusType)
        val tvBusTime = findViewById<TextView>(R.id.tvBusTime)

        val operator = intent.getStringExtra("OPERATOR") ?: "Bus"
        val busType = intent.getStringExtra("BUS_TYPE") ?: "Standard"
        val date = intent.getStringExtra("DATE") ?: ""
        tvBusName.text = operator
        tvBusType.text = busType
        tvBusTime.text = date

        // activity_payment.xml uses id="etCardName"
        val etCardName = findViewById<EditText>(R.id.etCardName)
        val etCardNumber = findViewById<EditText>(R.id.etCardNumber)
        val etCvv = findViewById<EditText>(R.id.etCvv)

        val btnPayNow = findViewById<View>(R.id.btnPayNow)

        btnPayNow.setOnClickListener {
            if (!validateCardInputs(etCardName, etCardNumber, etCvv)) return@setOnClickListener

            // Create booking + passengers + payment on Supabase
            btnPayNow.isEnabled = false
            lifecycleScope.launch {
                try {
                    val bookingId = withContext(Dispatchers.IO) { createBookingAndPayment() }
                    Toast.makeText(this@PaymentActivity, "Payment successful ✅", Toast.LENGTH_SHORT).show()

                    val i = Intent(this@PaymentActivity, PaymentSuccessActivity::class.java)
                    i.putExtra("BOOKING_ID", bookingId)
                    startActivity(i)
                    finish()
                } catch (e: Exception) {
                    btnPayNow.isEnabled = true
                    Toast.makeText(this@PaymentActivity, "Payment failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun validateCardInputs(
        etCardName: EditText,
        etCardNumber: EditText,
        etCvv: EditText
    ): Boolean {
        val name = etCardName.text.toString().trim()
        val number = etCardNumber.text.toString().replace(" ", "").trim()
        val cvv = etCvv.text.toString().trim()

        if (name.isEmpty()) {
            etCardName.error = "Required"
            return false
        }
        if (number.length < 12) {
            etCardNumber.error = "Invalid card number"
            return false
        }
        if (cvv.length < 3) {
            etCvv.error = "Invalid CVC"
            return false
        }
        return true
    }

    private suspend fun createBookingAndPayment(): Long {
        val user = SupabaseProvider.client.auth.currentUserOrNull()
            ?: throw IllegalStateException("Not signed in")
        val userId = user.id

        val tripId = intent.getLongExtra("TRIP_ID", 0L)
        if (tripId <= 0L) throw IllegalArgumentException("Missing trip")

        val selectedSeats = intent.getStringArrayListExtra("SELECTED_SEATS") ?: arrayListOf()
        if (selectedSeats.isEmpty()) throw IllegalArgumentException("No seat selected")

        val totalAmount = (intent.extras?.get("TOTAL_PRICE") as? Number)?.toInt() ?: 0
        if (totalAmount <= 0) throw IllegalArgumentException("Invalid total")

        val pickupStopId = intent.getIntExtra("PICKUP_STOP_ID", -1).takeIf { it > 0 }
        val dropoffStopId = intent.getIntExtra("DROPOFF_STOP_ID", -1).takeIf { it > 0 }

        val contactMobile = intent.getStringExtra("CONTACT_MOBILE")?.trim().orEmpty()
        val contactEmail = intent.getStringExtra("CONTACT_EMAIL")?.trim().orEmpty()
        val contactName = intent.getStringExtra("CONTACT_NAME")?.trim()
            ?: (intent.getStringArrayListExtra("PASSENGER_NAMES")?.firstOrNull()?.trim() ?: "Guest")

        val passengerNames = intent.getStringArrayListExtra("PASSENGER_NAMES")
            ?: arrayListOf()

        // 1) Create booking
        val booking = bookingsService.createBooking(
            BookingCreate(
                tripId = tripId,
                userId = userId,
                pickupStopId = pickupStopId,
                dropoffStopId = dropoffStopId,
                contactName = contactName,
                contactMobile = contactMobile,
                contactEmail = contactEmail,
                totalAmount = totalAmount.toDouble(),
                bookingStatus = BOOKING_STATUS_CONFIRMED
            )
        )

        val bookingId = booking.id

        // 2) Create booking passengers (seat -> name)
        // If passengerNames is missing, fall back to "Passenger #n"
        selectedSeats.forEachIndexed { idx, seatNo ->
            val name = passengerNames.getOrNull(idx)?.takeIf { it.isNotBlank() }
                ?: "Passenger ${idx + 1}"
            bookingPassengersService.createPassenger(
                BookingPassengerCreate(
                    bookingId = bookingId,
                    seatNumber = seatNo,
                    fullName = name
                )
            )
        }

        // 3) Create payment record (demo)
        val txRef = "TEST-${UUID.randomUUID()}"
        paymentsService.createPayment(
            PaymentCreate(
                bookingId = bookingId,
                transactionRef = txRef,
                amount = totalAmount,
                paymentMethod = "card",
                paymentStatus = PAYMENT_STATUS_SUCCESS
            )
        )

        return bookingId
    }
}
