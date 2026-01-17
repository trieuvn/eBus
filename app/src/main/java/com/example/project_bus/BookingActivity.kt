package com.example.project_bus

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project_bus.data.SupabaseProvider
import com.example.project_bus.data.services.RoutesService
import com.example.project_bus.data.services.TripsService
import com.example.project_bus.ui.adapters.TripsAdapter
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
    private lateinit var rvTrips: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking)

        // Bind Views
        tvSummaryFrom = findViewById(R.id.tvCardFrom)
        tvSummaryTo = findViewById(R.id.tvCardTo)
        tvSummaryDate = findViewById(R.id.tvCardDate)
        tvHeaderName = findViewById(R.id.tvHeaderName)
        rvTrips = findViewById(R.id.rvTrips)

        val backView: View? = findViewById(R.id.btnBack)
        backView?.setOnClickListener { finish() }

        rvTrips.layoutManager = LinearLayoutManager(this)

        // Get Intent Data
        val fromLoc = intent.getStringExtra("FROM_LOC") ?: "Kelaniya"
        val toLoc = intent.getStringExtra("TO_LOC") ?: "Colombo"
        val date = intent.getStringExtra("SELECTED_DATE") ?: "Today"

        // Display Data
        tvSummaryFrom.text = fromLoc
        tvSummaryTo.text = toLoc
        tvSummaryDate.text = "$date | Bus Day"

        val userEmail = SupabaseProvider.client.auth.currentUserOrNull()?.email ?: "User"
        tvHeaderName.text = "Hello $userEmail!"

        // Search
        searchTrips(fromLoc, toLoc, date)
    }

    private fun searchTrips(from: String, to: String, dateStr: String) = lifecycleScope.launch {
        Log.d("Booking", "Searching for $from -> $to")

        try {
            val allRoutes = withContext(Dispatchers.IO) { routesService.getAllRoutes() }

            val matchedRoute = allRoutes.find { route ->
                val name = route.name.lowercase()
                name.contains(from.lowercase()) && name.contains(to.lowercase())
            }

            if (matchedRoute == null) {
                Toast.makeText(this@BookingActivity, "No route found.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            // Dữ liệu này trả về List<models.Trip>
            val allTrips = withContext(Dispatchers.IO) { tripsService.getAllTrips() }

            val filteredTrips = allTrips.filter { it.routeId == matchedRoute.id && it.status == 1 }

            if (filteredTrips.isEmpty()) {
                Toast.makeText(this@BookingActivity, "No buses available.", Toast.LENGTH_SHORT).show()
                rvTrips.adapter = null
                return@launch
            }

            rvTrips.adapter = TripsAdapter(filteredTrips) { trip -> 
                val intent = Intent(this@BookingActivity, SeatSelectionActivity::class.java)
                intent.putExtra("TRIP_ID", trip.id)
                intent.putExtra("PRICE", trip.price)
                intent.putExtra("OPERATOR", trip.operatorName)
                intent.putExtra("BUS_TYPE", trip.busType)
                intent.putExtra("FROM_LOC", from)
                intent.putExtra("TO_LOC", to)
                intent.putExtra("DATE", dateStr)
                startActivity(intent)
            }

        } catch (e: Exception) {
            Log.e("BookingActivity", "searchTrips failed", e)
            Toast.makeText(this@BookingActivity, "Search failed. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }
}
