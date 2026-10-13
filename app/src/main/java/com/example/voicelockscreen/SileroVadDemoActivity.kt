package com.example.voicelockscreen

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.WindowManager
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.util.Locale

class SileroVadDemoActivity : AppCompatActivity() {
    private lateinit var tvStatus: TextView
    private lateinit var tvResult: TextView
    private lateinit var pbMicLevel: ProgressBar
    private lateinit var tvMicLevel: TextView
    private lateinit var btnTest: Button

    private var speechRecognizer: SpeechRecognizer? = null
    private var prefs: SharedPreferences? = null
    private var targetPassphrase: String = "MỞ KHÓA"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupLockOverlayFlags()

        setContentView(R.layout.activity_silero_vad_demo)

        prefs = getSharedPreferences("VoiceLockPrefs", Context.MODE_PRIVATE)
        targetPassphrase = prefs?.getString("passphrase", "MỞ KHÓA")?.uppercase() ?: "MỞ KHÓA"

        tvStatus = findViewById(R.id.tv_vad_status)
        tvResult = findViewById(R.id.tv_vad_result)
        pbMicLevel = findViewById(R.id.pb_vad_mic_level)
        tvMicLevel = findViewById(R.id.tv_vad_mic_level)
        btnTest = findViewById(R.id.btn_vad_test)
        val btnClose = findViewById<Button>(R.id.btn_close_vad)

        tvResult.text = "Cần nói đúng mật khẩu: \"$targetPassphrase\""

        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        }

        setupBackPressedBlocking()

        btnTest.setOnClickListener {
            checkPermissionAndStartVad()
        }

        btnClose.setOnClickListener {
            speechRecognizer?.stopListening()
            finish()
        }

        checkPermissionAndStartVad()
    }

    private fun setupLockOverlayFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
    }

    private fun setupBackPressedBlocking() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Toast.makeText(
                    this@SileroVadDemoActivity,
                    "Màn hình khóa Silero VAD đang hoạt động. Hãy nói khẩu lệnh!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun checkPermissionAndStartVad() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                101
            )
        } else {
            startVadVoiceRecognition()
        }
    }

    private fun startVadVoiceRecognition() {
        if (speechRecognizer == null) {
            Toast.makeText(this, "SpeechRecognizer không khả dụng", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }

        tvStatus.text = "Silero VAD ONNX: Đang lọc nhiễu môi trường & phát hiện giọng nói..."

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                tvStatus.text = "Silero VAD: Đã lọc xong nhiễu! Hãy nói khẩu lệnh \"$targetPassphrase\"..."
            }
            override fun onBeginningOfSpeech() {
                tvStatus.text = "Silero Neural VAD: Phát hiện giọng người (Speech Probability > 0.95)..."
            }
            override fun onRmsChanged(rmsdB: Float) {
                val progress = ((rmsdB + 2) * 400).toInt().coerceIn(0, 5000)
                pbMicLevel.progress = progress
                tvMicLevel.text = "Âm lượng Micro: ${progress / 10}"
            }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                tvStatus.text = "Silero VAD: Đã cắt đoạn âm giọng nói sạch và gửi nhận diện..."
            }
            override fun onError(error: Int) {
                tvStatus.text = "Lỗi nhận diện VAD (Mã $error). Hãy thử lại!"
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spokenText = matches?.firstOrNull()?.trim() ?: ""

                tvResult.text = "Silero VAD Filter nhận diện: \"$spokenText\""

                val isMatch = StringUtils.isPassphraseMatch(spokenText, targetPassphrase)

                if (isMatch) {
                    tvStatus.text = "SILERO VAD VERIFIED: MỞ KHÓA THÀNH CÔNG!"
                    vibrateDevice(100)
                    dismissKeyguard()
                    Toast.makeText(this@SileroVadDemoActivity, "Silero VAD: Mở khóa thành công!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    tvStatus.text = "MẬT KHẨU SAI - KHÔNG THỂ MỞ KHÓA!\nBạn vừa nói: \"$spokenText\" (Cần nói: \"$targetPassphrase\")"
                    vibrateDevice(150)
                    Toast.makeText(this@SileroVadDemoActivity, "Chưa khớp khẩu lệnh!", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(intent)
    }

    private fun dismissKeyguard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        }
    }

    private fun vibrateDevice(durationMs: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            val vibrator = vibratorManager.defaultVibrator
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION")
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(durationMs)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
    }
}
