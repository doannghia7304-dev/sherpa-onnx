package com.example.voicelockscreen

import android.Manifest
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
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
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.k2fsa.sherpa.onnx.KeywordSpotter
import com.k2fsa.sherpa.onnx.OnlineStream
import kotlin.concurrent.thread
import kotlin.math.sqrt

class VoiceLockActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences

    private lateinit var tvSayPassphrase: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvLiveResult: TextView
    private lateinit var btnFallbackPin: Button
    private lateinit var pbMicLevel: ProgressBar
    private lateinit var tvMicLevel: TextView

    private var kws: KeywordSpotter? = null
    private var stream: OnlineStream? = null
    private var audioRecord: AudioRecord? = null

    private var recordingThread: Thread? = null

    @Volatile
    private var isRecording: Boolean = false

    private val sampleRateInHz = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private var targetPassphrase: String = "MỞ KHÓA"
    private var savedPin: String = "1234"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupLockOverlayFlags()

        setContentView(R.layout.activity_voice_lock)

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        targetPassphrase = prefs.getString(PREF_KEY_PASSPHRASE, DEFAULT_PASSPHRASE)?.uppercase() ?: DEFAULT_PASSPHRASE
        savedPin = prefs.getString(PREF_KEY_PIN, DEFAULT_PIN) ?: DEFAULT_PIN

        tvSayPassphrase = findViewById(R.id.tv_say_passphrase)
        tvStatus = findViewById(R.id.tv_status)
        tvLiveResult = findViewById(R.id.tv_live_result)
        btnFallbackPin = findViewById(R.id.btn_fallback_pin)
        pbMicLevel = findViewById(R.id.pb_mic_level)
        tvMicLevel = findViewById(R.id.tv_mic_level)

        tvSayPassphrase.text = "Nói khẩu lệnh: \"$targetPassphrase\""

        setupBackPressedBlocking()

        btnFallbackPin.setOnClickListener {
            showPinFallbackDialog()
        }

        checkPermissionAndStartEngine()
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
                    this@VoiceLockActivity,
                    R.string.msg_say_passphrase_or_pin,
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun checkPermissionAndStartEngine() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            tvStatus.text = getString(R.string.err_no_record_audio_perm)
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO_CODE
            )
        } else {
            initVoiceEngineAndStartRecording()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initVoiceEngineAndStartRecording()
            } else {
                tvStatus.text = getString(R.string.err_no_record_audio_perm)
                Toast.makeText(this, R.string.msg_audio_perm_denied, Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun initVoiceEngineAndStartRecording() {
        try {
            val hasModel = ModelManager.isModelInAssets(this)
            if (!hasModel) {
                tvStatus.text = getString(R.string.err_no_model_assets)
                Toast.makeText(this, R.string.msg_check_model_assets, Toast.LENGTH_LONG).show()
                return
            }

            val config = ModelManager.getKeywordSpotterConfig(this, targetPassphrase)
            kws = KeywordSpotter(assetManager = application.assets, config = config)
            val localKws = kws ?: return

            stream = localKws.createStream("")
            val localStream = stream

            if (localStream == null || localStream.ptr == 0L) {
                Log.e(TAG, "Failed to create KWS stream")
                tvStatus.text = getString(R.string.err_init_stream_failed)
                return
            }

            val minBufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat)
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRateInHz,
                channelConfig,
                audioFormat,
                minBufferSize * 4
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize with VOICE_RECOGNITION source, trying MIC fallback")
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRateInHz,
                    channelConfig,
                    audioFormat,
                    minBufferSize * 4
                )
            }

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize")
                tvStatus.text = getString(R.string.err_init_mic_failed)
                return
            }

            audioRecord?.startRecording()
            isRecording = true

            recordingThread = thread(true) {
                processAudioSamples(localKws, localStream)
            }

            tvStatus.text = "Sherpa-ONNX KWS: Đang lắng nghe từ Micro..."
            Log.i(TAG, "Started voice recognition successfully")

        } catch (e: Throwable) {
            Log.e(TAG, "Failed to initialize voice engine", e)
            tvStatus.text = getString(R.string.err_init_stream_failed) + ": " + (e.localizedMessage ?: "")
        }
    }

    private fun processAudioSamples(localKws: KeywordSpotter, localStream: OnlineStream) {
        val interval = 0.1 // 100ms
        val bufferSize = (interval * sampleRateInHz).toInt()
        val buffer = ShortArray(bufferSize)

        while (isRecording) {
            val ret = audioRecord?.read(buffer, 0, buffer.size) ?: 0
            if (ret > 0) {
                val rms = calculateRMS(buffer, ret)

                runOnUiThread {
                    pbMicLevel.progress = rms.toInt().coerceIn(0, 5000)
                    tvMicLevel.text = "Âm lượng Micro: ${rms.toInt()}"
                }

                val samples = FloatArray(ret) { buffer[it] / 32768.0f }
                localStream.acceptWaveform(samples, sampleRate = sampleRateInHz)

                while (localKws.isReady(localStream)) {
                    localKws.decode(localStream)
                    val result = localKws.getResult(localStream)
                    val keyword = result.keyword

                    if (keyword.isNotBlank()) {
                        localKws.reset(localStream)
                        Log.i(TAG, "Sherpa-ONNX detected keyword: $keyword")

                        runOnUiThread {
                            checkPassphraseAndUnlock(keyword)
                        }
                    }
                }
            }
        }
        Log.i(TAG, "Audio recording loop ended")
    }

    private fun calculateRMS(buffer: ShortArray, readSize: Int): Double {
        var sum = 0.0
        for (i in 0 until readSize) {
            sum += (buffer[i] * buffer[i]).toDouble()
        }
        val mean = sum / readSize
        return sqrt(mean)
    }

    private fun checkPassphraseAndUnlock(detectedKeyword: String) {
        Log.i(TAG, "Comparing detectedKeyword '$detectedKeyword' with targetPassphrase '$targetPassphrase'")

        val isMatch = StringUtils.isPassphraseMatch(detectedKeyword, targetPassphrase)

        if (isMatch) {
            tvLiveResult.text = "Từ nhận diện: \"$detectedKeyword\" (Khớp mật khẩu!)"
            tvStatus.text = "MẬT KHẨU ĐÚNG: MỞ KHÓA THÀNH CÔNG!"
            unlockSuccess()
        } else {
            tvLiveResult.text = "Từ nhận diện: \"$detectedKeyword\" (Cần nói '$targetPassphrase')"
            tvStatus.text = "MẬT KHẨU SAI - KHÔNG THỂ MỞ KHÓA!"
            vibrateDevice(150)
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
        vibrateDevice(100)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        }

        finish()
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
        Log.i(TAG, "VoiceLockActivity onDestroy releasing resources")

        isRecording = false

        audioRecord?.let {
            try {
                if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    it.stop()
                }
                it.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing AudioRecord", e)
            }
        }
        audioRecord = null

        stream?.release()
        stream = null

        kws?.release()
        kws = null
    }

    companion object {
        private const val TAG = "VoiceLockActivity"
        private const val PREFS_NAME = "VoiceLockPrefs"
        private const val PREF_KEY_PASSPHRASE = "passphrase"
        private const val PREF_KEY_PIN = "pin"
        private const val DEFAULT_PASSPHRASE = "MỞ KHÓA"
        private const val DEFAULT_PIN = "1234"
        private const val REQUEST_RECORD_AUDIO_CODE = 201
    }
}
