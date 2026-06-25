package com.example.assistivepointer

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val overlayPermissionRequestCode = 5469
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(32, 32, 32, 32)
        }

        statusText = TextView(this).apply {
            text = "Checking Overlays Permission..."
            textSize = 18f
            setPadding(0, 0, 0, 32)
        }
        rootLayout.addView(statusText)

        val btnPermission = Button(this).apply {
            text = "Grant Window Overlay Permission"
            setOnClickListener { checkAndRequestOverlayPermission() }
        }
        rootLayout.addView(btnPermission)

        val btnStart = Button(this).apply {
            text = "Launch Floating Controller"
            setOnClickListener { startPointerService() }
        }
        rootLayout.addView(btnStart)

        val btnStop = Button(this).apply {
            text = "Kill Active Controller"
            setOnClickListener { stopService(Intent(this@MainActivity, FloatingPointerService::class.java)) }
        }
        rootLayout.addView(btnStop)

        setContentView(rootLayout)
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
            statusText.text = "Permission Status: GRANTED ✅"
        } else {
            statusText.text = "Permission Status: DENIED ❌"
        }
    }

    private fun checkAndRequestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, overlayPermissionRequestCode)
        }
    }

    private fun startPointerService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
            val intent = Intent(this, FloatingPointerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }
}
