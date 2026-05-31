package bin.mg.main.ui.editor

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.Selection
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import bin.mg.main.R
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Stack
import java.util.regex.Pattern

class FileEditorActivity : AppCompatActivity() {

    private var editText: LinedEditText? = null
    private var filenameText: TextView? = null
    private var lineNoEncodingText: TextView? = null
    private var symbolInput: LinearLayout? = null
    private var searchBar: LinearLayout? = null
    private var searchInput: EditText? = null
    private var searchResultCount: TextView? = null

    private var currentFilePath: String? = null
    private var isModified = false
    private var justSaved = false

    private val undoStack = Stack<String>()
    private val redoStack = Stack<String>()
    private var lastText = ""
    private var isTrackingChanges = true

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
        setupSearch()

        currentFilePath = intent.getStringExtra("file_path")
        if (currentFilePath != null && File(currentFilePath!!).exists()) {
            loadFile()
        } else {
            updateInfoBar()
        }
    }

    private fun initViews() {
        editText = findViewById(R.id.smali_editor)
        filenameText = findViewById(R.id.textview_filename)
        lineNoEncodingText = findViewById(R.id.textview_lineno_encoding)
        symbolInput = findViewById(R.id.symbol_input)
        searchBar = findViewById(R.id.search_bar)
        searchInput = findViewById(R.id.search_input)
        searchResultCount = findViewById(R.id.search_result_count)

        editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!justSaved && isTrackingChanges) {
                    setModified(true)
                }
                justSaved = false
                mHandler.sendEmptyMessage(0)
            }
        })

        editText?.setOnKeyListener { _, keyCode, event ->
            if (keyCode == android.view.KeyEvent.KEYCODE_TAB) {
                val start = maxOf(editText?.selectionStart ?: 0, 0)
                val end = maxOf(editText?.selectionEnd ?: 0, 0)
                editText?.text?.replace(minOf(start, end), maxOf(start, end), "    ")
                true
            } else false
        }
    }

    private fun setupClickListeners() {
        findViewById<View>(R.id.btn_menu)?.setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.btn_save)?.setOnClickListener {
            saveFile()
        }

        findViewById<View>(R.id.btn_undo)?.setOnClickListener {
            undo()
        }

        findViewById<View>(R.id.btn_redo)?.setOnClickListener {
            redo()
        }

        findViewById<View>(R.id.btn_search)?.setOnClickListener {
            toggleSearch()
        }

        findViewById<View>(R.id.btn_edit_mode)?.setOnClickListener {
            Toast.makeText(this, "Read-only mode toggled", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.btn_overflow)?.setOnClickListener {
            showPreferences()
        }
    }

    private fun setupSearch() {
        findViewById<View>(R.id.btn_search_close)?.setOnClickListener {
            closeSearch()
        }

        findViewById<View>(R.id.btn_search_prev)?.setOnClickListener {
            searchPrevious()
        }

        findViewById<View>(R.id.btn_search_next)?.setOnClickListener {
            searchNext()
        }

        searchInput?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchNext()
                true
            } else false
        }
    }

    private fun toggleSearch() {
        if (searchBar?.visibility == View.VISIBLE) {
            closeSearch()
        } else {
            searchBar?.visibility = View.VISIBLE
            searchInput?.requestFocus()
        }
    }

    private fun closeSearch() {
        searchBar?.visibility = View.GONE
        searchInput?.text?.clear()
        searchResultCount?.text = ""
        clearSearchHighlights()
    }

    private fun searchNext() {
        val query = searchInput?.text?.toString() ?: return
        if (query.isEmpty()) return

        val text = editText?.text?.toString() ?: return
        val pattern = Pattern.compile(Pattern.quote(query), Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)

        var count = 0
        while (matcher.find()) count++
        searchResultCount?.text = "$count found"

        val cursorPos = editText?.selectionStart ?: 0
        val found = matcher.reset()
        var lastMatchStart = -1
        var lastMatchEnd = -1

        while (found.start() <= cursorPos && found.find()) {
            lastMatchStart = found.start()
            lastMatchEnd = found.end()
        }

        if (!found.find() && lastMatchStart >= 0) {
            found.reset()
            if (found.find()) {
                lastMatchStart = found.start()
                lastMatchEnd = found.end()
            }
        } else if (found.find()) {
            lastMatchStart = found.start()
            lastMatchEnd = found.end()
        }

        if (lastMatchStart >= 0) {
            editText?.let {
                Selection.setSelection(it.text, lastMatchStart, lastMatchEnd)
            }
        }
    }

    private fun searchPrevious() {
        val query = searchInput?.text?.toString() ?: return
        if (query.isEmpty()) return

        val text = editText?.text?.toString() ?: return
        val pattern = Pattern.compile(Pattern.quote(query), Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)

        val cursorPos = editText?.selectionStart ?: 0
        var prevStart = -1
        var prevEnd = -1

        while (matcher.find()) {
            if (matcher.start() >= cursorPos) break
            prevStart = matcher.start()
            prevEnd = matcher.end()
        }

        if (prevStart < 0) {
            matcher.reset()
            while (matcher.find()) {
                prevStart = matcher.start()
                prevEnd = matcher.end()
            }
        }

        if (prevStart >= 0) {
            editText?.let {
                Selection.setSelection(it.text, prevStart, prevEnd)
            }
        }
    }

    private fun clearSearchHighlights() {
        val text = editText?.text ?: return
        if (text is SpannableStringBuilder) {
            text.getSpans(0, text.length, android.text.style.ForegroundColorSpan::class.java)
                .forEach { text.removeSpan(it) }
        }
    }

    private fun undo() {
        if (undoStack.isEmpty()) return
        isTrackingChanges = false
        redoStack.push(editText?.text?.toString() ?: "")
        val previous = undoStack.pop()
        editText?.setText(previous)
        Selection.setSelection(editText?.text ?: return, previous.length.coerceAtMost(editText?.text?.length ?: 0))
        isTrackingChanges = true
        if (undoStack.isEmpty()) setModified(false)
    }

    private fun redo() {
        if (redoStack.isEmpty()) return
        isTrackingChanges = false
        undoStack.push(editText?.text?.toString() ?: "")
        val next = redoStack.pop()
        editText?.setText(next)
        Selection.setSelection(editText?.text ?: return, next.length.coerceAtMost(editText?.text?.length ?: 0))
        isTrackingChanges = true
        setModified(true)
    }

    private fun pushUndo(text: String) {
        if (undoStack.size > 200) undoStack.removeAt(0)
        undoStack.push(text)
        redoStack.clear()
    }

    private fun updateInfoBar() {
        if (currentFilePath == null) {
            filenameText?.text = "Untitled"
            lineNoEncodingText?.text = "1:1   UTF-8"
            return
        }
        val file = File(currentFilePath!!)
        val prefix = if (isModified) "* " else ""
        filenameText?.text = prefix + file.name
    }

    private fun updateLineNumber() {
        if (editText == null || lineNoEncodingText == null) return

        var line = 1
        var col = 1
        val pos = editText?.selectionStart ?: 0
        val text = editText?.text?.toString() ?: ""

        if (pos > 0 && pos <= text.length) {
            val subText = text.substring(0, pos)
            val lines = subText.split("\n")
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

        val path = currentFilePath ?: return
        val treeUri = findTreeUriForPath(path)

        if (treeUri != null) {
            loadFileViaSaf(treeUri, path)
        } else {
            loadFileDirect(path)
        }
    }

    private fun loadFileDirect(path: String) {
        executor.execute {
            val content = try {
                val sb = StringBuilder()
                BufferedReader(FileReader(path)).use { br ->
                    var line: String?
                    while (br.readLine().also { line = it } != null) {
                        sb.append(line).append("\n")
                    }
                }
                sb.toString()
            } catch (e: Exception) {
                ""
            }

            mainHandler.post {
                if (content.isNotEmpty()) {
                    pushUndo("")
                    lastText = content
                    isTrackingChanges = false
                    editText?.setText(content)
                    isTrackingChanges = true
                    setModified(false)
                    updateLineNumber()
                } else {
                    Toast.makeText(this, "Failed to load file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadFileViaSaf(treeUri: String, path: String) {
        executor.execute {
            try {
                val treeUriParsed = android.net.Uri.parse(treeUri)
                val documentUri = bin.mg.main.utils.saf.SafHelper.findDocumentUri(
                    this@FileEditorActivity, treeUriParsed, path
                )
                if (documentUri != null) {
                    val content = bin.mg.main.utils.saf.SafHelper.readFileContent(
                        this@FileEditorActivity, documentUri
                    )
                    if (content != null) {
                        mainHandler.post {
                            pushUndo("")
                            lastText = content
                            isTrackingChanges = false
                            editText?.setText(content)
                            isTrackingChanges = true
                            setModified(false)
                            updateLineNumber()
                        }
                    } else {
                        mainHandler.post {
                            Toast.makeText(this, "Failed to read via SAF", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                mainHandler.post {
                    Toast.makeText(this, "SAF read failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveFile() {
        if (currentFilePath == null) return

        val path = currentFilePath ?: return
        val content = editText?.text?.toString() ?: return

        pushUndo(lastText)
        lastText = content

        val treeUri = findTreeUriForPath(path)
        if (treeUri != null) {
            saveFileViaSaf(treeUri, path, content)
        } else {
            saveFileDirect(path, content)
        }
    }

    private fun saveFileDirect(path: String, content: String) {
        executor.execute {
            val success = try {
                Files.write(Paths.get(path), content.toByteArray(StandardCharsets.UTF_8))
                true
            } catch (e: Exception) {
                false
            }

            mainHandler.post {
                if (success) {
                    justSaved = true
                    setModified(false)
                    Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveFileViaSaf(treeUri: String, path: String, content: String) {
        executor.execute {
            try {
                val treeUriParsed = android.net.Uri.parse(treeUri)
                val documentUri = bin.mg.main.utils.saf.SafHelper.findDocumentUri(
                    this@FileEditorActivity, treeUriParsed, path
                )
                if (documentUri != null) {
                    val success = bin.mg.main.utils.saf.SafHelper.writeFileContent(
                        this@FileEditorActivity, documentUri, content
                    )
                    mainHandler.post {
                        if (success) {
                            justSaved = true
                            setModified(false)
                            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                mainHandler.post {
                    Toast.makeText(this, "SAF save failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun findTreeUriForPath(path: String): String? {
        val prefs = getSharedPreferences("MagicManagerPrefs", MODE_PRIVATE)
        val pathsJson = prefs.getString("customPaths", "") ?: return null
        if (pathsJson.isEmpty()) return null

        val entries = pathsJson.split("|||")
        for (entry in entries) {
            val parts = entry.split("<::>")
            if (parts.size >= 3) {
                val savedPath = parts[0]
                val uri = parts[2]
                if (path.startsWith(savedPath) || savedPath.startsWith(path)) {
                    return uri
                }
            }
        }
        return null
    }

    private fun showPreferences() {
        val fragment = TextEditorPreferencesFragment()
        fragment.show(supportFragmentManager, "editor_preferences")
    }

    private fun setupSymbolBar() {
        val symbols = arrayOf(
            "Tab", "{", "}", "(", ")", "[", "]", "<", ">",
            "/", "=", "+", "-", "*", ";", ":", "\"", "'", "_", "#"
        )

        for (symbol in symbols) {
            val tv = TextView(this)
            val insertText = when (symbol) {
                "Tab" -> "    "
                else -> symbol
            }

            tv.text = symbol
            tv.textSize = 14f
            tv.setTextColor(0xFF212121.toInt())
            tv.setPadding(24, 12, 24, 12)
            tv.setOnClickListener {
                val start = maxOf(editText?.selectionStart ?: 0, 0)
                val end = maxOf(editText?.selectionEnd ?: 0, 0)
                pushUndo(editText?.text?.toString() ?: "")
                editText?.text?.replace(minOf(start, end), maxOf(start, end), insertText)
            }
            symbolInput?.addView(tv)
        }
    }

    override fun onBackPressed() {
        if (searchBar?.visibility == View.VISIBLE) {
            closeSearch()
            return
        }
        if (isModified) {
            android.app.AlertDialog.Builder(this)
                .setTitle("Unsaved changes")
                .setMessage("Do you want to save before exiting?")
                .setPositiveButton("Save") { _, _ ->
                    saveFile()
                    finish()
                }
                .setNegativeButton("Discard") { _, _ -> finish() }
                .setNeutralButton("Cancel", null)
                .show()
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        private val executor = java.util.concurrent.Executors.newSingleThreadExecutor()
        private val mainHandler = Handler(Looper.getMainLooper())

        @JvmStatic
        fun start(context: Activity, filePath: String) {
            val intent = Intent(context, FileEditorActivity::class.java)
            intent.putExtra("file_path", filePath)
            context.startActivity(intent)
        }
    }
}
