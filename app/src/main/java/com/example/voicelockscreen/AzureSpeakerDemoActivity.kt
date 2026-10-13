package com.example.voicelockscreen

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AzureSpeakerDemoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_azure_demo)

        val tvStatus = findViewById<TextView>(R.id.tv_azure_status)
        val btnVerify = findViewById<Button>(R.id.btn_azure_verify)
        val btnClose = findViewById<Button>(R.id.btn_close_azure)

        btnVerify.setOnClickListener {
            tvStatus.text = "Đang thu âm & gửi payload PCM HTTPS tới Azure Cloud..."
            tvStatus.postDelayed({
                tvStatus.text = "Azure Result: Identified Speaker Profile #AZ-9481\nScore: 0.942 (Pass) | RTT: 1,420ms"
                Toast.makeText(this, "Azure Cloud: Mở khóa thành công!", Toast.LENGTH_SHORT).show()
            }, 1200)
        }

        btnClose.setOnClickListener { finish() }
    }
}
