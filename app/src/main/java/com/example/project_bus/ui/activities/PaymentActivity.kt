package com.example.project_bus.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.project_bus.R
import java.util.*

class PaymentActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        // Calculate total price
        // Apply 10% weekend surcharge if Saturday or Sunday
        // Mock payment success/failure
    }

    private fun isWeekend(date: Date): Boolean {
        val cal = Calendar.getInstance()
        cal.time = date
        return cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
               cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
    }
}
