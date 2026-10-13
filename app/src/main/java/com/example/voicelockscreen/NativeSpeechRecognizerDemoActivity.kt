package com.example.voicelockscreen

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
import java.util.Locale

class NativeSpeechRecognizerDemoActivity : AppCompatActivity() {
    private var speechRecognizer: SpeechRecognizer? = null
    private var prefs: SharedPreferences? = null
    private var targetPassphrase: String = "MỞ KHÓA"
    private var savedPin: String = "1234"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupLockOverlayFlags()

        setContentView(R.layout.activity_native_stt_demo)

        prefs = getSharedPreferences("VoiceLockPrefs", Context.MODE_PRIVATE)
        targetPassphrase = prefs?.getString("passphrase", "MỞ KHÓA")?.uppercase() ?: "MỞ KHÓA"
        savedPin = prefs?.getString("pin", "1234") ?: "1234"

        val tvStatus = findViewById<TextView>(R.id.tv_stt_status)
        val tvResult = findViewById<TextView>(R.id.tv_stt_result)
        val btnListen = findViewById<Button>(R.id.btn_stt_listen)
        val btnFallbackPin = findViewById<Button>(R.id.btn_fallback_pin)

        tvResult.text = "Cần nói đúng mật khẩu: \"$targetPassphrase\""

        setupBackPressedBlocking()

        btnListen.setOnClickListener {
            startLiveVoiceRecognition(tvStatus, tvResult)
        }

        btnFallbackPin.setOnClickListener {
            showPinFallbackDialog()
        }

        startLiveVoiceRecognition(tvStatus, tvResult)
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
                    this@NativeSpeechRecognizerDemoActivity,
                    "Màn hình khóa Android STT đang hoạt động. Hãy nói khẩu lệnh!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun startLiveVoiceRecognition(tvStatus: TextView, tvResult: TextView) {
        if (speechRecognizer == null) {
            if (SpeechRecognizer.isRecognitionAvailable(this)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            } else {
                Toast.makeText(this, "SpeechRecognizer không khả dụng", Toast.LENGTH_SHORT).show()
                return
            }
        }

        try {
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            // ignore
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }

        tvStatus.text = "Android STT Lock: Đang mở Micro..."

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                tvStatus.text = "Màn hình đã khóa! Hãy nói khẩu lệnh \"$targetPassphrase\"..."
            }
            override fun onBeginningOfSpeech() {
                tvStatus.text = "Đang thu âm giọng nói..."
            }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                tvStatus.text = "Đang kiểm tra văn bản giọng nói với mật khẩu..."
            }
            override fun onError(error: Int) {
                Log.w("NativeSTTDemo", "SpeechRecognizer error: $error")
                when (error) {
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                        tvStatus.text = "Đang đặt lại Micro..."
                        speechRecognizer?.cancel()
                        window.decorView.postDelayed({ startLiveVoiceRecognition(tvStatus, tvResult) }, 300)
                    }
                    else -> {
                        tvStatus.text = "Chờ tín hiệu giọng nói (Mã $error). Hãy bấm nói lại!"
                    }
                }
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spokenText = matches?.firstOrNull()?.trim() ?: ""

                tvResult.text = "Giọng nói thu được: \"$spokenText\""

                val isMatch = StringUtils.isPassphraseMatch(spokenText, targetPassphrase)

                if (isMatch) {
                    tvStatus.text = "MẬT KHẨU ĐÚNG: MỞ KHÓA THÀNH CÔNG!"
                    unlockSuccess()
                } else {
                    tvStatus.text = "MẬT KHẨU SAI - KHÔNG THỂ MỞ KHÓA!\nBạn vừa nói: \"$spokenText\" (Cần nói: \"$targetPassphrase\")"
                    vibrateDevice(150)
                    Toast.makeText(this@NativeSpeechRecognizerDemoActivity, "Mật khẩu sai, không thể mở khóa!", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(intent)
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
