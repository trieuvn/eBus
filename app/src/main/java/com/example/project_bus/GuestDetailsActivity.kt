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

        // 1. Nhận dữ liệu
        val operator = intent.getStringExtra("OPERATOR") ?: "Bus"
        val busType = intent.getStringExtra("BUS_TYPE") ?: "Standard"
        val seats = intent.getStringArrayListExtra("SELECTED_SEATS") ?: arrayListOf()
        val total = intent.getDoubleExtra("TOTAL_PRICE", 0.0)

        // 2. Hiển thị thông tin Header
        findViewById<TextView>(R.id.tvBusName).text = operator
        findViewById<TextView>(R.id.tvBusType).text = busType
        // Giờ đi lấy từ Intent hoặc fix cứng tạm nếu chưa có
        // findViewById<TextView>(R.id.tvTripTime).text = "..."

        val userEmail = SupabaseProvider.client.auth.currentUserOrNull()?.email ?: "User"
        findViewById<TextView>(R.id.tvHeaderUser).text = "Hello $userEmail!"

        // Nút Back
        findViewById<android.view.View>(R.id.btnBack).setOnClickListener { finish() }

        // 3. TẠO FORM NHẬP LIỆU ĐỘNG
        containerPassengers = findViewById(R.id.containerPassengers)

        for ((index, seat) in seats.withIndex()) {
            addPassengerInput(index + 1, seat)
        }

        // 4. XỬ LÝ NÚT PROCEED
        findViewById<android.view.View>(R.id.btnProceedBook).setOnClickListener {
            if (validateInputs()) {
                val contactMobile = findViewById<EditText>(R.id.etContactMobile).text.toString()
                val contactEmail = findViewById<EditText>(R.id.etContactEmail).text.toString()

                // Collect passenger full names in the same order as SELECTED_SEATS
                val passengerNames = passengerInputViews.map { it.etName.text.toString().trim() }
                val contactName = passengerNames.firstOrNull().orEmpty()

                val nextIntent = Intent(this, PaymentActivity::class.java)
                nextIntent.putExtras(intent) // Chuyển tiếp toàn bộ dữ liệu cũ

                // Gửi thông tin liên hệ
                nextIntent.putExtra("CONTACT_MOBILE", contactMobile)
                nextIntent.putExtra("CONTACT_EMAIL", contactEmail)
                nextIntent.putExtra("CONTACT_NAME", contactName)

                // Gửi danh sách tên hành khách để PaymentActivity lưu vào Booking_passengers
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

        // Lưu tham chiếu để lấy dữ liệu sau này
        passengerInputViews.add(PassengerInputView(etName, etAge, rgGender))

        containerPassengers.addView(view)
    }

    private fun validateInputs(): Boolean {
        for (input in passengerInputViews) {
            if (input.etName.text.isEmpty()) {
                input.etName.error = "Required"
                return false
            }
            if (input.etAge.text.isEmpty()) {
                input.etAge.error = "Required"
                return false
            }
        }

        val mobile = findViewById<EditText>(R.id.etContactMobile)
        if (mobile.text.isEmpty()) {
            mobile.error = "Required"
            return false
        }

        return true
    }

    // Class helper để lưu trữ view của từng hành khách
    data class PassengerInputView(
        val etName: EditText,
        val etAge: EditText,
        val rgGender: RadioGroup
    )
}

