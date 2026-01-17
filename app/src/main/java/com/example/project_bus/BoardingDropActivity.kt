package com.example.project_bus

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.project_bus.data.SupabaseProvider
import com.example.project_bus.data.models.RouteStop
import com.example.project_bus.data.services.RoutesStopService
import com.example.project_bus.data.services.TripsService
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class BoardingDropActivity : AppCompatActivity() {

    private val routesStopService = RoutesStopService()
    private val tripsService = TripsService()

    private lateinit var tvSelectedBoarding: TextView
    private lateinit var tvSelectedDrop: TextView
    private var selectedBoardingId: Int = -1
    private var selectedDropId: Int = -1
    private var availableStops = listOf<RouteStop>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_boarding_drop)

        val tripId = intent.getLongExtra("TRIP_ID", -1)
        val fromLoc = intent.getStringExtra("FROM_LOC") ?: ""
        val toLoc = intent.getStringExtra("TO_LOC") ?: ""
        val totalPrice = intent.getDoubleExtra("TOTAL_PRICE", 0.0)
        
        findViewById<TextView>(R.id.tvHeaderFrom).text = fromLoc
        findViewById<TextView>(R.id.tvHeaderTo).text = toLoc
        
        val totalStr = if (totalPrice % 1.0 == 0.0) "%.0f".format(Locale.US, totalPrice) else "%.2f".format(Locale.US, totalPrice)
        findViewById<TextView>(R.id.tvTotalFare).text = "LKR $totalStr"
        
        val user = SupabaseProvider.client.auth.currentUserOrNull()
        findViewById<TextView>(R.id.tvHeaderUser).text = "Hello ${user?.email}!"

        tvSelectedBoarding = findViewById(R.id.tvSelectedBoarding)
        tvSelectedDrop = findViewById(R.id.tvSelectedDrop)
        
        loadStopsForTrip(tripId)

        findViewById<android.view.View>(R.id.layoutBoarding).setOnClickListener {
            showStopDialog("Select Boarding") { stop ->
                selectedBoardingId = stop.id
                tvSelectedBoarding.text = stop.locationName
            }
        }
        
        findViewById<android.view.View>(R.id.layoutDrop).setOnClickListener {
            showStopDialog("Select Drop-off") { stop ->
                selectedDropId = stop.id
                tvSelectedDrop.text = stop.locationName
            }
        }

        findViewById<android.view.View>(R.id.btnNext).setOnClickListener {
            if(selectedBoardingId == -1 || selectedDropId == -1) {
                Toast.makeText(this, "Please select both points", Toast.LENGTH_SHORT).show()
            } else {
                val next = Intent(this, GuestDetailsActivity::class.java)
                next.putExtras(intent)
                next.putExtra("PICKUP_STOP_ID", selectedBoardingId)
                next.putExtra("DROPOFF_STOP_ID", selectedDropId)
                startActivity(next)
            }
        }
        
        findViewById<android.view.View>(R.id.btnBack).setOnClickListener { finish() }

        // --- FIX: Home Icon Click ---
        findViewById<android.view.View>(R.id.navHome).setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }

    private fun loadStopsForTrip(tripId: Long) = lifecycleScope.launch {
        try {
            val trip = withContext(Dispatchers.IO) { tripsService.getTripById(tripId) }
            if (trip != null) {
                availableStops = withContext(Dispatchers.IO) { routesStopService.getStopsByRouteId(trip.routeId) }
            }
        } catch (e: Exception) {
            Toast.makeText(this@BoardingDropActivity, "Error loading stops", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showStopDialog(title: String, onSelect: (RouteStop) -> Unit) {
        if (availableStops.isEmpty()) {
            Toast.makeText(this, "Loading stops...", Toast.LENGTH_SHORT).show()
            return
        }
        val names = availableStops.map { it.locationName }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(names) { _, which ->
                onSelect(availableStops[which])
            }
            .show()
    }
}