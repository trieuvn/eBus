package com.example.project_bus

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project_bus.data.SupabaseProvider
import io.github.jan.supabase.auth.auth

class SeatSelectionActivity : AppCompatActivity() {

    private lateinit var rvSeats: RecyclerView
    private lateinit var btnConfirmSeat: Button
    private lateinit var adapter: SeatAdapter

    private val seatList = ArrayList<Seat>()
    private val selectedSeats = ArrayList<String>()

    // --- KHAI BÁO BIẾN TOÀN CỤC ĐỂ LƯU DỮ LIỆU ---
    private var ticketPrice: Double = 0.0
    private var tripId: Long = -1
    private var operatorName: String = ""
    private var busType: String = ""
    private var fromLoc: String = ""
    private var toLoc: String = ""
    private var dateStr: String = ""
    // ----------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seat_selection)

        // 1. NHẬN DỮ LIỆU TỪ MÀN HÌNH BOOKING VÀ LƯU VÀO BIẾN
        tripId = intent.getLongExtra("TRIP_ID", -1)
        ticketPrice = intent.getDoubleExtra("PRICE", 0.0)
        operatorName = intent.getStringExtra("OPERATOR") ?: "Unknown Bus"
        busType = intent.getStringExtra("BUS_TYPE") ?: "Standard"
        fromLoc = intent.getStringExtra("FROM_LOC") ?: "Start"
        toLoc = intent.getStringExtra("TO_LOC") ?: "End"
        dateStr = intent.getStringExtra("DATE") ?: "Date"

        // 2. HIỂN THỊ LÊN GIAO DIỆN (Để người dùng biết mình đang đặt xe gì)
        findViewById<TextView>(R.id.tvRouteFrom).text = fromLoc
        findViewById<TextView>(R.id.tvRouteTo).text = toLoc
        findViewById<TextView>(R.id.tvRouteDate).text = dateStr
        findViewById<TextView>(R.id.tvOperator).text = operatorName
        findViewById<TextView>(R.id.tvBusType).text = busType
        findViewById<TextView>(R.id.tvTicketPrice).text = "LKR ${ticketPrice.toInt()}"

        // Hiển thị tên user
        val userEmail = SupabaseProvider.client.auth.currentUserOrNull()?.email ?: "User"
        findViewById<TextView>(R.id.tvHeaderName).text = "Hello $userEmail!"

        // 3. KHỞI TẠO CÁC VIEW
        rvSeats = findViewById(R.id.rvSeats)
        btnConfirmSeat = findViewById(R.id.btnConfirmSeat)
        findViewById<CardView>(R.id.btnBack).setOnClickListener { finish() }

        // 4. TẠO SƠ ĐỒ GHẾ (7 hàng x 5 cột)
        initSeatData()

        // 5. SETUP RECYCLERVIEW
        adapter = SeatAdapter(seatList) { seat ->
            toggleSeat(seat)
        }
        rvSeats.layoutManager = GridLayoutManager(this, 5)
        rvSeats.adapter = adapter

        // 6. XỬ LÝ NÚT XÁC NHẬN -> CHUYỂN SANG MÀN HÌNH BOARDING
        btnConfirmSeat.setOnClickListener {
            if (selectedSeats.isEmpty()) {
                Toast.makeText(this, "Please select at least one seat", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(this, BoardingDropActivity::class.java)

                // --- QUAN TRỌNG: GỬI TOÀN BỘ DỮ LIỆU SANG TRANG SAU ---
                intent.putExtra("TRIP_ID", tripId)
                intent.putStringArrayListExtra("SELECTED_SEATS", selectedSeats)
                intent.putExtra("TOTAL_PRICE", selectedSeats.size * ticketPrice)

                // Gửi tiếp thông tin hành trình để trang sau hiển thị
                intent.putExtra("OPERATOR", operatorName)
                intent.putExtra("BUS_TYPE", busType)
                intent.putExtra("FROM_LOC", fromLoc)
                intent.putExtra("TO_LOC", toLoc)
                intent.putExtra("DATE", dateStr)
                // -------------------------------------------------------

                startActivity(intent)
            }
        }
    }

    private fun initSeatData() {
        var seatNumber = 1
        for (row in 1..7) {
            for (col in 0..4) {
                if (col == 2) {
                    seatList.add(Seat("AISLE", -1)) // Lối đi
                } else {
                    val isBooked = Math.random() < 0.2 // 20% ghế đã đặt
                    val status = if (isBooked) 1 else 0
                    seatList.add(Seat("S$seatNumber", status))
                    seatNumber++
                }
            }
        }
    }

    private fun toggleSeat(seat: Seat) {
        if (seat.status == 0) {
            seat.status = 2 // Chọn
            selectedSeats.add(seat.id)
        } else if (seat.status == 2) {
            seat.status = 0 // Bỏ chọn
            selectedSeats.remove(seat.id)
        }

        adapter.notifyDataSetChanged()

        val total = selectedSeats.size * ticketPrice
        if (selectedSeats.isEmpty()) {
            btnConfirmSeat.text = "Select a seat"
        } else {
            btnConfirmSeat.text = "Proceed to Boarding (LKR ${total.toInt()})"
        }
    }
}