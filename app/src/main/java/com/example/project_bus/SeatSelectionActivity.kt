package com.example.project_bus

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project_bus.data.SupabaseProvider
import com.example.project_bus.data.services.BookingPassengersService
import com.example.project_bus.data.services.BookingsService
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class SeatSelectionActivity : AppCompatActivity() {

    private lateinit var rvSeats: RecyclerView
    private lateinit var btnConfirmSeat: Button
    private lateinit var adapter: SeatAdapter
    
    private lateinit var btnLowerDeck: TextView
    private lateinit var btnUpperDeck: TextView
    
    private var currentDeck = "LOWER" 

    private val seatListLower = ArrayList<Seat>()
    private val seatListUpper = ArrayList<Seat>()
    
    private val selectedSeats = ArrayList<String>()

    private var ticketPrice: Double = 0.0
    private var tripId: Long = -1
    private var operatorName: String = ""
    private var busType: String = ""
    private var fromLoc: String = ""
    private var toLoc: String = ""
    private var dateStr: String = ""

    private val bookingsService = BookingsService()
    private val passengersService = BookingPassengersService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seat_selection)

        tripId = intent.getLongExtra("TRIP_ID", -1)
        ticketPrice = intent.getDoubleExtra("PRICE", 0.0)
        operatorName = intent.getStringExtra("OPERATOR") ?: "Unknown"
        busType = intent.getStringExtra("BUS_TYPE") ?: "Standard"
        fromLoc = intent.getStringExtra("FROM_LOC") ?: "Start"
        toLoc = intent.getStringExtra("TO_LOC") ?: "End"
        dateStr = intent.getStringExtra("DATE") ?: "Date"

        findViewById<TextView>(R.id.tvRouteFrom).text = fromLoc
        findViewById<TextView>(R.id.tvRouteTo).text = toLoc
        findViewById<TextView>(R.id.tvRouteDate).text = dateStr
        findViewById<TextView>(R.id.tvOperator).text = operatorName
        findViewById<TextView>(R.id.tvBusType).text = busType
        
        // CHANGED: Formatting
        val priceStr = if (ticketPrice % 1.0 == 0.0) "%.0f".format(Locale.US, ticketPrice) else "%.2f".format(Locale.US, ticketPrice)
        findViewById<TextView>(R.id.tvTicketPrice).text = "LKR $priceStr"

        val userEmail = SupabaseProvider.client.auth.currentUserOrNull()?.email ?: "User"
        findViewById<TextView>(R.id.tvHeaderName).text = "Hello $userEmail!"

        rvSeats = findViewById(R.id.rvSeats)
        btnConfirmSeat = findViewById(R.id.btnConfirmSeat)
        btnLowerDeck = findViewById(R.id.btnLowerDeck)
        btnUpperDeck = findViewById(R.id.btnUpperDeck)
        
        findViewById<CardView>(R.id.btnBack).setOnClickListener { finish() }

        // --- FIX: Home Icon Click ---
        findViewById<android.view.View>(R.id.navHome).setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        initEmptySeats()
        setupRecyclerView("LOWER")
        
        btnLowerDeck.setOnClickListener { switchDeck("LOWER") }
        btnUpperDeck.setOnClickListener { switchDeck("UPPER") }

        btnConfirmSeat.setOnClickListener {
            if (selectedSeats.isEmpty()) {
                Toast.makeText(this, "Please select at least one seat", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(this, BoardingDropActivity::class.java)
                intent.putExtra("TRIP_ID", tripId)
                intent.putStringArrayListExtra("SELECTED_SEATS", selectedSeats)
                intent.putExtra("TOTAL_PRICE", selectedSeats.size * ticketPrice)
                intent.putExtra("OPERATOR", operatorName)
                intent.putExtra("BUS_TYPE", busType)
                intent.putExtra("FROM_LOC", fromLoc)
                intent.putExtra("TO_LOC", toLoc)
                intent.putExtra("DATE", dateStr)
                startActivity(intent)
            }
        }

        loadBookedSeats()
    }

    private fun loadBookedSeats() = lifecycleScope.launch {
        try {
            val bookings = withContext(Dispatchers.IO) { 
                bookingsService.getBookingsByTripId(tripId).filter { it.bookingStatus == 1 }
            }
            val bookingIds = bookings.map { it.id }

            if (bookingIds.isNotEmpty()) {
                val passengers = withContext(Dispatchers.IO) {
                    passengersService.getPassengersByBookingIds(bookingIds)
                }
                val bookedSeatNumbers = passengers.mapNotNull { it.seatNumber }

                updateListStatus(seatListLower, bookedSeatNumbers)
                updateListStatus(seatListUpper, bookedSeatNumbers)
                
                adapter.notifyDataSetChanged()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this@SeatSelectionActivity, "Error loading seats: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateListStatus(list: ArrayList<Seat>, bookedNumbers: List<String>) {
        for (seat in list) {
            if (bookedNumbers.contains(seat.id)) {
                seat.status = 1 
            }
        }
    }

    private fun switchDeck(deck: String) {
        if (currentDeck == deck) return
        currentDeck = deck
        
        if (deck == "LOWER") {
            btnLowerDeck.setBackgroundResource(R.drawable.bg_outline_red)
            btnLowerDeck.setTextColor(ContextCompat.getColor(this, R.color.brand_red))
            btnUpperDeck.setBackgroundResource(R.drawable.bg_date_button)
            btnUpperDeck.setTextColor(ContextCompat.getColor(this, R.color.dot_gray))
        } else {
            btnUpperDeck.setBackgroundResource(R.drawable.bg_outline_red)
            btnUpperDeck.setTextColor(ContextCompat.getColor(this, R.color.brand_red))
            btnLowerDeck.setBackgroundResource(R.drawable.bg_date_button)
            btnLowerDeck.setTextColor(ContextCompat.getColor(this, R.color.dot_gray))
        }
        setupRecyclerView(deck)
    }

    private fun setupRecyclerView(deck: String) {
        val listToShow = if (deck == "LOWER") seatListLower else seatListUpper
        adapter = SeatAdapter(listToShow) { seat ->
            toggleSeat(seat)
        }
        rvSeats.layoutManager = GridLayoutManager(this, 5)
        rvSeats.adapter = adapter
    }

    private fun initEmptySeats() {
        var seatNum = 1
        for (row in 1..5) {
            for (col in 0..4) {
                if (col == 2) {
                    seatListLower.add(Seat("AISLE_L_$row", -1))
                } else {
                    val id = "L$seatNum"
                    seatListLower.add(Seat(id, 0)) 
                    seatNum++
                }
            }
        }
        
        seatNum = 1
        for (row in 1..5) {
            for (col in 0..4) {
                if (col == 2) {
                    seatListUpper.add(Seat("AISLE_U_$row", -1))
                } else {
                    val id = "U$seatNum"
                    seatListUpper.add(Seat(id, 0))
                    seatNum++
                }
            }
        }
    }

    private fun toggleSeat(seat: Seat) {
        if (seat.status == 0) {
            if (selectedSeats.size >= 5) {
                Toast.makeText(this, "Max 5 seats allowed", Toast.LENGTH_SHORT).show()
                return
            }
            seat.status = 2
            selectedSeats.add(seat.id)
        } else if (seat.status == 2) {
            seat.status = 0
            selectedSeats.remove(seat.id)
        }
        adapter.notifyDataSetChanged()
        
        val total = selectedSeats.size * ticketPrice
        if (selectedSeats.isEmpty()) {
            btnConfirmSeat.text = "Select a seat"
        } else {
            val s = selectedSeats.joinToString(",")
            // CHANGED: Formatting
            val totalStr = if (total % 1.0 == 0.0) "%.0f".format(Locale.US, total) else "%.2f".format(Locale.US, total)
            btnConfirmSeat.text = "Book $s (LKR $totalStr)"
        }
    }
}