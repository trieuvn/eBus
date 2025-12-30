package com.example.project_bus

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.example.project_bus.data.SupabaseProvider
import com.example.project_bus.data.models.Booking
import com.example.project_bus.data.services.AuthService
import com.example.project_bus.data.services.BookingsService
import com.example.project_bus.data.services.RoutesService
import com.example.project_bus.data.services.TripsService
import com.example.project_bus.ui.auth.LoginActivity
// Import quan trọng để dùng tính năng Auth
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeActivity : AppCompatActivity() {

    // Services
    private val authService = AuthService()
    private val bookingsService = BookingsService()
    private val tripsService = TripsService()
    private val routesService = RoutesService()

    // UI Elements
    private lateinit var tvUserName: TextView
    private lateinit var etFrom: EditText
    private lateinit var etTo: EditText
    private lateinit var btnSearch: AppCompatButton
    private lateinit var btnSwap: ImageButton
    private lateinit var tvNoUpcoming: TextView

    // Card 1
    private lateinit var cardUpcoming1: CardView
    private lateinit var tvUp1From: TextView
    private lateinit var tvUp1To: TextView
    private lateinit var tvUp1Time: TextView
    private lateinit var tvUp1Date: TextView

    // Card 2
    private lateinit var cardUpcoming2: CardView
    private lateinit var tvUp2From: TextView
    private lateinit var tvUp2To: TextView
    private lateinit var tvUp2Time: TextView
    private lateinit var tvUp2Date: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // --- BƯỚC 1: KIỂM TRA ĐĂNG NHẬP CHUẨN ---
        val currentUser = SupabaseProvider.client.auth.currentUserOrNull()

        if (currentUser == null) {
            // Nếu chưa đăng nhập, đá về trang Login ngay lập tức
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }
        // ----------------------------------------

        initViews()
        setupListeners()

        // --- BƯỚC 2: TẢI DỮ LIỆU CỦA USER ĐANG ĐĂNG NHẬP ---
        loadData(currentUser.id)
    }

    private fun initViews() {
        tvUserName = findViewById(R.id.tvUserName)
        etFrom = findViewById(R.id.etFrom)
        etTo = findViewById(R.id.etTo)
        btnSearch = findViewById(R.id.btnSearch)
        btnSwap = findViewById(R.id.btnSwap)

        // Xử lý an toàn nếu view tvNoUpcoming chưa có trong layout
        tvNoUpcoming = try { findViewById(R.id.tvNoUpcoming) } catch (e: Exception) { null } ?: TextView(this)

        // Card 1
        cardUpcoming1 = findViewById(R.id.cardUpcoming1)
        tvUp1From = findViewById(R.id.tvUp1From)
        tvUp1To = findViewById(R.id.tvUp1To)
        tvUp1Time = findViewById(R.id.tvUp1Time)
        tvUp1Date = findViewById(R.id.tvUp1Date)

        // Card 2
        cardUpcoming2 = findViewById(R.id.cardUpcoming2)
        tvUp2From = findViewById(R.id.tvUp2From)
        tvUp2To = findViewById(R.id.tvUp2To)
        tvUp2Time = findViewById(R.id.tvUp2Time)
        tvUp2Date = findViewById(R.id.tvUp2Date)

        // Setup Bottom Nav Link
        val navTicket = findViewById<View>(R.id.navTicket)
        navTicket?.setOnClickListener {
            startActivity(Intent(this, BookingActivity::class.java))
        }
    }

    private fun setupListeners() {
        btnSearch.setOnClickListener {
            val fromLoc = etFrom.text.toString().trim()
            val toLoc = etTo.text.toString().trim()
            if (fromLoc.isEmpty() || toLoc.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập điểm đi và đến", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this, BookingActivity::class.java).apply {
                putExtra("FROM_LOC", fromLoc)
                putExtra("TO_LOC", toLoc)
            }
            startActivity(intent)
        }

        btnSwap.setOnClickListener {
            val temp = etFrom.text.toString()
            etFrom.setText(etTo.text.toString())
            etTo.setText(temp)
        }
    }

    private fun loadData(userId: String) = lifecycleScope.launch {
        // 1. Load Profile
        try {
            val userProfile = withContext(Dispatchers.IO) {
                try { authService.getProfileByAuthId(userId) } catch (e: Exception) { null }
            }
            // Hiển thị tên hoặc email nếu chưa có tên
            val name = userProfile?.fullName ?: SupabaseProvider.client.auth.currentUserOrNull()?.email ?: "User"
            tvUserName.text = "Hello $name!"
        } catch (e: Exception) {
            Log.e("HomeActivity", "Error loading profile: ${e.message}")
        }

        // 2. Load Bookings
        try {
            val bookings = withContext(Dispatchers.IO) {
                bookingsService.getBookingsByUserId(userId)
            }

            // Ẩn các card trước khi bind dữ liệu mới
            withContext(Dispatchers.Main) {
                cardUpcoming1.visibility = View.GONE
                cardUpcoming2.visibility = View.GONE
            }

            if (bookings.isEmpty()) {
                // Có thể hiển thị text thông báo "Bạn chưa có vé" ở đây nếu muốn
                return@launch
            }

            if (bookings.isNotEmpty()) {
                bindBookingToCard(bookings[0], cardUpcoming1, tvUp1From, tvUp1To, tvUp1Time, tvUp1Date)
            }
            if (bookings.size > 1) {
                bindBookingToCard(bookings[1], cardUpcoming2, tvUp2From, tvUp2To, tvUp2Time, tvUp2Date)
            }

        } catch (e: Exception) {
            Log.e("HomeActivity", "Error loading bookings: ${e.message}")
            withContext(Dispatchers.Main) {
                Toast.makeText(this@HomeActivity, "Lỗi tải dữ liệu vé", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun bindBookingToCard(
        booking: Booking,
        card: CardView,
        tvFrom: TextView,
        tvTo: TextView,
        tvTime: TextView,
        tvDate: TextView
    ) {
        withContext(Dispatchers.IO) {
            val trip = tripsService.getTripById(booking.tripId)
            if (trip != null) {
                val route = routesService.getRouteById(trip.routeId)

                withContext(Dispatchers.Main) {
                    val routeName = route?.name ?: "Unknown Route"
                    if (routeName.contains("-") || routeName.contains("->")) {
                        // Xử lý tách chuỗi tên tuyến đường
                        val separator = if (routeName.contains("->")) "->" else "-"
                        val parts = routeName.split(separator)
                        tvFrom.text = "From : ${parts[0].trim()}"
                        tvTo.text = "To : ${parts[1].trim()}"
                    } else {
                        tvFrom.text = "From : $routeName"
                        tvTo.text = "To : ..."
                    }

                    tvTime.text = trip.departureTime?.take(5) ?: "00:00"
                    tvDate.text = booking.createdAt?.take(10) ?: "Upcoming"
                    card.visibility = View.VISIBLE
                }
            }
        }
    }
}