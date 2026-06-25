package com.example.assistivepointer

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import         android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.app.NotificationCompat

class FloatingPointerService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private var isWidgetLocked = false
    private var scaleX = 1.0f
    private var scaleY = 1.0f

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startCustomForeground()
        setupFloatingWindow()
    }

    private fun startCustomForeground() {
        val channelId = "pointer_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Pointer Background Service", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Pointer Active")
            .setContentText("Assistive overlay tracking system is running.")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build()

        startForeground(101, notification)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupFloatingWindow() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_pointer_widget, null)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 200
        params.y = 200

        val pointerDot = floatingView.findViewById<View>(R.id.pointer_dot)
        val btnGear = floatingView.findViewById<ImageView>(R.id.btn_gear)
        val btnLock = floatingView.findViewById<ImageView>(R.id.btn_lock)
        val settingsPanel = floatingView.findViewById<LinearLayout>(R.id.settings_panel)
        val seekX = floatingView.findViewById<SeekBar>(R.id.seek_x)
        val seekY = floatingView.findViewById<SeekBar>(R.id.seek_y)
        val labelX = floatingView.findViewById<TextView>(R.id.label_x)
        val labelY = floatingView.findViewById<TextView>(R.id.label_y)

        btnLock.setOnClickListener {
            isWidgetLocked = !isWidgetLocked
            if (isWidgetLocked) {
                btnLock.setImageResource(android.R.drawable.ic_lock_lock)
            } else {
                btnLock.setImageResource(android.R.drawable.ic_lock_idle_lock)
            }
        }

        btnGear.setOnClickListener {
            if (settingsPanel.visibility == View.GONE) {
                settingsPanel.visibility = View.VISIBLE
            } else {
                settingsPanel.visibility = View.GONE
            }
        }

        seekX.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                scaleX = 1.0f + (progress / 10.0f)
                labelX.text = "Sensitivity X: ${String.format("%.1f", scaleX)}x"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekY.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                scaleY = 1.0f + (progress / 10.0f)
                labelY.text = "Sensitivity Y: ${String.format("%.1f", scaleY)}x"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        pointerDot.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = event.rawX - initialTouchX
                        val deltaY = event.rawY - initialTouchY

                        if (!isWidgetLocked) {
                            params.x = initialX + deltaX.toInt()
                            params.y = initialY + deltaY.toInt()
                        } else {
                            params.x = initialX + (deltaX * scaleX).toInt()
                            params.y = initialY + (deltaY * scaleY).toInt()
                        }
                        windowManager.updateViewLayout(floatingView, params)
                        return true
                    }
                }
                return false
            }
        })

        windowManager.addView(floatingView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingView.isInitialized) {
            windowManager.removeView(floatingView)
        }
    }
}
