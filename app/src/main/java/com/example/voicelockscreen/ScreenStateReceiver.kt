package com.example.voicelockscreen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ScreenStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.i("ScreenStateReceiver", "Received action: $action")

        val prefs = context.getSharedPreferences("VoiceLockPrefs", Context.MODE_PRIVATE)
        val isServiceEnabled = prefs.getBoolean("service_enabled", true)

        if (!isServiceEnabled) return

        if (action == Intent.ACTION_SCREEN_ON) {
            Log.i("ScreenStateReceiver", "Screen turned ON. Launching VoiceLockActivity overlay.")
            val lockIntent = Intent(context, VoiceLockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            context.startActivity(lockIntent)
        }
    }
}
