package bin.mg.main.ui.editor

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.Selection
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.github.rosemoe.sora.langs.java.JavaLanguage
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import org.eclipse.tm4e.core.registry.IGrammarSource
import org.eclipse.tm4e.core.registry.IThemeSource
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.EventReceiver
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.event.Unsubscribe
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.text.Cursor
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.SymbolInputView
import bin.mg.main.R
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.Executors

class FileEditorActivity : AppCompatActivity(), EditorPreferencesFragment.OnPreferencesAppliedListener, SyntaxSelectorFragment.OnSyntaxSelectedListener {

    private var codeEditor: CodeEditor? = null
    private var symbolInput: SymbolInputView? = null
    private var filenameText: TextView? = null
    private var lineNoEncodingText: TextView? = null
    private var searchBar: LinearLayout? = null
    private var searchInput: EditText? = null
    private var searchCount: TextView? = null

    private var currentFilePath: String? = null
    private var isModified = false
    private var justSaved = false
    private var isReadOnly = false
    private var currentSyntax = "text"
    private var searchVisible = false

    private val executor = Executors.newFixedThreadPool(2)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val prefs by lazy {
        getSharedPreferences("editor_prefs", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_editor)

        initViews()
        setupToolbar()
        setupSearchBar()

        currentFilePath = intent.getStringExtra("file_path")
        if (currentFilePath != null && File(currentFilePath!!).exists()) {
            detectSyntaxAndLoad()
        } else {
            codeEditor?.setText("// No file loaded")
        }
    }

    private fun initViews() {
        codeEditor = findViewById(R.id.code_editor)
        symbolInput = findViewById(R.id.symbol_input)
        filenameText = findViewById(R.id.textview_filename)
        lineNoEncodingText = findViewById(R.id.textview_lineno_encoding)
        searchBar = findViewById(R.id.search_bar)
        searchInput = findViewById(R.id.search_input)
        searchCount = findViewById(R.id.search_count)

        applyEditorSettings()

        codeEditor?.subscribeEvent(ContentChangeEvent::class.java, object : EventReceiver<ContentChangeEvent> {
            override fun onReceive(event: ContentChangeEvent, unsubscribe: Unsubscribe) {
                if (!justSaved) {
                    isModified = true
                    updateInfoBar()
                }
                justSaved = false
                handleUndoRedoState()
            }
        })

        codeEditor?.subscribeEvent(SelectionChangeEvent::class.java, object : EventReceiver<SelectionChangeEvent> {
            override fun onReceive(event: SelectionChangeEvent, unsubscribe: Unsubscribe) {
                updateCursorPosition()
            }
        })

        symbolInput?.bindEditor(codeEditor)
        symbolInput?.addSymbols(
            arrayOf("Tab", "{", "}", "(", ")", "[", "]", "<", ">", "/", "=", "+", "-", "*", ";", ":", "\"", "'", "#", "$"),
            arrayOf("\t", "{}", "}", "(", ")", "[", "]", "<", ">", "/", "=", "+", "-", "*", ";", ":", "\"", "'", "#", "$")
        )
    }

    private fun setupToolbar() {
        findViewById<ImageView>(R.id.btn_back).setOnClickListener { confirmClose() }
        findViewById<ImageView>(R.id.btn_pin).setOnClickListener { toggleSearchBar() }
        findViewById<ImageView>(R.id.btn_undo).setOnClickListener { codeEditor?.undo() }
        findViewById<ImageView>(R.id.btn_redo).setOnClickListener { codeEditor?.redo() }
        findViewById<ImageView>(R.id.btn_save).setOnClickListener { saveFile() }
        findViewById<ImageView>(R.id.btn_edit_mode).setOnClickListener { showEditMenu(it) }
        findViewById<ImageView>(R.id.btn_overflow).setOnClickListener { showOverflowMenu(it) }
    }

    private fun setupSearchBar() {
        searchInput?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                performSearch(s?.toString() ?: "")
            }
        })

        findViewById<ImageView>(R.id.btn_search_prev).setOnClickListener {
            searchNext(false)
        }
        findViewById<ImageView>(R.id.btn_search_next).setOnClickListener {
            searchNext(true)
        }
        findViewById<ImageView>(R.id.btn_search_close).setOnClickListener {
            toggleSearchBar()
        }
    }

    private fun toggleSearchBar() {
        searchVisible = !searchVisible
        searchBar?.visibility = if (searchVisible) View.VISIBLE else View.GONE
        if (searchVisible) {
            searchInput?.requestFocus()
        } else {
            searchInput?.setText("")
        }
    }

    private var lastSearchQuery = ""

    private fun performSearch(query: String) {
        lastSearchQuery = query
        if (query.isEmpty()) {
            searchCount?.text = ""
            return
        }
        executor.execute {
            val text = codeEditor?.text?.toString() ?: return@execute
            val count = text.split(query).size - 1
            mainHandler.post {
                searchCount?.text = if (count > 0) "$count found" else "No match"
            }
        }
    }

    private fun searchNext(forward: Boolean) {
        val query = lastSearchQuery
        if (query.isEmpty()) return
        val editor = codeEditor ?: return
        val text = editor.text.toString()
        val cursor = editor.cursor
        var pos = if (forward) cursor.right else cursor.left

        if (forward) {
            val idx = text.indexOf(query, pos)
            if (idx >= 0) {
                val lineCol = offsetToLineCol(editor, idx)
                editor.setSelectionRegion(lineCol.first, lineCol.second, lineCol.first, lineCol.second + query.length)
            } else {
                val idx2 = text.indexOf(query, 0)
                if (idx2 >= 0) {
                    val lineCol = offsetToLineCol(editor, idx2)
                    editor.setSelectionRegion(lineCol.first, lineCol.second, lineCol.first, lineCol.second + query.length)
                }
            }
        } else {
            val idx = text.lastIndexOf(query, maxOf(0, pos - 1))
            if (idx >= 0) {
                val lineCol = offsetToLineCol(editor, idx)
                editor.setSelectionRegion(lineCol.first, lineCol.second, lineCol.first, lineCol.second + query.length)
            } else {
                val idx2 = text.lastIndexOf(query)
                if (idx2 >= 0) {
                    val lineCol = offsetToLineCol(editor, idx2)
                    editor.setSelectionRegion(lineCol.first, lineCol.second, lineCol.first, lineCol.second + query.length)
                }
            }
        }
    }

    private fun offsetToLineCol(editor: CodeEditor, offset: Int): Pair<Int, Int> {
        val text = editor.text
        var remaining = offset
        for (line in 0 until text.lineCount) {
            val lineLen = text.getColumnCount(line) + 1
            if (remaining < lineLen) return Pair(line, remaining)
            remaining -= lineLen
        }
        return Pair(0, 0)
    }

    private fun showEditMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, "Copy line")
        popup.menu.add(0, 2, 1, "Cut line")
        popup.menu.add(0, 3, 2, "Delete line")
        popup.menu.add(0, 4, 3, "Duplicate line")
        popup.menu.add(0, 5, 4, "Toggle comment")
        popup.menu.add(0, 6, 5, "Increase indent")
        popup.menu.add(0, 7, 6, "Decrease indent")
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> { codeEditor?.copyText(); true }
                2 -> { codeEditor?.cutLine(); true }
                3 -> { deleteCurrentLine(); true }
                4 -> { duplicateCurrentLine(); true }
                5 -> { toggleComment(); true }
                6 -> { indentLine(); true }
                7 -> { unindentLine(); true }
                else -> false
            }
        }
        popup.show()
    }

    private fun showOverflowMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, "File")
        popup.menu.add(0, 2, 1, "Search")
        popup.menu.add(0, 3, 2, "Syntax")
        popup.menu.add(0, 4, 3, "Jump to line")
        val wrapItem = popup.menu.add(0, 5, 4, "Soft wrap")
        wrapItem.isCheckable = true
        wrapItem.isChecked = prefs.getBoolean("word_wrap", false)
        val roItem = popup.menu.add(0, 6, 5, "Read-only mode")
        roItem.isCheckable = true
        roItem.isChecked = isReadOnly
        popup.menu.add(0, 7, 6, "Preferences")
        popup.menu.add(0, 8, 7, "Close file")
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> { showFilePopup(anchor); true }
                2 -> { toggleSearchBar(); true }
                3 -> { showSyntaxSelector(); true }
                4 -> { showJumpToLineDialog(); true }
                5 -> {
                    item.isChecked = !item.isChecked
                    prefs.edit().putBoolean("word_wrap", item.isChecked).apply()
                    codeEditor?.setWordwrap(item.isChecked)
                    true
                }
                6 -> {
                    item.isChecked = !item.isChecked
                    isReadOnly = item.isChecked
                    codeEditor?.setEditable(!isReadOnly)
                    true
                }
                7 -> { showPreferencesDialog(); true }
                8 -> { confirmClose(); true }
                else -> false
            }
        }
        popup.show()
    }

    private fun showFilePopup(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, "Save")
        popup.menu.add(0, 2, 1, "Save as...")
        popup.menu.add(0, 3, 2, "Close")
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> { saveFile(); true }
                2 -> { true }
                3 -> { confirmClose(); true }
                else -> false
            }
        }
        popup.show()
    }

    private fun showSyntaxSelector() {
        SyntaxSelectorFragment().show(supportFragmentManager, "syntax")
    }

    override fun onSyntaxSelected(syntaxName: String) {
        currentSyntax = syntaxName
        loadLanguageForSyntax(syntaxName)
    }

    private fun showPreferencesDialog() {
        EditorPreferencesFragment().show(supportFragmentManager, "prefs")
    }

    override fun onPreferencesApplied(
        fontSize: Int,
        wordWrap: Boolean,
        lineNumbers: Boolean,
        autoSave: Boolean,
        autoIndent: Boolean,
        syntaxHighlighting: Boolean
    ) {
        applyEditorSettings()
    }

    private fun showJumpToLineDialog() {
        val input = EditText(this)
        input.hint = "Line number"
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER

        AlertDialog.Builder(this)
            .setTitle("Jump to line")
            .setView(input)
            .setPositiveButton("Go") { _, _ ->
                val line = input.text.toString().toIntOrNull()
                if (line != null && line > 0) {
                    codeEditor?.jumpToLine(line - 1)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun applyEditorSettings() {
        val editor = codeEditor ?: return
        val fontSize = prefs.getInt("font_size", 14)
        val wordWrap = prefs.getBoolean("word_wrap", false)
        val lineNumbers = prefs.getBoolean("show_line_numbers", true)

        editor.setTextSize(fontSize.toFloat())
        editor.setWordwrap(wordWrap)
        editor.setLineNumberEnabled(lineNumbers)
        editor.setLineNumberMarginLeft(2f)
        editor.setLineSpacing(2.0f, 1.1f)

        val fontType = prefs.getString("font_type", "normal")
        val typeface = if (fontType == "monospace") Typeface.MONOSPACE else Typeface.DEFAULT
        editor.setTypefaceText(typeface)
        editor.setTypefaceLineNumber(typeface)

        try {
            val scheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
            editor.setColorScheme(scheme)
        } catch (_: Exception) {}
    }

    private fun handleUndoRedoState() {
        val undoEnabled = codeEditor?.canUndo() == true
        val redoEnabled = codeEditor?.canRedo() == true
        findViewById<ImageView>(R.id.btn_undo).alpha = if (undoEnabled) 1.0f else 0.3f
        findViewById<ImageView>(R.id.btn_redo).alpha = if (redoEnabled) 1.0f else 0.3f
    }

    private fun detectSyntaxAndLoad() {
        val path = currentFilePath ?: return
        val ext = path.substringAfterLast(".", "").lowercase()
        currentSyntax = when (ext) {
            "java" -> "java"
            "kt" -> "kotlin"
            "py" -> "python"
            "js" -> "javascript"
            "html", "htm" -> "html"
            "css" -> "css"
            "xml" -> "xml"
            "json" -> "json"
            "sh", "bash" -> "shell"
            "smali" -> "smali"
            else -> "text"
        }
        loadFile()
    }

    private fun loadFile() {
        if (currentFilePath == null) return
        val path = currentFilePath!!

        val dialog = ProgressDialog.show(this, "Loading", "Reading file...", true)
        executor.execute {
            try {
                val sb = StringBuilder()
                BufferedReader(FileReader(path)).use { br ->
                    var line: String?
                    while (br.readLine().also { line = it } != null) {
                        sb.append(line).append("\n")
                    }
                }
                val content = sb.toString()

                mainHandler.post {
                    dialog.dismiss()
                    codeEditor?.setText(content)
                    loadLanguageForSyntax(currentSyntax)
                    isModified = false
                    justSaved = true
                    updateInfoBar()
                    handleUndoRedoState()
                }
            } catch (e: Exception) {
                mainHandler.post {
                    dialog.dismiss()
                    Toast.makeText(this@FileEditorActivity, "Failed to load file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadLanguageForSyntax(syntax: String) {
        val editor = codeEditor ?: return
        val enabled = prefs.getBoolean("syntax_highlighting", true)

        if (!enabled || syntax == "text") {
            editor.setEditorLanguage(EmptyLanguage())
            return
        }

        try {
            when (syntax) {
                "java" -> editor.setEditorLanguage(JavaLanguage())
                "kotlin" -> editor.setEditorLanguage(JavaLanguage())
                "smali" -> loadTextMateLanguage("smali")
                else -> editor.setEditorLanguage(EmptyLanguage())
            }
        } catch (_: Exception) {
            editor.setEditorLanguage(EmptyLanguage())
        }
    }

    private fun loadTextMateLanguage(grammarName: String) {
        val editor = codeEditor ?: return
        try {
            val themeRegistry = ThemeRegistry.getInstance()
            val themeSource = IThemeSource.fromInputStream(
                assets.open("themes/light.json"),
                "light.json",
                null
            )
            themeRegistry.loadTheme(ThemeModel(themeSource, "light"))
            themeRegistry.setTheme("light")

            val grammarSource = IGrammarSource.fromInputStream(
                assets.open("$grammarName/syntaxes/$grammarName.tmLanguage.json"),
                "$grammarName.tmLanguage.json",
                null
            )
            val langConfig = assets.open("$grammarName/language-configuration.json").bufferedReader()
            val language = TextMateLanguage.create(grammarSource, langConfig, themeSource)
            editor.setEditorLanguage(language)
        } catch (_: Exception) {
            editor.setEditorLanguage(EmptyLanguage())
        }
    }

    private fun updateCursorPosition() {
        val editor = codeEditor ?: return
        val cursor = editor.cursor
        val line = cursor.leftLine + 1
        val col = cursor.leftColumn + 1
        val modified = if (isModified) " *" else ""
        lineNoEncodingText?.text = "$line:$col   UTF-8$modified"
    }

    private fun updateInfoBar() {
        updateCursorPosition()
    }

    private fun saveFile() {
        if (currentFilePath == null) return
        val path = currentFilePath!!

        val dialog = ProgressDialog.show(this, "Saving", "Writing file...", true)
        executor.execute {
            try {
                val content = codeEditor?.text?.toString() ?: ""
                Files.write(Paths.get(path), content.toByteArray(StandardCharsets.UTF_8))
                mainHandler.post {
                    dialog.dismiss()
                    justSaved = true
                    isModified = false
                    updateInfoBar()
                    Toast.makeText(this@FileEditorActivity, "Saved", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                mainHandler.post {
                    dialog.dismiss()
                    Toast.makeText(this@FileEditorActivity, "Save failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun deleteCurrentLine() {
        val editor = codeEditor ?: return
        val cursor = editor.cursor
        val line = cursor.leftLine
        val lineCount = editor.lineCount
        if (lineCount <= 1) {
            editor.setSelection(0, 0)
            editor.setText("")
        } else if (line == lineCount - 1) {
            editor.setSelectionRegion(line - 1, editor.text.getColumnCount(line - 1), line, editor.text.getColumnCount(line))
            editor.deleteText()
        } else {
            editor.setSelectionRegion(line, 0, line + 1, 0)
            editor.deleteText()
        }
    }

    private fun duplicateCurrentLine() {
        val editor = codeEditor ?: return
        val cursor = editor.cursor
        val line = cursor.leftLine
        val lineContent = editor.text.getLineString(line)
        editor.text.insert(line, editor.text.getColumnCount(line), "\n$lineContent")
    }

    private fun toggleComment() {
        val editor = codeEditor ?: return
        val cursor = editor.cursor
        val line = cursor.leftLine
        val lineStr = editor.text.getLineString(line)
        val firstNonSpace = lineStr.indexOfFirst { !it.isWhitespace() }

        if (firstNonSpace < 0) return

        if (lineStr[firstNonSpace] == '#') {
            val endPos = if (firstNonSpace + 1 < lineStr.length && lineStr[firstNonSpace + 1] == ' ') firstNonSpace + 2 else firstNonSpace + 1
            editor.setSelectionRegion(line, firstNonSpace, line, endPos)
            editor.deleteText()
        } else {
            editor.setSelection(line, firstNonSpace)
            editor.commitText("# ")
        }
    }

    private fun indentLine() {
        val editor = codeEditor ?: return
        val cursor = editor.cursor
        val line = cursor.leftLine
        editor.setSelection(line, 0)
        editor.commitText("    ")
    }

    private fun unindentLine() {
        val editor = codeEditor ?: return
        val cursor = editor.cursor
        val line = cursor.leftLine
        val lineStr = editor.text.getLineString(line)
        if (lineStr.startsWith("    ")) {
            editor.setSelectionRegion(line, 0, line, 4)
            editor.deleteText()
        } else if (lineStr.startsWith("\t")) {
            editor.setSelectionRegion(line, 0, line, 1)
            editor.deleteText()
        }
    }

    private fun confirmClose() {
        if (isModified) {
            AlertDialog.Builder(this)
                .setTitle("Unsaved Changes")
                .setMessage("Do you want to save the changes?")
                .setPositiveButton("Save") { _, _ -> saveAndFinish() }
                .setNegativeButton("Discard") { _, _ -> finish() }
                .setNeutralButton("Cancel", null)
                .show()
        } else {
            finish()
        }
    }

    private fun saveAndFinish() {
        saveFile()
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        confirmClose()
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
