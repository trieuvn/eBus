package com.example.project_bus

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.project_bus.data.SupabaseProvider
import com.example.project_bus.data.models.RouteStop
import com.example.project_bus.data.services.RoutesStopService
import com.example.project_bus.data.services.TripsService
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import io.github.jan.supabase.auth.auth

class BoardingDropActivity : AppCompatActivity() {

    // Biến lưu dữ liệu
    private var selectedBoarding: String = ""
    private var selectedDrop: String = ""

    private var selectedBoardingStopId: Int? = null
    private var selectedDropStopId: Int? = null

    private val tripsService = TripsService()
    private val routesStopService = RoutesStopService()

    private var boardingStops: List<RouteStop> = emptyList()
    private var dropStops: List<RouteStop> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_boarding_drop)

        // 1. Ánh xạ View
        val tvHeaderFrom = findViewById<TextView>(R.id.tvHeaderFrom)
        val tvHeaderTo = findViewById<TextView>(R.id.tvHeaderTo)
        val tvHeaderDate = findViewById<TextView>(R.id.tvHeaderDate)
        val tvBusName = findViewById<TextView>(R.id.tvBusName)
        val tvBusType = findViewById<TextView>(R.id.tvBusType)
        val tvTicketPrice = findViewById<TextView>(R.id.tvTicketPrice)
        val tvSeatNumbers = findViewById<TextView>(R.id.tvSeatNumbers)
        val tvTotalFare = findViewById<TextView>(R.id.tvTotalFare)
        val tvHeaderUser = findViewById<TextView>(R.id.tvHeaderUser)

        val layoutBoarding = findViewById<LinearLayout>(R.id.layoutBoarding)
        val tvSelectedBoarding = findViewById<TextView>(R.id.tvSelectedBoarding)
        val layoutDrop = findViewById<LinearLayout>(R.id.layoutDrop)
        val tvSelectedDrop = findViewById<TextView>(R.id.tvSelectedDrop)
        val btnNext = findViewById<View>(R.id.btnNext)
        val btnBack = findViewById<View>(R.id.btnBack)

        // 2. Nhận dữ liệu từ Intent
        val tripId = intent.getLongExtra("TRIP_ID", 0L)
        val operator = intent.getStringExtra("OPERATOR") ?: "Bus"
        val busType = intent.getStringExtra("BUS_TYPE") ?: "Standard"
        val fromLoc = intent.getStringExtra("FROM_LOC") ?: "Start"
        val toLoc = intent.getStringExtra("TO_LOC") ?: "End"
        val date = intent.getStringExtra("DATE") ?: "Date"
        val seats = intent.getStringArrayListExtra("SELECTED_SEATS") ?: arrayListOf()
        val total = intent.getDoubleExtra("TOTAL_PRICE", 0.0)

        // 3. Hiển thị dữ liệu lên màn hình
        tvHeaderFrom.text = fromLoc
        tvHeaderTo.text = toLoc
        tvHeaderDate.text = date
        tvBusName.text = operator
        tvBusType.text = busType
        tvSeatNumbers.text = seats.joinToString(", ")
        tvTotalFare.text = "LKR ${total.toInt()}"
        tvTicketPrice.text = "LKR ${total.toInt()}" // Giá hiển thị trên card nhỏ (hoặc giá đơn vị nếu muốn)

        val userEmail = SupabaseProvider.client.auth.currentUserOrNull()?.email ?: "User"
        tvHeaderUser.text = "Hello $userEmail!"

        // 4. Xử lý nút chọn điểm Đón (Giả lập Menu)
        layoutBoarding.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            // Thêm dữ liệu giả lập (Sau này lấy từ API)
            popup.menu.add("Main Stand - $fromLoc")
            popup.menu.add("Town Hall Stop")
            popup.menu.add("Post Office Junction")

            popup.setOnMenuItemClickListener { item ->
                selectedBoarding = item.title.toString()
                tvSelectedBoarding.text = selectedBoarding
                tvSelectedBoarding.setTextColor(resources.getColor(R.color.black, null)) // Đổi màu chữ cho đậm
                true
            }
            popup.show()
        }

        // 5. Xử lý nút chọn điểm Trả
        layoutDrop.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menu.add("Main Stand - $toLoc")
            popup.menu.add("City Center")
            popup.menu.add("New Bazaar Stop")

            popup.setOnMenuItemClickListener { item ->
                selectedDrop = item.title.toString()
                tvSelectedDrop.text = selectedDrop
                tvSelectedDrop.setTextColor(resources.getColor(R.color.black, null))
                true
            }
            popup.show()
        }

        // 6. Nút Proceed (Tiếp tục sang thanh toán)
        btnNext.setOnClickListener {
            if (selectedBoarding.isEmpty() || selectedDrop.isEmpty()) {
                Toast.makeText(this, "Please select Boarding and Drop points", Toast.LENGTH_SHORT).show()
            } else {
                // --- SỬA THÀNH GuestDetailsActivity ---
                val nextIntent = Intent(this, GuestDetailsActivity::class.java)
                nextIntent.putExtras(intent) // Chuyển tiếp toàn bộ dữ liệu cũ

                // Gửi thêm điểm đón trả mới chọn
                nextIntent.putExtra("BOARDING_POINT", selectedBoarding)
                nextIntent.putExtra("DROP_POINT", selectedDrop)

                startActivity(nextIntent)
            }
        }

        btnBack.setOnClickListener { finish() }
    }
}

