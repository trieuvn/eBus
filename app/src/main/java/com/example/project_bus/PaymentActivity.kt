package com.example.project_bus

import android.app.DatePickerDialog
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
import java.util.Calendar
import java.util.UUID

class PaymentActivity : AppCompatActivity() {

    private val bookingsService = BookingsService()
    private val bookingPassengersService = BookingPassengersService()
    private val paymentsService = PaymentsService()

    private lateinit var etCardName: EditText
    private lateinit var etCardNumber: EditText
    private lateinit var etCvv: EditText
    private lateinit var tvExpMonth: TextView
    private lateinit var tvExpYear: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        try {
            // UI Init
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

            // Date Picker for Expiry
            setupExpiryDatePickers()

            val btnPayNow = findViewById<View>(R.id.btnPayNow)
            btnPayNow.setOnClickListener {
                processPayment(btnPayNow)
            }
        } catch (e: Exception) {
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
        val name = etCardName.text.toString().trim()
        val number = etCardNumber.text.toString().trim()
        val cvv = etCvv.text.toString().trim()
        val month = tvExpMonth.text.toString()
        val year = tvExpYear.text.toString()

        if (name.isEmpty()) {
            etCardName.error = "Please enter name"
            return false
        }
        // Name only letters and spaces
        if (!name.matches(Regex("^[a-zA-Z\\s]+$"))) {
            etCardName.error = "Name must only contain letters"
            return false
        }

        if (number.length != 16) {
            etCardNumber.error = "Card number must be 16 digits"
            return false
        }
        if (!number.matches(Regex("\\d+"))) {
            etCardNumber.error = "Digits only"
            return false
        }

        if (cvv.length != 3) {
            etCvv.error = "CVV must be 3 digits"
            return false
        }

        if (month == "Month" || year == "Year") {
            Toast.makeText(this, "Please select expiry date", Toast.LENGTH_SHORT).show()
            return false
        }
        
        return true
    }

    private fun processPayment(btn: View) {
        try {
            if (!validateInputs()) return

            btn.isEnabled = false
            lifecycleScope.launch {
                try {
                    val bookingId = withContext(Dispatchers.IO) { createBookingRecord() }
                    Toast.makeText(this@PaymentActivity, "Payment Successful!", Toast.LENGTH_SHORT).show()
                    
                    val i = Intent(this@PaymentActivity, PaymentSuccessActivity::class.java)
                    i.putExtra("BOOKING_ID", bookingId)
                    startActivity(i)
                    finish()
                } catch (e: Exception) {
                    btn.isEnabled = true
                    Toast.makeText(this@PaymentActivity, "Transaction Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            btn.isEnabled = true
            Toast.makeText(this, "Unexpected Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun createBookingRecord(): Long {
        val user = SupabaseProvider.client.auth.currentUserOrNull() ?: throw Exception("User not logged in")
        
        val tripId = intent.getLongExtra("TRIP_ID", -1)
        val selectedSeats = intent.getStringArrayListExtra("SELECTED_SEATS") ?: arrayListOf()
        val totalPrice = intent.getDoubleExtra("TOTAL_PRICE", 0.0)
        
        val pickupId = intent.getIntExtra("PICKUP_STOP_ID", -1).takeIf { it > 0 }
        val dropId = intent.getIntExtra("DROPOFF_STOP_ID", -1).takeIf { it > 0 }
        val contactName = intent.getStringExtra("CONTACT_NAME")
        val contactMobile = intent.getStringExtra("CONTACT_MOBILE")
        val contactEmail = intent.getStringExtra("CONTACT_EMAIL")
        
        // 1. Booking
        val booking = bookingsService.createBooking(
            BookingCreate(
                userId = user.id,
                tripId = tripId,
                pickupStopId = pickupId,
                dropoffStopId = dropId,
                contactName = contactName,
                contactMobile = contactMobile,
                contactEmail = contactEmail,
                totalAmount = totalPrice,
                bookingStatus = 1 // Confirmed
            )
        )
        
        // 2. Passengers
        val passengerNames = intent.getStringArrayListExtra("PASSENGER_NAMES")
        selectedSeats.forEachIndexed { index, seat ->
            val pName = passengerNames?.getOrNull(index) ?: "Guest"
            bookingPassengersService.createPassenger(
                BookingPassengerCreate(
                    bookingId = booking.id,
                    seatNumber = seat,
                    fullName = pName
                )
            )
        }
        
        // 3. Payment
        paymentsService.createPayment(
            PaymentCreate(
                bookingId = booking.id,
                transactionRef = "TXN-${UUID.randomUUID().toString().take(8).uppercase()}",
                amount = totalPrice.toInt(),
                paymentMethod = "Credit Card",
                paymentStatus = 1
            )
        )
        
        return booking.id
    }
}