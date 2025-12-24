package com.example.project_bus
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.project_bus.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ID lấy từ activity_main.xml
        val btnGetStarted = findViewById<Button>(R.id.btnGetStarted)
        val tvCreateAccount = findViewById<TextView>(R.id.tvCreateAccount)


    }
}