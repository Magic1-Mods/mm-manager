package bin.mg.main.ui.editor

import android.app.Activity
import android.app.ProgressDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import bin.mg.main.R
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

class FileEditorActivity : AppCompatActivity() {

    private var drawerLayout: androidx.drawerlayout.widget.DrawerLayout? = null
    private var editText: LinedEditText? = null
    private var filenameText: android.widget.TextView? = null
    private var lineNoEncodingText: android.widget.TextView? = null
    private var symbolInput: android.widget.LinearLayout? = null
    private var drawerCurrentFilename: android.widget.TextView? = null
    private var drawerCurrentPath: android.widget.TextView? = null

    private var currentFilePath: String? = null
    private var isModified = false
    private var justSaved = false

    private val mHandler = Handler(Looper.getMainLooper()) {
        updateLineNumber()
        true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_editor)

        initViews()
        setupSymbolBar()
        setupClickListeners()

        currentFilePath = intent.getStringExtra("file_path")

        if (currentFilePath != null && File(currentFilePath!!).exists()) {
            loadFile()
        } else {
            currentFilePath = "/storage/emulated/0/convert.py"
            updateInfoBar()
        }
    }

    private fun initViews() {
        drawerLayout = findViewById(R.id.drawer_layout)
        editText = findViewById(R.id.smali_editor)
        filenameText = findViewById(R.id.textview_filename)
        lineNoEncodingText = findViewById(R.id.textview_lineno_encoding)
        symbolInput = findViewById(R.id.symbol_input)
        drawerCurrentFilename = findViewById(R.id.drawer_current_filename)
        drawerCurrentPath = findViewById(R.id.drawer_current_path)

        editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!justSaved) {
                    setModified(true)
                }
                justSaved = false
                mHandler.sendEmptyMessage(0)
            }
        })
    }

    private fun setupClickListeners() {
        findViewById<android.view.View>(R.id.btn_menu).setOnClickListener {
            drawerLayout?.openDrawer(GravityCompat.START)
        }
        findViewById<android.view.View>(R.id.btn_minimize).setOnClickListener {
            drawerLayout?.closeDrawer(GravityCompat.START)
        }
        findViewById<android.view.View>(R.id.btn_save).setOnClickListener {
            saveFile()
        }
        findViewById<android.view.View>(R.id.btn_undo).setOnClickListener {
            Toast.makeText(this, "Undo clicked", Toast.LENGTH_SHORT).show()
        }
        findViewById<android.view.View>(R.id.btn_redo).setOnClickListener {
            Toast.makeText(this, "Redo clicked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSymbolBar() {
        val symbols = arrayOf("→", "/", "+", "-", "*", "=", "<", ">", "(", ")", "[", "]", "{", "}", ":", "\"", "'", ";", "_")

        for (symbol in symbols) {
            val tv = android.widget.TextView(this)
            val insertText = if (symbol == "→") "\t" else symbol

            tv.text = symbol
            tv.textSize = 18f
            tv.setTextColor(0xFF212121.toInt())
            tv.setPadding(32, 16, 32, 16)
            tv.setOnClickListener {
                val start = maxOf(editText?.selectionStart ?: 0, 0)
                val end = maxOf(editText?.selectionEnd ?: 0, 0)
                editText?.text?.replace(minOf(start, end), maxOf(start, end), insertText)
            }
            symbolInput?.addView(tv)
        }
    }

    private fun updateInfoBar() {
        if (currentFilePath == null) return
        val file = File(currentFilePath!!)

        val prefix = if (isModified) "* " else ""
        filenameText?.text = prefix + file.name

        drawerCurrentFilename?.text = file.name
        drawerCurrentPath?.text = currentFilePath
    }

    private fun updateLineNumber() {
        if (editText == null || lineNoEncodingText == null) return

        var line = 1
        var col = 1
        val pos = editText?.selectionStart ?: 0
        val text = editText?.text?.toString() ?: ""

        if (pos > 0 && pos <= text.length) {
            val subText = text.substring(0, pos)
            val lines = subText.split("\n".toRegex(), limit = -1).toTypedArray()
            line = lines.size
            col = lines[lines.size - 1].length + 1
        }

        lineNoEncodingText?.text = "$line:$col   UTF-8"
    }

    private fun setModified(modified: Boolean) {
        isModified = modified
        updateInfoBar()
    }

    private fun loadFile() {
        if (currentFilePath == null) return
        LoadFileTask().execute(currentFilePath)
    }

    private fun saveFile() {
        if (currentFilePath == null) return
        SaveFileTask().execute(currentFilePath)
    }

    @Suppress("DEPRECATION")
    private inner class LoadFileTask : android.os.AsyncTask<String, Void, String>() {
        var dialog: ProgressDialog? = null

        override fun onPreExecute() {
            dialog = ProgressDialog.show(this@FileEditorActivity, "Loading", "Reading file...", true)
        }

        override fun doInBackground(vararg params: String?): String {
            val sb = StringBuilder()
            try {
                BufferedReader(FileReader(params[0])).use { br ->
                    var line: String?
                    while (br.readLine().also { line = it } != null) {
                        sb.append(line).append("\n")
                    }
                }
                return sb.toString()
            } catch (e: Exception) {
                return ""
            }
        }

        override fun onPostExecute(result: String?) {
            dialog?.takeIf { it.isShowing }?.dismiss()

            if (!result.isNullOrEmpty()) {
                editText?.setText(result)
                setModified(false)
                updateLineNumber()
            } else {
                Toast.makeText(this@FileEditorActivity, "Failed to load file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Suppress("DEPRECATION")
    private inner class SaveFileTask : android.os.AsyncTask<String, Void, Boolean>() {
        var dialog: ProgressDialog? = null

        override fun onPreExecute() {
            dialog = ProgressDialog.show(this@FileEditorActivity, "Saving", "Writing file...", true)
        }

        override fun doInBackground(vararg params: String?): Boolean {
            return try {
                val content = editText?.text?.toString() ?: ""
                Files.write(Paths.get(params[0]), content.toByteArray(StandardCharsets.UTF_8))
                true
            } catch (e: Exception) {
                false
            }
        }

        override fun onPostExecute(result: Boolean?) {
            dialog?.takeIf { it.isShowing }?.dismiss()

            if (result == true) {
                justSaved = true
                setModified(false)
                Toast.makeText(this@FileEditorActivity, "Saved", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@FileEditorActivity, "Save failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        @JvmStatic
        fun start(context: Activity, filePath: String) {
            val intent = android.content.Intent(context, FileEditorActivity::class.java)
            intent.putExtra("file_path", filePath)
            context.startActivity(intent)
        }
    }
}
