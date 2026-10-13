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
import android.util.Log
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.util.Locale

class ChallengeResponseDemoActivity : AppCompatActivity() {
    private lateinit var tvCode: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnRefresh: Button
    private lateinit var btnVerify: Button

    private var currentChallengeDigits: String = ""
    private var speechRecognizer: SpeechRecognizer? = null
    private var prefs: SharedPreferences? = null
    private var savedPin: String = "1234"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupLockOverlayFlags()

        setContentView(R.layout.activity_challenge_response_demo)

        prefs = getSharedPreferences("VoiceLockPrefs", Context.MODE_PRIVATE)
        savedPin = prefs?.getString("pin", "1234") ?: "1234"

        tvCode = findViewById(R.id.tv_challenge_code)
        tvStatus = findViewById(R.id.tv_challenge_status)
        btnRefresh = findViewById(R.id.btn_refresh_code)
        btnVerify = findViewById(R.id.btn_verify_challenge)
        val btnFallbackPin = findViewById<Button>(R.id.btn_fallback_pin)

        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        }

        generateRandomDigits()

        setupBackPressedBlocking()

        btnRefresh.setOnClickListener {
            generateRandomDigits()
        }

        btnVerify.setOnClickListener {
            checkPermissionAndStartChallengeListen()
        }

        btnFallbackPin.setOnClickListener {
            showPinFallbackDialog()
        }

        checkPermissionAndStartChallengeListen()
    }

    override fun onResume() {
        super.onResume()
        enableLockTaskMode()
    }

    private fun enableLockTaskMode() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                startLockTask()
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun disableLockTaskMode() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                stopLockTask()
            }
        } catch (e: Exception) {
            // ignore
        }
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
                    this@ChallengeResponseDemoActivity,
                    "Màn hình khóa Challenge-Response đang hoạt động. Hãy đọc mã OTP!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun generateRandomDigits() {
        currentChallengeDigits = (100000..999999).random().toString().chunked(1).joinToString(" ")
        tvCode.text = currentChallengeDigits
        tvStatus.text = "Sẵn sàng! Bấm 'Đọc & Xác Thực' và đọc đúng dãy số OTP trên."
    }

    private fun checkPermissionAndStartChallengeListen() {
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
            startChallengeLiveRecognition()
        }
    }

    private fun startChallengeLiveRecognition() {
        if (speechRecognizer == null) {
            Toast.makeText(this, "SpeechRecognizer không khả dụng", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }

        tvStatus.text = "Hãy đọc dãy số OTP: $currentChallengeDigits vào Micro..."

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                tvStatus.text = "Micro đã mở! Hãy đọc dãy số OTP..."
            }
            override fun onBeginningOfSpeech() {
                tvStatus.text = "Đang thu âm giọng nói đọc dãy số OTP..."
            }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                tvStatus.text = "Đang kiểm tra đối chiếu mã OTP $currentChallengeDigits..."
            }
            override fun onError(error: Int) {
                tvStatus.text = "Lỗi thu âm (Mã lỗi: $error). Hãy đọc lại mã OTP!"
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spokenText = matches?.firstOrNull()?.trim() ?: ""

                val rawChallenge = currentChallengeDigits.replace(" ", "")
                val rawSpokenDigits = convertSpokenTextToDigits(spokenText)

                Log.i("ChallengeDemo", "Raw challenge: '$rawChallenge', Spoken digits: '$rawSpokenDigits', Spoken raw: '$spokenText'")

                if (rawSpokenDigits == rawChallenge || rawSpokenDigits.contains(rawChallenge) || (spokenText.replace(" ", "").contains(rawChallenge))) {
                    tvStatus.text = "MÃ OTP XÁC THỰC THÀNH CÔNG!\nBạn đã đọc đúng mã: \"$spokenText\"\nTrạng thái: Trùng khớp mã OTP $currentChallengeDigits | Mở khóa thành công!"
                    unlockSuccess()
                } else {
                    tvStatus.text = "SAI MÃ OTP - KHÔNG THỂ MỞ KHÓA!\nBạn vừa đọc: \"$spokenText\" (Dãy số nhận diện: \"$rawSpokenDigits\")\nMã OTP yêu cầu: \"$currentChallengeDigits\""
                    vibrateDevice(150)
                    Toast.makeText(this@ChallengeResponseDemoActivity, "Mã OTP không đúng!", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(intent)
    }

    private fun convertSpokenTextToDigits(spokenText: String): String {
        var text = spokenText.lowercase().trim()
        text = text.replace("không", "0")
            .replace("khong", "0")
            .replace("một", "1")
            .replace("mot", "1")
            .replace("mốt", "1")
            .replace("hai", "2")
            .replace("ba", "3")
            .replace("bốn", "4")
            .replace("bon", "4")
            .replace("tư", "4")
            .replace("năm", "5")
            .replace("nam", "5")
            .replace("lăm", "5")
            .replace("sáu", "6")
            .replace("sau", "6")
            .replace("bảy", "7")
            .replace("bay", "7")
            .replace("tám", "8")
            .replace("tam", "8")
            .replace("chín", "9")
            .replace("chin", "9")

        return text.filter { it.isDigit() }
    }

    private fun showPinFallbackDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_pin_fallback, null)
        val etPin = dialogView.findViewById<EditText>(R.id.et_dialog_pin)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialogView.findViewById<Button>(R.id.btn_cancel_pin).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btn_confirm_pin).setOnClickListener {
            val enteredPin = etPin.text.toString().trim()
            if (enteredPin == savedPin) {
                dialog.dismiss()
                unlockSuccess()
            } else {
                Toast.makeText(this, R.string.pin_incorrect, Toast.LENGTH_SHORT).show()
                vibrateDevice(200)
            }
        }

        dialog.show()
    }

    private fun unlockSuccess() {
        disableLockTaskMode()
        vibrateDevice(100)
        dismissKeyguard()
        Toast.makeText(this, "Mở khóa thành công!", Toast.LENGTH_SHORT).show()
        finish()
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
        disableLockTaskMode()
        speechRecognizer?.destroy()
    }
}
