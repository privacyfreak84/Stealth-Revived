package com.cosmos.unreddit.util

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A small text log of network fallbacks, written next to the crash logs in
 * `Android/data/<app id>/files/diagnostics.log` so a user can share it without adb.
 * It holds no post content, only which request failed and why. Kept under 64 KB.
 */
object DiagnosticLog {

    private const val FILE_NAME = "diagnostics.log"
    private const val MAX_BYTES = 64 * 1024

    @Volatile
    private var file: File? = null

    fun init(context: Context) {
        file = File(context.getExternalFilesDir(null) ?: context.filesDir, FILE_NAME)
    }

    @Synchronized
    fun write(message: String) {
        val target = file ?: return
        runCatching {
            if (target.length() > MAX_BYTES) {
                // Keep the newer half.
                val text = target.readText()
                target.writeText(text.substring(text.length / 2).substringAfter('\n'))
            }
            val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            target.appendText("$time  ${message.replace('\n', ' ').take(500)}\n")
        }
    }
}
