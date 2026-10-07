package com.example.voicelockscreen

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class NativeSpeechRecognizerDemoActivity : AppCompatActivity() {
    private var speechRecognizer: SpeechRecognizer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_native_stt_demo)

        val tvStatus = findViewById<TextView>(R.id.tv_stt_status)
        val tvResult = findViewById<TextView>(R.id.tv_stt_result)
        val btnListen = findViewById<Button>(R.id.btn_stt_listen)
        val btnClose = findViewById<Button>(R.id.btn_close_stt)

        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        }

        btnListen.setOnClickListener {
            if (speechRecognizer == null) {
                Toast.makeText(this, "SpeechRecognizer không khả dụng", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            }

            tvStatus.text = "Android SpeechRecognizer đang lắng nghe..."
            tvResult.text = "Hãy nói mật khẩu..."

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    tvStatus.text = "Đang xử lý kết quả nhận diện..."
                }
                override fun onError(error: Int) {
                    tvStatus.text = "Lỗi nhận diện (Error code: $error)"
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val spokenText = matches?.firstOrNull() ?: ""
                    tvResult.text = "Kết quả: \"$spokenText\""
                    tvStatus.text = "Khớp từ khóa bằng Text Matching!"
                    Toast.makeText(this@NativeSpeechRecognizerDemoActivity, "STT Match: Mở khóa!", Toast.LENGTH_SHORT).show()
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            speechRecognizer?.startListening(intent)
        }

        btnClose.setOnClickListener { finish() }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
    }
}
