package com.universalshot.app

import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import rikka.shizuku.Shizuku
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ShotHelper {
    private val main = Handler(Looper.getMainLooper())

    fun ready(): Boolean = try {
        Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (e: Throwable) {
        false
    }

    private fun toast(ctx: Context, msg: String) {
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
    }

    fun capture(ctx: Context, collapseShade: Boolean, done: ((Boolean) -> Unit)? = null) {
        val app = ctx.applicationContext
        if (!ready()) {
            toast(app, "Shizuku চালু নেই বা permission দেওয়া হয়নি")
            done?.invoke(false)
            return
        }
        Thread {
            var ok = false
            var err = ""
            try {
                val dir = "/sdcard/Pictures/UniversalShot"
                val name = "shot_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".png"
                val path = "$dir/$name"
                val cmd = StringBuilder("mkdir -p $dir; ")
                if (collapseShade) cmd.append("cmd statusbar collapse; sleep 1; ")
                cmd.append("screencap -p $path")

                val m = Shizuku::class.java.getDeclaredMethod(
                    "newProcess",
                    Array<String>::class.java,
                    Array<String>::class.java,
                    String::class.java
                )
                m.isAccessible = true
                val p = m.invoke(null, arrayOf("sh", "-c", cmd.toString()), null, null) as Process
                err = p.errorStream.bufferedReader().readText()
                ok = p.waitFor() == 0
                if (ok) {
                    MediaScannerConnection.scanFile(app, arrayOf(path), arrayOf("image/png"), null)
                }
            } catch (e: Throwable) {
                err = e.toString()
            }
            main.post {
                toast(app, if (ok) "স্ক্রিনশট সেভ হয়েছে" else "Error: $err")
                done?.invoke(ok)
            }
        }.start()
    }
}
