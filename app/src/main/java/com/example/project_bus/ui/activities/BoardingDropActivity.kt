package com.example.project_bus.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.project_bus.R

class BoardingDropActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_boarding_drop)

        // Load boarding & drop-off points by route
        // User must select both before continue
    }
}
