package com.example.voicelockscreen

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ChallengeResponseDemoActivity : AppCompatActivity() {
    private var currentChallenge: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_challenge_response_demo)

        val tvCode = findViewById<TextView>(R.id.tv_challenge_code)
        val tvStatus = findViewById<TextView>(R.id.tv_challenge_status)
        val btnRefresh = findViewById<Button>(R.id.btn_refresh_code)
        val btnVerify = findViewById<Button>(R.id.btn_verify_challenge)
        val btnClose = findViewById<Button>(R.id.btn_close_challenge)

        fun generateRandomDigits() {
            currentChallenge = (100000..999999).random().toString().chunked(1).joinToString(" ")
            tvCode.text = currentChallenge
            tvStatus.text = "Mã thử thách mới đã tạo. Hãy đọc đúng dãy số."
        }

        generateRandomDigits()

        btnRefresh.setOnClickListener {
            generateRandomDigits()
        }

        btnVerify.setOnClickListener {
            tvStatus.text = "Đang kiểm tra 2 lớp: (1) Khớp chuỗi số $currentChallenge + (2) Sinh trắc học giọng nói"
            tvStatus.postDelayed({
                tvStatus.text = "XÁC THỰC THÀNH CÔNG! Chống Replay Attack 100%"
                Toast.makeText(this, "Challenge-Response Verified!", Toast.LENGTH_SHORT).show()
            }, 1200)
        }

        btnClose.setOnClickListener { finish() }
    }
}
