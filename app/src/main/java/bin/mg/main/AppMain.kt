package bin.mg.main

import android.app.Application
import android.content.Intent
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppMain : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            handleCrash(t, e)
        }
    }

    private fun handleCrash(thread: Thread, throwable: Throwable) {
        Log.e(TAG, "CRASH: ${throwable.message}", throwable)

        val crashLog = StringBuilder()
        crashLog.append("=== CRASH LOG ===\n")
        crashLog.append("Time: ").append(SIMPLE_DATE_FORMAT.format(Date())).append("\n")
        crashLog.append("App: Magic Manager\n")
        crashLog.append("Version: ").append(BuildConfig.VERSION_NAME).append("\n")
        crashLog.append("Android: ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n")
        crashLog.append("Device: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\n\n")
        crashLog.append("Thread: ").append(thread.name).append("\n")
        crashLog.append("Exception: ").append(throwable.javaClass.name).append("\n")
        crashLog.append("Message: ").append(throwable.message).append("\n\n")

        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        crashLog.append("=== STACK TRACE ===\n")
        crashLog.append(sw.toString())

        crashLog.append("\n=== CAUSE ===\n")
        var cause = throwable.cause
        while (cause != null) {
            crashLog.append("Cause: ").append(cause.javaClass.name).append("\n")
            crashLog.append("Message: ").append(cause.message).append("\n\n")
            cause = cause.cause
        }

        try {
            val logDir = File(getExternalFilesDir(null), "crash_logs")
            if (!logDir.exists()) {
                logDir.mkdirs()
            }
            val fileName = "crash_${System.currentTimeMillis()}.txt"
            val logFile = File(logDir, fileName)
            val writer = FileWriter(logFile)
            writer.write(crashLog.toString())
            writer.close()
            Log.d(TAG, "Crash log saved: ${logFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save crash log", e)
        }

        val intent = Intent(this, CrashHandler::class.java)
        intent.putExtra("crash_log", crashLog.toString())
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)

        android.os.Process.killProcess(android.os.Process.myPid())
        System.exit(1)
    }

    companion object {
        private const val TAG = "AppMain"
        private val SIMPLE_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        @JvmStatic
        lateinit var instance: AppMain
            private set
    }
}
