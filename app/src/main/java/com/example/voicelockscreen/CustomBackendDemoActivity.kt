package com.example.voicelockscreen

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CustomBackendDemoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_backend_demo)

        val tvStatus = findViewById<TextView>(R.id.tv_backend_status)
        val btnVerify = findViewById<Button>(R.id.btn_backend_verify)
        val btnClose = findViewById<Button>(R.id.btn_close_backend)

        btnVerify.setOnClickListener {
            tvStatus.text = "Đang gửi Multipart Audio tới FastAPI (PyTorch ResNet50)..."
            tvStatus.postDelayed({
                tvStatus.text = "FastAPI Response 200 OK\nSpeaker Similarity: 0.891\nServer Inference: 240ms | Network RTT: 650ms"
                Toast.makeText(this, "Custom GPU Backend: Mở khóa thành công!", Toast.LENGTH_SHORT).show()
            }, 1000)
        }

        btnClose.setOnClickListener { finish() }
    }
}
