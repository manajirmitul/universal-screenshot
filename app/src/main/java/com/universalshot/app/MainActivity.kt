package com.universalshot.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import rikka.shizuku.Shizuku

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var floatBtn: Button
    private val handler = Handler(Looper.getMainLooper())

    private val permListener = Shizuku.OnRequestPermissionResultListener { _, _ ->
        runOnUiThread { refresh() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dp = resources.displayMetrics.density
        val pad = (20 * dp).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
        }

        status = TextView(this).apply { textSize = 18f }
        root.addView(status)

        fun addBtn(label: String, onClick: () -> Unit): Button {
            val b = Button(this).apply {
                text = label
                setOnClickListener { onClick() }
            }
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (12 * dp).toInt() }
            root.addView(b, lp)
            return b
        }

        addBtn("1. Shizuku permission দাও") {
            try {
                if (!Shizuku.pingBinder()) {
                    toast("আগে Shizuku অ্যাপ চালু কর")
                } else if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                    toast("Permission আগেই দেওয়া আছে")
                } else {
                    Shizuku.requestPermission(100)
                }
            } catch (e: Throwable) {
                toast("Shizuku error: $e")
            }
        }

        addBtn("2. Overlay permission দাও") {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }

        floatBtn = addBtn("3. ফ্লোটিং বাটন চালু") {
            if (FloatingService.running) {
                stopService(Intent(this, FloatingService::class.java))
            } else if (!Settings.canDrawOverlays(this)) {
                toast("আগে Overlay permission দাও")
            } else {
                startForegroundService(Intent(this, FloatingService::class.java))
            }
            handler.postDelayed({ refresh() }, 500)
        }

        addBtn("টেস্ট স্ক্রিনশট (৩ সেকেন্ড পর)") {
            toast("৩ সেকেন্ডের মধ্যে অন্য স্ক্রিনে যা")
            handler.postDelayed({ ShotHelper.capture(this, false) }, 3000)
        }

        root.addView(
            TextView(this).apply {
                text = "\nQuick Settings টাইল: নোটিফিকেশন প্যানেলে এডিট (পেন্সিল) চেপে \"Screenshot\" টাইল যোগ কর।\n\nস্ক্রিনশট সেভ হয়: Pictures/UniversalShot"
                textSize = 14f
            }
        )

        setContentView(ScrollView(this).apply { addView(root) })

        Shizuku.addRequestPermissionResultListener(permListener)

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permListener)
        super.onDestroy()
    }

    private fun refresh() {
        val s = try {
            when {
                !Shizuku.pingBinder() -> "Shizuku চলছে না ❌"
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> "Shizuku ✅ permission আছে"
                else -> "Shizuku চলছে, permission দরকার ⚠️"
            }
        } catch (e: Throwable) {
            "Shizuku পাওয়া যায়নি ❌"
        }
        val o = if (Settings.canDrawOverlays(this)) "Overlay ✅" else "Overlay ❌"
        status.text = "$s\n$o"
        floatBtn.text = if (FloatingService.running) "3. ফ্লোটিং বাটন বন্ধ কর" else "3. ফ্লোটিং বাটন চালু"
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
}
