package com.universalshot.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import kotlin.math.abs

class FloatingService : Service() {

    companion object {
        @Volatile
        var running = false
    }

    private lateinit var wm: WindowManager
    private var btn: TextView? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        running = true
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        startFg()
        addButton()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        running = false
        btn?.let {
            try {
                wm.removeView(it)
            } catch (e: Throwable) {
            }
        }
        btn = null
        super.onDestroy()
    }

    private fun startFg() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("shot", "Floating button", NotificationManager.IMPORTANCE_MIN)
        )
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val n = Notification.Builder(this, "shot")
            .setContentTitle("Universal Shot চালু আছে")
            .setContentText("ফ্লোটিং বাটন চালু")
            .setSmallIcon(R.drawable.ic_shot)
            .setContentIntent(pi)
            .build()
        startForeground(1, n)
    }

    private fun addButton() {
        val density = resources.displayMetrics.density
        val size = (56 * density).toInt()

        val tv = TextView(this).apply {
            text = "📸"
            textSize = 22f
            gravity = Gravity.CENTER
            alpha = 0.85f
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#CC222222"))
            }
        }

        val lp = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = (200 * density).toInt()
        }

        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false

        tv.setOnTouchListener { v, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = lp.x
                    startY = lp.y
                    touchX = e.rawX
                    touchY = e.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - touchX).toInt()
                    val dy = (e.rawY - touchY).toInt()
                    if (abs(dx) > 10 || abs(dy) > 10) moved = true
                    if (moved) {
                        lp.x = startX + dx
                        lp.y = startY + dy
                        wm.updateViewLayout(v, lp)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) takeShot(tv)
                    true
                }
                else -> false
            }
        }

        wm.addView(tv, lp)
        btn = tv
    }

    private fun takeShot(tv: View) {
        tv.visibility = View.INVISIBLE
        handler.postDelayed({
            ShotHelper.capture(this, false) {
                tv.visibility = View.VISIBLE
            }
        }, 400)
    }
}
