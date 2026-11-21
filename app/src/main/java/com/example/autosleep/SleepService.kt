package com.example.autosleep

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent

class SleepService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private val lockRunnable = Runnable { lockIfIdle() }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> restartTimer()
                Intent.ACTION_SCREEN_OFF -> handler.removeCallbacks(lockRunnable)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenReceiver, filter)
        restartTimer()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        when (event?.eventType) {
            AccessibilityEvent.TYPE_TOUCH_INTERACTION_START,
            AccessibilityEvent.TYPE_TOUCH_INTERACTION_END,
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_VIEW_LONG_CLICKED,
            AccessibilityEvent.TYPE_VIEW_SCROLLED,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> restartTimer()
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        handler.removeCallbacks(lockRunnable)
        try {
            unregisterReceiver(screenReceiver)
        } catch (_: Exception) {
        }
        super.onDestroy()
    }

    private fun prefs() = getSharedPreferences(PREFS, MODE_PRIVATE)

    private fun isEnabled() = prefs().getBoolean(KEY_ENABLED, true)

    private fun timeoutMillis(): Long {
        val seconds = prefs().getInt(KEY_SECONDS, DEFAULT_SECONDS)
        return seconds.coerceAtLeast(5) * 1000L
    }

    private fun restartTimer() {
        handler.removeCallbacks(lockRunnable)
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (isEnabled() && pm.isInteractive) {
            handler.postDelayed(lockRunnable, timeoutMillis())
        }
    }

    private fun lockIfIdle() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (isEnabled() && pm.isInteractive) {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        }
    }

    companion object {
        const val PREFS = "autosleep"
        const val KEY_SECONDS = "seconds"
        const val KEY_ENABLED = "enabled"
        const val DEFAULT_SECONDS = 60
    }
}
