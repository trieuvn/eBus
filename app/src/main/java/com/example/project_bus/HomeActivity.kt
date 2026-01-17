package com.example.project_bus

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AutoCompleteTextView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project_bus.data.SupabaseProvider
import com.example.project_bus.data.services.AuthService
import com.example.project_bus.data.services.BookingsService
import com.example.project_bus.data.services.RoutesService
import com.example.project_bus.data.services.TripsService
import com.example.project_bus.ui.auth.LoginActivity
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HomeActivity : AppCompatActivity() {

    // Services
    private val authService = AuthService()
    private val bookingsService = BookingsService()
    private val tripsService = TripsService()
    private val routesService = RoutesService()

    // UI Elements
    private lateinit var tvUserName: TextView
    private lateinit var etFrom: AutoCompleteTextView
    private lateinit var etTo: AutoCompleteTextView
    private lateinit var btnSearch: AppCompatButton
    private lateinit var btnSwap: ImageButton
    private lateinit var tvNoUpcoming: TextView

    // --- THAY ĐỔI QUAN TRỌNG: Dùng RecyclerView thay vì CardView lẻ ---
    private lateinit var rvBookings: RecyclerView

    // Các nút chọn ngày
    private lateinit var btnToday: TextView
    private lateinit var btnTomorrow: TextView
    private lateinit var btnOtherDate: LinearLayout
    private lateinit var tvOtherDateText: TextView

    // Biến lưu ngày đã chọn (Mặc định là hôm nay)
    private var selectedDateStr: String = ""

    // --- Data for origin/destination suggestions (Routes) ---
    private var routePairs: List<Pair<String, String>> = emptyList()
    private var allOrigins: List<String> = emptyList()
    private var allDestinations: List<String> = emptyList()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // 1. Kiểm tra Auth (Bắt buộc phải đăng nhập)
        val currentUser = SupabaseProvider.client.auth.currentUserOrNull()
        if (currentUser == null) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        initViews()

        // Mặc định chọn "Hôm nay" khi vừa mở
        selectDateOption("TODAY")

        setupListeners()
        loadLocationSuggestions()
        loadData(currentUser.id)
    }

    private fun initViews() {
        tvUserName = findViewById(R.id.tvUserName)
        etFrom = findViewById(R.id.etFrom)
        etTo = findViewById(R.id.etTo)
        btnSearch = findViewById(R.id.btnSearch)
        btnSwap = findViewById(R.id.btnSwap)

        // Tránh lỗi null nếu view chưa có
        tvNoUpcoming = try { findViewById(R.id.tvNoUpcoming) } catch (e: Exception) { null } ?: TextView(this)

        // Date Buttons
        btnToday = findViewById(R.id.btnToday)
        btnTomorrow = findViewById(R.id.btnTomorrow)
        btnOtherDate = findViewById(R.id.btnOtherDate)
        tvOtherDateText = findViewById(R.id.tvOtherDateText)

        // --- Setup RecyclerView (Danh sách cuộn) ---
        rvBookings = findViewById(R.id.rvBookings)
        rvBookings.layoutManager = LinearLayoutManager(this)

        // --- Bottom Navigation ---
        findViewById<View>(R.id.navTicket)?.setOnClickListener {
            startActivity(Intent(this, BookingActivity::class.java))
        }

        findViewById<View>(R.id.navWallet)?.setOnClickListener {
            Toast.makeText(this, "Wallet feature is under development.", Toast.LENGTH_SHORT).show()
        }

        // Nút Settings -> Đăng xuất
        findViewById<View>(R.id.navSettings)?.setOnClickListener {
            lifecycleScope.launch {
                SupabaseProvider.client.auth.signOut()
                Toast.makeText(this@HomeActivity, "Signed out.", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@HomeActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }
    }

    private fun setupListeners() {
        // Nút Tìm kiếm
        btnSearch.setOnClickListener {
            val fromLoc = etFrom.text.toString().trim()
            val toLoc = etTo.text.toString().trim()

            if (fromLoc.isEmpty() || toLoc.isEmpty()) {
                Toast.makeText(this, "Please enter origin and destination.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, BookingActivity::class.java).apply {
                putExtra("FROM_LOC", fromLoc)
                putExtra("TO_LOC", toLoc)
                putExtra("SELECTED_DATE", selectedDateStr)
            }
            startActivity(intent)
        }

        // Nút Đảo chiều
        btnSwap.setOnClickListener {
            val temp = etFrom.text.toString()
            etFrom.setText(etTo.text.toString())
            etTo.setText(temp)

            // After swapping, refresh destination suggestions based on new origin
            updateDestinationSuggestions(etFrom.text.toString())
        }

        // --- Sự kiện chọn ngày ---
        btnToday.setOnClickListener { selectDateOption("TODAY") }
        btnTomorrow.setOnClickListener { selectDateOption("TOMORROW") }
        btnOtherDate.setOnClickListener { showDatePicker() }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val formattedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
            selectedDateStr = formattedDate

            resetDateButtonsUI()
            btnOtherDate.alpha = 1.0f
            tvOtherDateText.text = "$selectedDay/${selectedMonth + 1}"

        }, year, month, day)

        datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000
        datePickerDialog.show()
    }

    private fun selectDateOption(option: String) {
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        resetDateButtonsUI()

        when (option) {
            "TODAY" -> {
                selectedDateStr = sdf.format(calendar.time)
                btnToday.alpha = 1.0f
            }
            "TOMORROW" -> {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                selectedDateStr = sdf.format(calendar.time)
                btnTomorrow.alpha = 1.0f
            }
        }
    }

    private fun resetDateButtonsUI() {
        btnToday.alpha = 0.5f
        btnTomorrow.alpha = 0.5f
        btnOtherDate.alpha = 0.5f
        tvOtherDateText.text = "Other"
    }

    /**
     * Load origin/destination suggestions from the Routes table (Supabase).
     * Route names are expected in formats like "TP.HCM -> Vũng Tàu" or "Hà Nội - Hải Phòng".
     */
    private fun loadLocationSuggestions() = lifecycleScope.launch {
        try {
            val routes = withContext(Dispatchers.IO) { routesService.getAllRoutes() }

            val pairs = routes.mapNotNull { parseRouteName(it.name) }
            routePairs = pairs

            allOrigins = pairs.map { it.first }.distinct().sorted()
            allDestinations = pairs.map { it.second }.distinct().sorted()

            // Setup adapters
            setupAutoComplete(etFrom, allOrigins)
            setupAutoComplete(etTo, allDestinations)

            // When selecting an origin, restrict destination suggestions
            etFrom.setOnItemClickListener { _, _, _, _ ->
                updateDestinationSuggestions(etFrom.text.toString())
            }

        } catch (e: Exception) {
            Log.e("Home", "Error loading location suggestions: ${e.message}")
        }
    }

    private fun setupAutoComplete(view: AutoCompleteTextView, items: List<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, items)
        view.setAdapter(adapter)

        // Show dropdown when user taps into the field.
        // NOTE: AutoCompleteTextView normally requires enough characters to filter.
        // We use InstantAutoCompleteTextView (custom view) in XML to always allow dropdown.
        view.setOnClickListener { view.showDropDown() }
        view.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) view.showDropDown()
        }
    }

    private fun updateDestinationSuggestions(originRaw: String) {
        val origin = originRaw.trim()
        val dests = if (origin.isBlank()) {
            allDestinations
        } else {
            routePairs
                .filter { it.first.equals(origin, ignoreCase = true) }
                .map { it.second }
                .distinct()
                .sorted()
                .ifEmpty { allDestinations }
        }

        etTo.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, dests))

        // If the currently typed destination is not valid for the selected origin, clear it
        val currentTo = etTo.text.toString().trim()
        if (origin.isNotBlank() && currentTo.isNotBlank() && dests.none { it.equals(currentTo, ignoreCase = true) }) {
            etTo.setText("")
        }
    }

    private fun parseRouteName(routeNameRaw: String): Pair<String, String>? {
        val routeName = routeNameRaw.trim()
        if (routeName.isBlank()) return null

        // Supported separators: "->", "→", "-"
        val separators = listOf("->", "→", "-")
        val sep = separators.firstOrNull { routeName.contains(it) } ?: return null
        val idx = routeName.indexOf(sep)
        if (idx <= 0 || idx >= routeName.length - sep.length) return null

        val from = routeName.substring(0, idx).trim()
        val to = routeName.substring(idx + sep.length).trim()
        if (from.isBlank() || to.isBlank()) return null
        return Pair(from, to)
    }

    private fun loadData(userId: String) = lifecycleScope.launch {
        // A. Load Profile
        try {
            val userProfile = withContext(Dispatchers.IO) {
                try { authService.getProfileByAuthId(userId) } catch (e: Exception) { null }
            }
            val name = userProfile?.fullName ?: SupabaseProvider.client.auth.currentUserOrNull()?.email ?: "User"
            tvUserName.text = "Hello $name!"
        } catch (e: Exception) { Log.e("Home", "Error profile: ${e.message}") }

        // B. Load Bookings List (Sử dụng RecyclerView)
        try {
            val bookings = withContext(Dispatchers.IO) {
                bookingsService.getBookingsByUserId(userId)
            }

            if (bookings.isEmpty()) {
                withContext(Dispatchers.Main) {
                    rvBookings.visibility = View.GONE
                    tvNoUpcoming.visibility = View.VISIBLE
                    tvNoUpcoming.text = "Bạn chưa có chuyến đi nào."
                }
                return@launch
            }

            // Chuyển đổi dữ liệu sang BookingItem cho Adapter
            val uiList = mutableListOf<BookingItem>()
            withContext(Dispatchers.IO) {
                bookings.forEachIndexed { index, booking ->
                    val trip = tripsService.getTripById(booking.tripId)
                    if (trip != null) {
                        val route = routesService.getRouteById(trip.routeId)
                        val routeName = route?.name ?: "Unknown"

                        var txtFrom = "Start"
                        var txtTo = "End"
                        if (routeName.contains("-") || routeName.contains("->")) {
                            val separator = if (routeName.contains("->")) "->" else "-"
                            val parts = routeName.split(separator)
                            txtFrom = "From: " + parts[0].trim()
                            txtTo = "To: " + parts[1].trim()
                        } else {
                            txtFrom = "From: $routeName"
                            txtTo = ""
                        }

                        uiList.add(
                            BookingItem(
                                index = index + 1,
                                fromLoc = txtFrom,
                                toLoc = txtTo,
                                time = trip.departureTime?.take(5) ?: "--:--",
                                date = booking.createdAt?.take(10) ?: "Upcoming"
                            )
                        )
                    }
                }
            }

            // Gán Adapter
            withContext(Dispatchers.Main) {
                if (uiList.isEmpty()) {
                    tvNoUpcoming.visibility = View.VISIBLE
                } else {
                    tvNoUpcoming.visibility = View.GONE
                    rvBookings.visibility = View.VISIBLE
                    // QUAN TRỌNG: Gọi BookingsAdapter
                    rvBookings.adapter = BookingsAdapter(uiList)
                }
            }
        } catch (e: Exception) {
            Log.e("Home", "Error bookings: ${e.message}")
        }
    }
}
