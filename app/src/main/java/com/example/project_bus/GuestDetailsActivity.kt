package com.example.project_bus

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.project_bus.data.SupabaseProvider
import io.github.jan.supabase.auth.auth

class GuestDetailsActivity : AppCompatActivity() {

    private lateinit var containerPassengers: LinearLayout
    private val passengerInputViews = ArrayList<PassengerInputView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guest_details)

        val operator = intent.getStringExtra("OPERATOR") ?: "Bus"
        val busType = intent.getStringExtra("BUS_TYPE") ?: "Standard"
        val seats = intent.getStringArrayListExtra("SELECTED_SEATS") ?: arrayListOf()
        // val total = intent.getDoubleExtra("TOTAL_PRICE", 0.0) // Không cần dùng ở đây

        findViewById<TextView>(R.id.tvBusName).text = operator
        findViewById<TextView>(R.id.tvBusType).text = busType

        val userEmail = SupabaseProvider.client.auth.currentUserOrNull()?.email ?: "User"
        findViewById<TextView>(R.id.tvHeaderUser).text = "Hello $userEmail!"

        findViewById<android.view.View>(R.id.btnBack).setOnClickListener { finish() }

        containerPassengers = findViewById(R.id.containerPassengers)

        // Tạo ô nhập liệu cho từng ghế
        for ((index, seat) in seats.withIndex()) {
            addPassengerInput(index + 1, seat)
        }

        findViewById<android.view.View>(R.id.btnProceedBook).setOnClickListener {
            if (validateInputs()) {
                val contactMobile = findViewById<EditText>(R.id.etContactMobile).text.toString().trim()
                val contactEmail = findViewById<EditText>(R.id.etContactEmail).text.toString().trim()

                // Lấy danh sách tên hành khách
                val passengerNames = passengerInputViews.map { it.etName.text.toString().trim() }

                // Lấy tên người liên hệ (Mặc định là người đầu tiên nếu không nhập gì khác)
                // Vì layout không có ô Contact Name, ta lấy tên hành khách đầu tiên làm đại diện
                val contactName = passengerNames.firstOrNull() ?: "Unknown"

                val nextIntent = Intent(this, PaymentActivity::class.java)
                nextIntent.putExtras(intent) // Truyền tiếp các dữ liệu cũ (TripID, Seats, Price...)

                nextIntent.putExtra("CONTACT_NAME", contactName) // Thêm dòng này
                nextIntent.putExtra("CONTACT_MOBILE", contactMobile)
                nextIntent.putExtra("CONTACT_EMAIL", contactEmail)
                nextIntent.putStringArrayListExtra("PASSENGER_NAMES", ArrayList(passengerNames))

                startActivity(nextIntent)
            }
        }
    }

    private fun addPassengerInput(index: Int, seatName: String) {
        val view = LayoutInflater.from(this).inflate(R.layout.item_passenger_input, containerPassengers, false)

        val tvLabel = view.findViewById<TextView>(R.id.tvPassengerLabel)
        val etName = view.findViewById<EditText>(R.id.etFullName)
        val etAge = view.findViewById<EditText>(R.id.etAge)
        val rgGender = view.findViewById<RadioGroup>(R.id.rgGender)

        tvLabel.text = "Passenger $index (Seat $seatName)"

        passengerInputViews.add(PassengerInputView(etName, etAge, rgGender))
        containerPassengers.addView(view)
    }

    private fun validateInputs(): Boolean {
        for (input in passengerInputViews) {
            if (input.etName.text.isBlank()) {
                input.etName.error = "Required"
                return false
            }
            if (input.etAge.text.isBlank()) {
                input.etAge.error = "Required"
                return false
            }
        }

        val mobile = findViewById<EditText>(R.id.etContactMobile)
        if (mobile.text.isBlank()) {
            mobile.error = "Required"
            return false
        }

        val email = findViewById<EditText>(R.id.etContactEmail)
        if (email.text.isBlank()) {
            email.error = "Required"
            return false
        }

        return true
    }

    data class PassengerInputView(
        val etName: EditText,
        val etAge: EditText,
        val rgGender: RadioGroup
    )
}