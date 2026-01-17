package com.example.project_bus

import android.content.Intent
import android.os.Bundle
import android.view.View // Import View chung
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project_bus.data.SupabaseProvider
import com.example.project_bus.data.services.RoutesService
import com.example.project_bus.data.services.TripsService
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookingActivity : AppCompatActivity() {

    private val routesService = RoutesService()
    private val tripsService = TripsService()

    private lateinit var tvSummaryFrom: TextView
    private lateinit var tvSummaryTo: TextView
    private lateinit var tvSummaryDate: TextView
    private lateinit var tvHeaderName: TextView
    private lateinit var tvResultCount: TextView
    private lateinit var rvTrips: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking)

        // 1. Ánh xạ View
        tvSummaryFrom = findViewById(R.id.tvSummaryFrom)
        tvSummaryTo = findViewById(R.id.tvSummaryTo)
        tvSummaryDate = findViewById(R.id.tvSummaryDate)
        tvHeaderName = findViewById(R.id.tvHeaderName)
        tvResultCount = findViewById(R.id.tvResultCount)
        rvTrips = findViewById(R.id.rvTrips)

        // Sửa lỗi: Tìm View chung vì btnBack bây giờ là CardView
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        rvTrips.layoutManager = LinearLayoutManager(this)

        // 2. Nhận dữ liệu
        val fromLoc = intent.getStringExtra("FROM_LOC") ?: "Kelaniya"
        val toLoc = intent.getStringExtra("TO_LOC") ?: "Colombo"
        val date = intent.getStringExtra("SELECTED_DATE") ?: "Today"

        // 3. Hiển thị dữ liệu
        tvSummaryFrom.text = fromLoc
        tvSummaryTo.text = toLoc

        // Sử dụng String Template để tránh warning cộng chuỗi
        tvSummaryDate.text = "$date | Bus Day"

        val userEmail = SupabaseProvider.client.auth.currentUserOrNull()?.email ?: "User"
        tvHeaderName.text = "Hello $userEmail!"

        // 4. Tìm kiếm
        searchTrips(fromLoc, toLoc)
    }

    private fun searchTrips(from: String, to: String) = lifecycleScope.launch {
        tvResultCount.text = "Searching..."

        try {
            val allRoutes = withContext(Dispatchers.IO) {
                routesService.getAllRoutes()
            }

            val matchedRoute = allRoutes.find { route ->
                val name = route.name.orEmpty().lowercase()
                name.contains(from.lowercase()) && name.contains(to.lowercase())
            }

            if (matchedRoute == null) {
                tvResultCount.text = "No route found."
                return@launch
            }

            val allTrips = withContext(Dispatchers.IO) {
                tripsService.getAllTrips()
            }

            val filteredTrips = allTrips.filter {
                it.routeId == matchedRoute.id && it.status == 1
            }

            if (filteredTrips.isEmpty()) {
                tvResultCount.text = "No buses available."
            } else {
                tvResultCount.text = "Found ${filteredTrips.size} buses"

                val adapter = TripsAdapter(filteredTrips) { selectedTrip ->
                    val intent = Intent(this@BookingActivity, SeatSelectionActivity::class.java)

                    intent.putExtra("TRIP_ID", selectedTrip.id)
                    intent.putExtra("PRICE", selectedTrip.price)
                    intent.putExtra("OPERATOR", selectedTrip.operatorName)
                    intent.putExtra("BUS_TYPE", selectedTrip.busType)
                    intent.putExtra("FROM_LOC", from)
                    intent.putExtra("TO_LOC", to)

                    val dateStr = this@BookingActivity.intent.getStringExtra("SELECTED_DATE") ?: "Today"
                    intent.putExtra("DATE", dateStr)

                    startActivity(intent)
                }
                rvTrips.adapter = adapter
            }

        } catch (e: Exception) {
            tvResultCount.text = "Error: ${e.message}"
        }
    }
}

