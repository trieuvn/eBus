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
            btnPayNow.setOnClickListener {
                processPayment(btnPayNow)
            }
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
                // Chạy tác vụ mạng trong IO Dispatcher để không chặn UI
                val bookingId = withContext(Dispatchers.IO) { createBookingRecord() }

                Log.d("PaymentActivity", "Success! Booking ID: $bookingId")
                Toast.makeText(this@PaymentActivity, "Booking Successful!", Toast.LENGTH_SHORT).show()

                val i = Intent(this@PaymentActivity, PaymentSuccessActivity::class.java)
                i.putExtra("BOOKING_ID", bookingId)
                startActivity(i)
                finish()
            } catch (e: Exception) {
                btn.isEnabled = true
                e.printStackTrace()
                Log.e("PaymentError", "Transaction Failed", e)

                // Hiển thị lỗi cụ thể lên màn hình để dễ sửa
                val errorMsg = e.message ?: "Unknown error"
                Toast.makeText(this@PaymentActivity, "Failed: $errorMsg", Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun createBookingRecord(): Long {
        val user = SupabaseProvider.client.auth.currentUserOrNull()
            ?: throw Exception("User not logged in. Please relogin.")

        val tripId = intent.getLongExtra("TRIP_ID", -1)
        if (tripId == -1L) throw Exception("Invalid Trip ID")

        val selectedSeats = intent.getStringArrayListExtra("SELECTED_SEATS") ?: arrayListOf()
        val totalPrice = intent.getDoubleExtra("TOTAL_PRICE", 0.0)

        // Lấy thông tin liên hệ, nếu thiếu thì lấy tên khách đầu tiên
        val passengerNames = intent.getStringArrayListExtra("PASSENGER_NAMES") ?: arrayListOf()
        var contactName = intent.getStringExtra("CONTACT_NAME")
        if (contactName.isNullOrBlank()) {
            contactName = passengerNames.firstOrNull() ?: "Unknown Guest"
        }

        val contactMobile = intent.getStringExtra("CONTACT_MOBILE")
        val contactEmail = intent.getStringExtra("CONTACT_EMAIL")

        Log.d("PaymentActivity", "Creating Booking... User: ${user.id}, Trip: $tripId")

        // 1. Tạo Booking
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
                bookingStatus = 1
            )
        )

        // 2. Tạo Passengers
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

        // 3. Tạo Payment
        paymentsService.createPayment(
            PaymentCreate(
                bookingId = booking.id,
                transactionRef = "TXN-${UUID.randomUUID().toString().take(8).uppercase()}",
                amount = totalPrice.toInt(),
                paymentMethod = "Card",
                paymentStatus = 1
            )
        )

        return booking.id
    }
}