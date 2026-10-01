package com.example.voicelockscreen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.switchmaterial.SwitchMaterial

class MainActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences

    private lateinit var tvModelStatus: TextView
    private lateinit var switchService: SwitchMaterial
    private lateinit var etPassphrase: EditText
    private lateinit var etPin: EditText
    private lateinit var btnSave: Button
    private lateinit var btnGrantAudio: Button
    private lateinit var btnGrantOverlay: Button
    private lateinit var btnTestLock: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        tvModelStatus = findViewById(R.id.tv_model_status)
        switchService = findViewById(R.id.switch_service)
        etPassphrase = findViewById(R.id.et_passphrase)
        etPin = findViewById(R.id.et_pin)
        btnSave = findViewById(R.id.btn_save_settings)
        btnGrantAudio = findViewById(R.id.btn_grant_audio)
        btnGrantOverlay = findViewById(R.id.btn_grant_overlay)
        btnTestLock = findViewById(R.id.btn_test_lock)

        loadSettings()
        checkPermissionsAndStatus()

        btnSave.setOnClickListener {
            saveSettings()
        }

        switchService.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(PREF_KEY_SERVICE_ENABLED, isChecked).apply()
            if (isChecked) {
                startLockService()
            } else {
                stopLockService()
            }
        }

        btnGrantAudio.setOnClickListener {
            requestAudioPermission()
        }

        btnGrantOverlay.setOnClickListener {
            requestOverlayPermission()
        }

        btnTestLock.setOnClickListener {
            if (checkAudioPermission() && checkOverlayPermission()) {
                val intent = Intent(this, VoiceLockActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, R.string.msg_grant_perms_first, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadSettings() {
        val passphrase = prefs.getString(PREF_KEY_PASSPHRASE, DEFAULT_PASSPHRASE)
        val pin = prefs.getString(PREF_KEY_PIN, DEFAULT_PIN)
        val isEnabled = prefs.getBoolean(PREF_KEY_SERVICE_ENABLED, true)

        etPassphrase.setText(passphrase)
        etPin.setText(pin)
        switchService.isChecked = isEnabled

        val hasModel = ModelManager.isModelInAssets(this)
        if (hasModel) {
            tvModelStatus.text = getString(R.string.model_status_ok)
            tvModelStatus.setTextColor(ContextCompat.getColor(this, R.color.accent_green))
        } else {
            tvModelStatus.text = getString(R.string.model_status_missing)
            tvModelStatus.setTextColor(ContextCompat.getColor(this, R.color.accent_red))
        }
    }

    private fun saveSettings() {
        val newPassphrase = etPassphrase.text.toString().trim().uppercase()
        val newPin = etPin.text.toString().trim()

        if (newPassphrase.isEmpty()) {
            Toast.makeText(this, R.string.msg_enter_passphrase_prompt, Toast.LENGTH_SHORT).show()
            return
        }

        if (newPin.length < MIN_PIN_LENGTH) {
            Toast.makeText(this, R.string.msg_pin_length_prompt, Toast.LENGTH_SHORT).show()
            return
        }

        prefs.edit()
            .putString(PREF_KEY_PASSPHRASE, newPassphrase)
            .putString(PREF_KEY_PIN, newPin)
            .apply()

        Toast.makeText(this, R.string.msg_settings_saved, Toast.LENGTH_SHORT).show()
    }

    private fun startLockService() {
        val serviceIntent = Intent(this, LockScreenService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun stopLockService() {
        val serviceIntent = Intent(this, LockScreenService::class.java)
        stopService(serviceIntent)
    }

    private fun checkAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun checkPermissionsAndStatus() {
        if (!checkAudioPermission()) {
            requestAudioPermission()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIF_PERMISSION)
            }
        }
    }

    private fun requestAudioPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            REQUEST_AUDIO_PERMISSION
        )
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        } else {
            Toast.makeText(this, R.string.msg_overlay_perm_granted, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_AUDIO_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.msg_audio_perm_granted, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, R.string.msg_audio_perm_denied, Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "VoiceLockPrefs"
        private const val PREF_KEY_PASSPHRASE = "passphrase"
        private const val PREF_KEY_PIN = "pin"
        private const val PREF_KEY_SERVICE_ENABLED = "service_enabled"
        private const val DEFAULT_PASSPHRASE = "OPEN PHONE"
        private const val DEFAULT_PIN = "1234"
        private const val MIN_PIN_LENGTH = 4
        private const val REQUEST_AUDIO_PERMISSION = 101
        private const val REQUEST_NOTIF_PERMISSION = 102
    }
}
