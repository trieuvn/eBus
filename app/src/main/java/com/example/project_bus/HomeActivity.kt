package com.example.project_bus

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
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
import com.example.project_bus.ui.widgets.InstantAutoCompleteTextView
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Date

class HomeActivity : AppCompatActivity() {

    private val authService = AuthService()
    private val bookingsService = BookingsService()
    private val tripsService = TripsService()
    private val routesService = RoutesService()

    private lateinit var tvHomeUserName: TextView
    private lateinit var actvHomeOrigin: InstantAutoCompleteTextView
    private lateinit var actvHomeDestination: InstantAutoCompleteTextView
    private lateinit var btnHomeFindBus: AppCompatButton
    private lateinit var btnHomeSwapLocations: ImageButton
    private lateinit var tvHomeNoHistory: TextView
    private lateinit var rvHomeBookingHistory: RecyclerView
    private lateinit var btnHomeDateToday: TextView
    private lateinit var btnHomeDateTomorrow: TextView
    private lateinit var btnHomeDateOther: LinearLayout
    private lateinit var tvHomeDateOtherText: TextView
    
    private var selectedDateStr: String = ""
    private var routePairs: List<Pair<String, String>> = emptyList()
    private var allOrigins: List<String> = emptyList()
    private var allDestinations: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        
        val currentUser = SupabaseProvider.client.auth.currentUserOrNull()
        if (currentUser == null) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }
        
        initViews()
        selectDateOption("TODAY")
        setupListeners()
        loadLocationSuggestions()
        loadData(currentUser.id)
    }

    private fun initViews() {
        tvHomeUserName = findViewById(R.id.tvHomeUserName)
        actvHomeOrigin = findViewById(R.id.actvHomeOrigin)
        actvHomeDestination = findViewById(R.id.actvHomeDestination)
        btnHomeFindBus = findViewById(R.id.btnHomeFindBus)
        btnHomeSwapLocations = findViewById(R.id.btnHomeSwapLocations)
        tvHomeNoHistory = findViewById(R.id.tvHomeNoHistory)
        rvHomeBookingHistory = findViewById(R.id.rvHomeBookingHistory)
        rvHomeBookingHistory.layoutManager = LinearLayoutManager(this)
        
        btnHomeDateToday = findViewById(R.id.btnHomeDateToday)
        btnHomeDateTomorrow = findViewById(R.id.btnHomeDateTomorrow)
        btnHomeDateOther = findViewById(R.id.btnHomeDateOther)
        tvHomeDateOtherText = findViewById(R.id.tvHomeDateOtherText)
        
        findViewById<View>(R.id.navTicket)?.setOnClickListener { startActivity(Intent(this, BookingActivity::class.java)) }
        findViewById<View>(R.id.navWallet)?.setOnClickListener { Toast.makeText(this, "Wallet feature is under development.", Toast.LENGTH_SHORT).show() }
        findViewById<View>(R.id.navSettings)?.setOnClickListener {
            lifecycleScope.launch {
                SupabaseProvider.client.auth.signOut()
                val intent = Intent(this@HomeActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }
    }

    private fun setupListeners() {
        btnHomeFindBus.setOnClickListener {
            val fromLoc = actvHomeOrigin.text.toString().trim()
            val toLoc = actvHomeDestination.text.toString().trim()
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
        
        btnHomeSwapLocations.setOnClickListener {
            val temp = actvHomeOrigin.text.toString()
            actvHomeOrigin.setText(actvHomeDestination.text.toString())
            actvHomeDestination.setText(temp)
            updateDestinationSuggestions(actvHomeOrigin.text.toString())
        }
        
        btnHomeDateToday.setOnClickListener { selectDateOption("TODAY") }
        btnHomeDateTomorrow.setOnClickListener { selectDateOption("TOMORROW") }
        btnHomeDateOther.setOnClickListener { showDatePicker() }
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
            btnHomeDateOther.setBackgroundResource(R.drawable.bg_date_btn_selected)
            tvHomeDateOtherText.text = "$selectedDay/${selectedMonth + 1}"
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
                btnHomeDateToday.setBackgroundResource(R.drawable.bg_date_btn_selected)
            }
            "TOMORROW" -> {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                selectedDateStr = sdf.format(calendar.time)
                btnHomeDateTomorrow.setBackgroundResource(R.drawable.bg_date_btn_selected)
            }
        }
    }

    private fun resetDateButtonsUI() {
        btnHomeDateToday.setBackgroundResource(R.drawable.bg_date_btn_unselected)
        btnHomeDateTomorrow.setBackgroundResource(R.drawable.bg_date_btn_unselected)
        btnHomeDateOther.setBackgroundResource(R.drawable.bg_date_btn_unselected)
        tvHomeDateOtherText.text = "Other"
    }

    private fun loadLocationSuggestions() = lifecycleScope.launch {
        try {
            val routes = withContext(Dispatchers.IO) { routesService.getAllRoutes() }
            val pairs = routes.mapNotNull { parseRouteName(it.name) }
            routePairs = pairs
            allOrigins = pairs.map { it.first }.distinct().sorted()
            allDestinations = pairs.map { it.second }.distinct().sorted()
            
            setupAutoComplete(actvHomeOrigin, allOrigins)
            setupAutoComplete(actvHomeDestination, allDestinations)
            
            actvHomeOrigin.setOnItemClickListener { _, _, _, _ -> updateDestinationSuggestions(actvHomeOrigin.text.toString()) }
        } catch (e: Exception) { Log.e("Home", "Error suggestions: ${e.message}") }
    }

    private fun setupAutoComplete(view: InstantAutoCompleteTextView, items: List<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, items)
        view.setAdapter(adapter)
        view.setOnClickListener { view.showDropDown() }
        view.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) view.showDropDown() }
    }

    private fun updateDestinationSuggestions(originRaw: String) {
        val origin = originRaw.trim()
        val dests = if (origin.isBlank()) allDestinations else {
            routePairs.filter { it.first.equals(origin, ignoreCase = true) }
                .map { it.second }.distinct().sorted().ifEmpty { allDestinations }
        }
        actvHomeDestination.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, dests))
    }

    private fun parseRouteName(routeNameRaw: String): Pair<String, String>? {
        val routeName = routeNameRaw.trim()
        val separators = listOf("->", "→", "-")
        val sep = separators.firstOrNull { routeName.contains(it) } ?: return null
        val idx = routeName.indexOf(sep)
        val from = routeName.substring(0, idx).trim()
        val to = routeName.substring(idx + sep.length).trim()
        return if (from.isNotBlank() && to.isNotBlank()) Pair(from, to) else null
    }

    private fun formatDateTimeForDisplay(dateTimeStr: String?): Pair<String, String> {
        if (dateTimeStr.isNullOrEmpty()) return Pair("--:-- , --", "----.--.--")
        try {
            val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = try {
                parser.parse(dateTimeStr)
            } catch (e: Exception) {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateTimeStr)
            } ?: Date()

            val timeFormat = SimpleDateFormat("h a", Locale.getDefault())
            val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
            val timeStr = timeFormat.format(date)
            val dayStr = dayFormat.format(date)
            val line1 = "$timeStr , $dayStr"
            val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
            val line2 = dateFormat.format(date)
            return Pair(line1, line2)
        } catch (e: Exception) {
            return Pair(dateTimeStr.take(5) + " , --", "----.--.--")
        }
    }

    private fun loadData(userId: String) = lifecycleScope.launch {
        try {
            val userProfile = withContext(Dispatchers.IO) { try { authService.getProfileByAuthId(userId) } catch (e: Exception) { null } }
            val name = userProfile?.fullName ?: SupabaseProvider.client.auth.currentUserOrNull()?.email ?: "User"
            tvHomeUserName.text = "Hello $name!"
        } catch (e: Exception) { Log.e("Home", "Error profile: ${e.message}") }

        try {
            val bookings = withContext(Dispatchers.IO) { bookingsService.getBookingsByUserId(userId) }
            
            if (bookings.isEmpty()) {
                rvHomeBookingHistory.visibility = View.GONE
                tvHomeNoHistory.visibility = View.VISIBLE
                return@launch
            }
            val uiList = mutableListOf<BookingItem>()
            withContext(Dispatchers.IO) {
                bookings.forEachIndexed { index, booking ->
                    val trip = tripsService.getTripById(booking.tripId)
                    if (trip != null) {
                        val route = routesService.getRouteById(trip.routeId)
                        val routeName = route?.name ?: "Unknown"
                        val pair = parseRouteName(routeName)
                        
                        val dateTimeSource = if (trip.departureTime != null && trip.departureTime.contains("-")) {
                            trip.departureTime 
                        } else {
                            val datePart = booking.createdAt?.take(10) ?: "2024-01-01"
                            val timePart = trip.departureTime ?: "09:00:00"
                            "$datePart $timePart"
                        }

                        val (formattedTimeLine, formattedDateLine) = formatDateTimeForDisplay(dateTimeSource)

                        uiList.add(BookingItem(
                            bookingId = booking.id,
                            index = index + 1,
                            fromLoc = pair?.first ?: "Start",
                            toLoc = pair?.second ?: "End",
                            timeAndDay = formattedTimeLine,
                            fullDate = formattedDateLine
                        ))
                    }
                }
            }
            if (uiList.isEmpty()) {
                tvHomeNoHistory.visibility = View.VISIBLE
                rvHomeBookingHistory.visibility = View.GONE
            } else {
                tvHomeNoHistory.visibility = View.GONE
                rvHomeBookingHistory.visibility = View.VISIBLE
                
                rvHomeBookingHistory.adapter = BookingsAdapter(uiList) { bookingId ->
                    val intent = Intent(this@HomeActivity, TicketDetailActivity::class.java)
                    intent.putExtra("BOOKING_ID", bookingId)
                    startActivity(intent)
                }
            }
        } catch (e: Exception) { Log.e("Home", "Error bookings: ${e.message}") }
    }
}
