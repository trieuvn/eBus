package com.example.project_bus

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.project_bus.data.SupabaseProvider
import com.example.project_bus.data.api.PaymentApi
import com.example.project_bus.data.api.PaymentDetailsDto
import com.example.project_bus.data.api.ProcessPaymentRequestDto
import com.example.project_bus.data.api.TransactionInfoDto
import com.example.project_bus.data.models.BookingCreate
import com.example.project_bus.data.models.BookingPassengerCreate
import com.example.project_bus.data.models.BookingUpdate
import com.example.project_bus.data.models.PaymentCreate
import com.example.project_bus.data.services.BookingPassengersService
import com.example.project_bus.data.services.BookingsService
import com.example.project_bus.data.services.PaymentsService
import com.example.project_bus.data.services.TripsService
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class PaymentActivity : AppCompatActivity() {

    private val bookingsService = BookingsService()
    private val bookingPassengersService = BookingPassengersService()
    private val paymentsService = PaymentsService()
    private val tripsService = TripsService()

    private lateinit var etCardName: EditText
    private lateinit var etCardNumber: EditText
    private lateinit var etCvv: EditText
    private lateinit var tvExpMonth: TextView
    private lateinit var tvExpYear: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        try {
            val operator = intent.getStringExtra("OPERATOR") ?: ""
            val busType = intent.getStringExtra("BUS_TYPE") ?: ""
            val date = intent.getStringExtra("DATE") ?: ""

            findViewById<TextView>(R.id.tvBusName).text = operator
            findViewById<TextView>(R.id.tvBusType).text = busType
            findViewById<TextView>(R.id.tvBusTime).text = date

            val user = SupabaseProvider.client.auth.currentUserOrNull()
            findViewById<TextView>(R.id.tvUserName).text = "Hello ${user?.email ?: "User"}!"

            etCardName = findViewById(R.id.etCardName)
            etCardNumber = findViewById(R.id.etCardNumber)
            etCvv = findViewById(R.id.etCvv)
            tvExpMonth = findViewById(R.id.tvExpMonth)
            tvExpYear = findViewById(R.id.tvExpYear)

            findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
            setupExpiryDatePickers()

            val btnPayNow = findViewById<View>(R.id.btnPayNow)
            btnPayNow.setOnClickListener { processPayment(btnPayNow) }
        } catch (e: Exception) {
            Log.e("PaymentInit", "Error in onCreate", e)
            Toast.makeText(this, "Init Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupExpiryDatePickers() {
        val c = Calendar.getInstance()
        val pickDate = {
            DatePickerDialog(this, { _, year, month, _ ->
                tvExpMonth.text = String.format("%02d", month + 1)
                tvExpYear.text = year.toString()
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), 1).show()
        }
        tvExpMonth.setOnClickListener { pickDate() }
        tvExpYear.setOnClickListener { pickDate() }
    }

    private fun validateInputs(): Boolean {
        if (etCardName.text.isBlank()) { etCardName.error = "Required"; return false }
        if (etCardNumber.text.length != 16) { etCardNumber.error = "16 digits required"; return false }
        if (etCvv.text.length != 3) { etCvv.error = "3 digits required"; return false }
        if (tvExpMonth.text == "Month" || tvExpYear.text == "Year") {
            Toast.makeText(this, "Select Expiry Date", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun processPayment(btn: View) {
        if (!validateInputs()) return

        btn.isEnabled = false
        Toast.makeText(this, "Processing Payment...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                val bookingId = withContext(Dispatchers.IO) {
                    val tripId = intent.getLongExtra("TRIP_ID", -1)
                    if (tripId == -1L) throw Exception("Invalid Trip ID")

                    val selectedSeats = intent.getStringArrayListExtra("SELECTED_SEATS") ?: arrayListOf()
                    if (selectedSeats.isEmpty()) throw Exception("No seats selected")

                    // Re-fetch trip to get accurate price
                    val trip = tripsService.getTripById(tripId)
                        ?: throw Exception("Trip not found")

                    val unitPrice = trip.price
                    if (unitPrice <= 0) throw Exception("Trip price invalid: $unitPrice")

                    val totalPrice = unitPrice * selectedSeats.size

                    // 1) Create booking + passengers with PENDING status
                    val createdBookingId = createBookingAndPassengersPending(totalPrice)

                    // 2) Call Payment API (Supabase Edge Function)
                    val resp = PaymentApi.processPayment(
                        ProcessPaymentRequestDto(
                            paymentDetails = PaymentDetailsDto(
                                cardHolderName = etCardName.text.toString().trim(),
                                cardNumber = etCardNumber.text.toString().trim(),
                                expirationMonth = tvExpMonth.text.toString().trim(),
                                expirationYear = tvExpYear.text.toString().trim(),
                                cvv = etCvv.text.toString().trim()
                            ),
                            transactionInfo = TransactionInfoDto(
                                amount = totalPrice,
                                currency = "USD"
                            ),
                            bookingId = createdBookingId
                        )
                    )

                    if (resp.status.lowercase() != "success" || resp.transactionId.isNullOrBlank()) {
                        bookingsService.updateBooking(createdBookingId, BookingUpdate(bookingStatus = 2)) // 2 = Failed
                        throw Exception(resp.message ?: "Payment failed")
                    }

                    // 3) Create Payment record + mark booking PAID
                    // CHANGED: Passed totalPrice directly as Double, removed .toInt()
                    paymentsService.createPayment(
                        PaymentCreate(
                            bookingId = createdBookingId,
                            transactionRef = resp.transactionId,
                            amount = totalPrice, 
                            paymentMethod = "Card",
                            paymentStatus = 1
                        )
                    )
                    bookingsService.updateBooking(createdBookingId, BookingUpdate(bookingStatus = 1)) // 1 = Confirmed

                    createdBookingId
                }

                Log.d("PaymentActivity", "Success! Booking ID: $bookingId")
                Toast.makeText(this@PaymentActivity, "Booking Successful!", Toast.LENGTH_SHORT).show()

                val i = Intent(this@PaymentActivity, PaymentSuccessActivity::class.java)
                i.putExtra("BOOKING_ID", bookingId)
                startActivity(i)
                finish()
            } catch (e: Exception) {
                btn.isEnabled = true
                Log.e("PaymentError", "Transaction Failed", e)
                Toast.makeText(this@PaymentActivity, "Failed: ${e.message ?: "Unknown error"}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun createBookingAndPassengersPending(totalPrice: Double): Long {
        val user = SupabaseProvider.client.auth.currentUserOrNull()
            ?: throw Exception("User not logged in. Please relogin.")

        val tripId = intent.getLongExtra("TRIP_ID", -1)
        if (tripId == -1L) throw Exception("Invalid Trip ID")

        val selectedSeats = intent.getStringArrayListExtra("SELECTED_SEATS") ?: arrayListOf()

        val passengerNames = intent.getStringArrayListExtra("PASSENGER_NAMES") ?: arrayListOf()
        var contactName = intent.getStringExtra("CONTACT_NAME")
        if (contactName.isNullOrBlank()) contactName = passengerNames.firstOrNull() ?: "Unknown Guest"

        val contactMobile = intent.getStringExtra("CONTACT_MOBILE")
        val contactEmail = intent.getStringExtra("CONTACT_EMAIL")

        // 1) Create Booking - PENDING (0)
        val booking = bookingsService.createBooking(
            BookingCreate(
                userId = user.id,
                tripId = tripId,
                pickupStopId = intent.getIntExtra("PICKUP_STOP_ID", -1).takeIf { it > 0 },
                dropoffStopId = intent.getIntExtra("DROPOFF_STOP_ID", -1).takeIf { it > 0 },
                contactName = contactName,
                contactMobile = contactMobile,
                contactEmail = contactEmail,
                totalAmount = totalPrice,
                bookingStatus = 0
            )
        )

        // 2) Create passengers
        selectedSeats.forEachIndexed { index, seat ->
            val pName = passengerNames.getOrNull(index) ?: "Guest"
            bookingPassengersService.createPassenger(
                BookingPassengerCreate(
                    bookingId = booking.id,
                    seatNumber = seat,
                    fullName = pName
                )
            )
        }

        return booking.id
    }
}
