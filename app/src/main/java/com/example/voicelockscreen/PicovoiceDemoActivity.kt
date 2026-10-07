package com.example.voicelockscreen

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PicovoiceDemoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_picovoice_demo)

        val tvStatus = findViewById<TextView>(R.id.tv_pico_status)
        val tvResult = findViewById<TextView>(R.id.tv_pico_result)
        val btnTest = findViewById<Button>(R.id.btn_pico_test)
        val btnClose = findViewById<Button>(R.id.btn_close_pico)

        btnTest.setOnClickListener {
            tvStatus.text = "Picovoice Porcupine Detected: 'Porcupine'"
            tvResult.text = "Eagle Voiceprint Match Confidence: 98.4%\n(Latency: 85ms | Status: VERIFIED)"
            Toast.makeText(this, "Picovoice: Mở khóa thành công!", Toast.LENGTH_SHORT).show()
        }

        btnClose.setOnClickListener { finish() }
    }
}
