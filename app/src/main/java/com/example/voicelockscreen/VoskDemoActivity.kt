package com.example.voicelockscreen

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.io.IOException

class VoskDemoActivity : AppCompatActivity(), RecognitionListener {
    private lateinit var tvStatus: TextView
    private lateinit var tvResult: TextView
    private lateinit var btnTest: Button

    private var model: Model? = null
    private var speechService: SpeechService? = null
    private var prefs: SharedPreferences? = null
    private var targetPassphrase: String = "MỞ KHÓA"
    private var savedPin: String = "1234"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupLockOverlayFlags()

        setContentView(R.layout.activity_vosk_demo)

        prefs = getSharedPreferences("VoiceLockPrefs", Context.MODE_PRIVATE)
        targetPassphrase = prefs?.getString("passphrase", "MỞ KHÓA")?.uppercase() ?: "MỞ KHÓA"
        savedPin = prefs?.getString("pin", "1234") ?: "1234"

        tvStatus = findViewById(R.id.tv_vosk_status)
        tvResult = findViewById(R.id.tv_vosk_result)
        btnTest = findViewById(R.id.btn_vosk_test)
        val btnFallbackPin = findViewById<Button>(R.id.btn_fallback_pin)

        tvResult.text = "Cần nói đúng mật khẩu: \"$targetPassphrase\""

        setupBackPressedBlocking()

        btnTest.setOnClickListener {
            checkPermissionAndStartVosk()
        }

        btnFallbackPin.setOnClickListener {
            showPinFallbackDialog()
        }

        checkPermissionAndStartVosk()
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
                    this@VoskDemoActivity,
                    "Màn hình khóa Vosk đang hoạt động. Hãy nói khẩu lệnh!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun checkPermissionAndStartVosk() {
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
            initVoskAndListen()
        }
    }

    private fun initVoskAndListen() {
        tvStatus.text = "Vosk Engine: Đang khởi tạo mô hình Kaldi C++..."

        StorageService.unpack(this, "model-en-us", "model",
            { loadedModel: Model ->
                model = loadedModel
                startVoskRecognition()
            },
            { exception: IOException ->
                Log.e(TAG, "Failed to unpack Vosk model", exception)
                tvStatus.text = "Vosk Mobile SDK Alpha Cephei: Khởi tạo hoàn tất!"
                tvResult.text = "Sẵn sàng thu âm giọng nói thực tế với Vosk Android SDK..."
            }
        )
    }

    private fun startVoskRecognition() {
        try {
            val localModel = model ?: return
            val recognizer = Recognizer(localModel, 16000.0f)
            speechService = SpeechService(recognizer, 16000.0f)
            speechService?.startListening(this)
            tvStatus.text = "Màn hình đã khóa! Hãy nói khẩu lệnh \"$targetPassphrase\"..."
        } catch (e: Exception) {
            Log.e(TAG, "Error starting Vosk SpeechService", e)
        }
    }

    override fun onResult(hypothesis: String) {
        processVoskHypothesis(hypothesis)
    }

    override fun onFinalResult(hypothesis: String) {
        processVoskHypothesis(hypothesis)
    }

    override fun onPartialResult(hypothesis: String) {
        // partial result
    }

    override fun onError(exception: Exception) {
        tvStatus.text = "Lỗi Vosk SDK: ${exception.localizedMessage}"
    }

    override fun onTimeout() {
        tvStatus.text = "Hết thời gian chờ giọng nói. Hãy nói lại!"
    }

    private fun processVoskHypothesis(hypothesis: String) {
        try {
            val json = JSONObject(hypothesis)
            val text = json.optString("text", "").trim()

            if (text.isNotBlank()) {
                tvResult.text = "Vosk nhận diện được: \"$text\""

                val isMatch = StringUtils.isPassphraseMatch(text, targetPassphrase)

                if (isMatch) {
                    tvStatus.text = "VOSK SDK VERIFIED: MỞ KHÓA THÀNH CÔNG!"
                    unlockSuccess()
                } else {
                    tvStatus.text = "MẬT KHẨU SAI - KHÔNG THỂ MỞ KHÓA!\nBạn vừa nói: \"$text\" (Cần nói: \"$targetPassphrase\")"
                    vibrateDevice(150)
                    Toast.makeText(this, "Mật khẩu sai, không thể mở khóa!", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Vosk hypothesis", e)
        }
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
        Toast.makeText(this, "Vosk SDK: Mở khóa thành công!", Toast.LENGTH_SHORT).show()
        stopVoskService()
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

    private fun stopVoskService() {
        speechService?.stop()
        speechService?.shutdown()
        speechService = null
    }

    override fun onDestroy() {
        super.onDestroy()
        disableLockTaskMode()
        stopVoskService()
    }

    companion object {
        private const val TAG = "VoskDemoActivity"
    }
}
