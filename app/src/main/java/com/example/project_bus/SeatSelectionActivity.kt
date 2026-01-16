package com.example.project_bus

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project_bus.data.SupabaseProvider
import io.github.jan.supabase.auth.auth

class SeatSelectionActivity : AppCompatActivity() {

    private lateinit var rvSeats: RecyclerView
    private lateinit var btnConfirmSeat: Button
    private lateinit var adapter: SeatAdapter
    
    // Nút chuyển tầng
    private lateinit var btnLowerDeck: TextView
    private lateinit var btnUpperDeck: TextView
    
    private var currentDeck = "LOWER" // Trạng thái hiện tại

    // Danh sách ghế riêng cho 2 tầng
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
        findViewById<TextView>(R.id.tvTicketPrice).text = "LKR ${ticketPrice.toInt()}"

        val userEmail = SupabaseProvider.client.auth.currentUserOrNull()?.email ?: "User"
        findViewById<TextView>(R.id.tvHeaderName).text = "Hello $userEmail!"

        rvSeats = findViewById(R.id.rvSeats)
        btnConfirmSeat = findViewById(R.id.btnConfirmSeat)
        btnLowerDeck = findViewById(R.id.btnLowerDeck)
        btnUpperDeck = findViewById(R.id.btnUpperDeck)
        
        findViewById<CardView>(R.id.btnBack).setOnClickListener { finish() }

        initSeatData()

        // Mặc định load tầng dưới
        setupRecyclerView("LOWER")
        
        // Sự kiện chuyển tầng
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
    }

    private fun switchDeck(deck: String) {
        if (currentDeck == deck) return
        currentDeck = deck
        
        // Đổi màu nút để biết đang chọn tầng nào
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

    private fun initSeatData() {
        // Tầng dưới: L1 -> L20
        var seatNum = 1
        for (row in 1..5) {
            for (col in 0..4) {
                if (col == 2) {
                    seatListLower.add(Seat("AISLE_L_$row", -1))
                } else {
                    val id = "L$seatNum"
                    val isBooked = Math.random() < 0.2
                    seatListLower.add(Seat(id, if(isBooked) 1 else 0))
                    seatNum++
                }
            }
        }
        
        // Tầng trên: U1 -> U20
        seatNum = 1
        for (row in 1..5) {
            for (col in 0..4) {
                if (col == 2) {
                    seatListUpper.add(Seat("AISLE_U_$row", -1))
                } else {
                    val id = "U$seatNum"
                    val isBooked = Math.random() < 0.2
                    seatListUpper.add(Seat(id, if(isBooked) 1 else 0))
                    seatNum++
                }
            }
        }
    }

    private fun toggleSeat(seat: Seat) {
        if (seat.status == 0) {
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
            btnConfirmSeat.text = "Book $s (LKR ${total.toInt()})"
        }
    }
}