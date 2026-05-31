package bin.mg.main

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File

class CrashHandler : AppCompatActivity() {

    private var crashLog: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.crash_handler)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        crashLog = intent.getStringExtra("crash_log") ?: "No crash log available"

        val textLog = findViewById<android.widget.TextView>(R.id.crash_log)
        textLog.text = crashLog

        findViewById<android.widget.Button>(R.id.btn_copy).setOnClickListener {
            copyToClipboard()
        }
        findViewById<android.widget.Button>(R.id.btn_share).setOnClickListener {
            shareCrashLog()
        }
        findViewById<android.widget.Button>(R.id.btn_restart).setOnClickListener {
            restartApp()
        }
    }

    private fun copyToClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("crash_log", crashLog)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Crash log copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun shareCrashLog() {
        try {
            val logDir = File(getExternalFilesDir(null), "crash_logs")
            val files = logDir.listFiles()
            if (files != null && files.isNotEmpty()) {
                val latestLog = files[files.size - 1]
                val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", latestLog)
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "text/plain"
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Crash Log")
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(Intent.createChooser(shareIntent, "Share crash log"))
            } else {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "text/plain"
                shareIntent.putExtra(Intent.EXTRA_TEXT, crashLog)
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Crash Log")
                startActivity(Intent.createChooser(shareIntent, "Share crash log"))
            }
        } catch (e: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_TEXT, crashLog)
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Crash Log")
            startActivity(Intent.createChooser(shareIntent, "Share crash log"))
        }
    }

    private fun restartApp() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }
}
