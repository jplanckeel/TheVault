package com.thevault

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

/** Android main entry point. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySystemBars(dark = false)
        askNotificationPermission()
        setContent { App(onDarkShell = ::applySystemBars) }
    }

    /** Wishlist alerts need the notification permission (Android 13+); asked once. */
    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        val prefs = getPreferences(MODE_PRIVATE)
        if (prefs.getBoolean(KEY_ASKED, false)) return
        prefs.edit().putBoolean(KEY_ASKED, true).apply()
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
    }

    private companion object {
        const val KEY_ASKED = "notifications.asked"
    }

    /** Light icons on « Black Edition », dark icons on « Coque grise ». */
    private fun applySystemBars(dark: Boolean) {
        val style =
            if (dark) {
                SystemBarStyle.dark(Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
            }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
}
