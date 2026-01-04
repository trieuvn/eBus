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

        // 4. Tải danh sách điểm đón/trả từ DB theo route của Trip
        loadRouteStopsForTrip(tripId)

        // 5. Chọn điểm Đón
        layoutBoarding.setOnClickListener { anchor ->
            val list = if (boardingStops.isNotEmpty()) boardingStops else emptyList()
            showStopsPopup(
                anchor = anchor,
                stops = list,
                fallbackItems = listOf("Main Stand - $fromLoc", "Town Hall Stop", "Post Office Junction")
            ) { stopId, title ->
                selectedBoardingStopId = stopId
                selectedBoarding = title
                tvSelectedBoarding.text = selectedBoarding
                tvSelectedBoarding.setTextColor(resources.getColor(R.color.black, null))
            }
        }

        // 6. Chọn điểm Trả
        layoutDrop.setOnClickListener { anchor ->
            val list = if (dropStops.isNotEmpty()) dropStops else emptyList()
            showStopsPopup(
                anchor = anchor,
                stops = list,
                fallbackItems = listOf("Main Stand - $toLoc", "City Center", "New Bazaar Stop")
            ) { stopId, title ->
                selectedDropStopId = stopId
                selectedDrop = title
                tvSelectedDrop.text = selectedDrop
                tvSelectedDrop.setTextColor(resources.getColor(R.color.black, null))
            }
        }

        // 7. Nút Proceed (Tiếp tục sang thanh toán)
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

                // Gửi cả stop_id để lưu vào Bookings
                selectedBoardingStopId?.let { nextIntent.putExtra("PICKUP_STOP_ID", it) }
                selectedDropStopId?.let { nextIntent.putExtra("DROPOFF_STOP_ID", it) }

                startActivity(nextIntent)
            }
        }

        btnBack.setOnClickListener { finish() }
    }

    private fun loadRouteStopsForTrip(tripId: Long) {
        if (tripId <= 0L) return

        lifecycleScope.launch {
            try {
                val trip = withContext(Dispatchers.IO) { tripsService.getTripById(tripId) }
                val routeId = trip?.routeId
                if (routeId == null) return@launch

                val allStops = withContext(Dispatchers.IO) { routesStopService.getStopsByRouteId(routeId) }
                // stopType is an Int in DB. Common convention: 1 = pickup/boarding, 2 = dropoff.
                val pickups = allStops.filter { it.stopType == 1 }
                val drops = allStops.filter { it.stopType == 2 }

                boardingStops = if (pickups.isNotEmpty()) pickups else allStops
                dropStops = if (drops.isNotEmpty()) drops else allStops

            } catch (e: Exception) {
                Toast.makeText(this@BoardingDropActivity, "Failed to load boarding points: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Shows a popup menu for RouteStop list (or fallback string items).
     * onSelected provides (stopId?, title).
     */
    private fun showStopsPopup(
        anchor: View,
        stops: List<RouteStop>,
        fallbackItems: List<String>,
        onSelected: (Int?, String) -> Unit
    ) {
        val popup = PopupMenu(this, anchor)

        if (stops.isNotEmpty()) {
            stops.forEachIndexed { idx, stop ->
                val title = stop.locationName ?: "Stop ${stop.id ?: idx}"
                val id = stop.id ?: (100000 + idx)
                popup.menu.add(0, id, idx, title)
            }
        } else {
            fallbackItems.forEachIndexed { idx, title ->
                popup.menu.add(0, 100000 + idx, idx, title)
            }
        }

        popup.setOnMenuItemClickListener { item ->
            val title = item.title.toString()
            // Map back to a real stop id if it exists in the provided list
            val matchedStopId = stops.firstOrNull {
                val stopTitle = it.locationName ?: "Stop ${it.id}"
                stopTitle == title
            }?.id
            onSelected(matchedStopId, title)
            true
        }
        popup.show()
    }
}

