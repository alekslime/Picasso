package app.picasso.tracer.data

import android.content.Context
import android.content.Intent
import android.os.Build
import app.picasso.tracer.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Privacy-preserving on-device crash recorder.
 * Stores only the most recent uncaught exception in private app storage so users can
 * optionally share it when reporting a bug — nothing is ever uploaded automatically.
 */
class CrashLogger(private val crashFile: File) {

    fun install() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { record(thread.name, throwable) }
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun record(threadName: String, throwable: Throwable, nowMs: Long = System.currentTimeMillis()) {
        val report = formatReport(
            threadName = threadName,
            throwable = throwable,
            timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US).format(Date(nowMs)),
            appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            deviceInfo = "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})",
        )
        crashFile.parentFile?.mkdirs()
        crashFile.writeText(report)
    }

    fun hasReport(): Boolean = crashFile.exists() && crashFile.length() > 0L

    fun readReport(): String? = if (hasReport()) runCatching { crashFile.readText() }.getOrNull() else null

    fun clear() {
        runCatching { crashFile.delete() }
    }

    companion object {
        private const val FILE_NAME = "last_crash.txt"

        fun forContext(context: Context): CrashLogger =
            CrashLogger(File(context.filesDir, FILE_NAME))

        fun formatReport(
            threadName: String,
            throwable: Throwable,
            timestamp: String,
            appVersion: String,
            deviceInfo: String,
        ): String {
            val stack = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()
            return buildString {
                appendLine("Picasso Crash Report")
                appendLine("Time: $timestamp")
                appendLine("App: $appVersion")
                appendLine("Device: $deviceInfo")
                appendLine("Thread: $threadName")
                appendLine()
                append(stack)
            }
        }

        fun shareReport(context: Context, report: String): Boolean {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Picasso Crash Report")
                putExtra(Intent.EXTRA_TEXT, report)
            }
            return runCatching {
                context.startActivity(Intent.createChooser(intent, null))
                true
            }.getOrDefault(false)
        }
    }
}
