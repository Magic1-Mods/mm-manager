package bin.mg.main.file

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import bin.mg.main.R
import bin.mg.main.utils.theme.ThemeManager
import modder.hub.editor.EditView
import modder.hub.editor.GapBuffer
import modder.hub.editor.OnTextChangedListener
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.Executors
import org.mozilla.universalchardet.UniversalDetector

class TextEditorActivity : AppCompatActivity(),
    EditorPreferencesFragment.OnPreferencesAppliedListener,
    SyntaxSelectorFragment.OnSyntaxSelectedListener {

    // ─── Views ────────────────────────────────────────────────────────────────
    private var codeEditor: EditView? = null
    private var symbolInput: LinearLayout? = null
    private var filenameText: TextView? = null
    private var lineNoEncodingText: TextView? = null
    private var searchBar: LinearLayout? = null
    private var searchInput: EditText? = null
    private var searchCount: TextView? = null
    private var drawerLayout: DrawerLayout? = null
    private var openFilesRecycler: RecyclerView? = null

    // ─── State ────────────────────────────────────────────────────────────────
    private var currentFilePath: String? = null
    private var isModified = false
    private var isReadOnly = false
    private var currentSyntax = "text"
    private var searchVisible = false
    private var mDefaultCharset: Charset = StandardCharsets.UTF_8
    private var positionHistory = mutableListOf<Pair<Int, Int>>()
    private var positionIndex = -1

    // Multi-file support
    private val openFiles = mutableListOf<String>()
    private var currentFileIndex = -1
    private val fileContents = mutableMapOf<String, String>()
    private val fileSyntaxes = mutableMapOf<String, String>()
    private val filePositions = mutableMapOf<String, Pair<Int, Int>>()

    private val executor = Executors.newFixedThreadPool(2)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var openFileAdapter: OpenFileAdapter? = null
    private var mLastCursorLine = 0
    private var mLastCursorCol = 0
    private var suppressContentChange = false

    private val prefs by lazy { getSharedPreferences("editor_prefs", MODE_PRIVATE) }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_editor)

        initViews()
        applyTheme()
        setupDrawer()
        setupToolbar()
        setupSearchBar()

        val intentPath = intent.getStringExtra("file_path")
        if (intentPath != null && File(intentPath).exists()) {
            openFile(intentPath)
        } else {
            codeEditor?.setText("// No file loaded\n// Tap the menu to open a file")
            filenameText?.text = "untitled"
        }
    }

    // ─── Theme ────────────────────────────────────────────────────────────────

    private fun applyTheme() {
        val isDark = ThemeManager.isDarkMode(this)
        val toolbarBg = ThemeManager.toolbarBackground(this)
        val drawerBg = ThemeManager.drawerBackground(this)
        val dividerColor = ThemeManager.dividerColor(this)

        window.statusBarColor = toolbarBg

        val toolbarIconTint = 0xFFFFFFFF.toInt()
        val filenameColor = if (isDark) 0xFFCCCCCC.toInt() else ThemeManager.lighten(ThemeManager.primary(this), 0.80f)
        val statsColor = ThemeManager.statsText(this)
        val symbolBarBg = if (isDark) 0xFF1A1A1A.toInt() else ThemeManager.lighten(ThemeManager.primary(this), 0.94f)
        val searchBg = if (isDark) 0xFF1E1E1E.toInt() else ThemeManager.lighten(ThemeManager.primary(this), 0.96f)
        val searchTextColor = if (isDark) 0xFFDDDDDD.toInt() else 0xFF222222.toInt()
        val searchHintColor = if (isDark) 0xFF666666.toInt() else ThemeManager.lighten(ThemeManager.primary(this), 0.55f)
        val searchIconTint = if (isDark) 0xFF888888.toInt() else ThemeManager.lighten(ThemeManager.primary(this), 0.45f)
        val editorBodyBg = if (isDark) 0xFF1E1E1E.toInt() else 0xFFFAFAFA.toInt()

        findViewById<LinearLayout>(R.id.editor_main_content)?.setBackgroundColor(editorBodyBg)
        findViewById<LinearLayout>(R.id.editor_toolbar)?.setBackgroundColor(toolbarBg)

        for (id in listOf(R.id.btn_menu, R.id.btn_pin, R.id.btn_undo, R.id.btn_redo,
                          R.id.btn_save, R.id.btn_edit_mode, R.id.btn_overflow)) {
            findViewById<ImageView>(id)?.setColorFilter(toolbarIconTint)
        }

        filenameText?.setTextColor(filenameColor)
        lineNoEncodingText?.setTextColor(statsColor)
        findViewById<View>(R.id.editor_divider)?.setBackgroundColor(dividerColor)
        searchBar?.setBackgroundColor(searchBg)
        searchInput?.setTextColor(searchTextColor)
        searchInput?.setHintTextColor(searchHintColor)
        searchCount?.setTextColor(searchHintColor)
        for (id in listOf(R.id.btn_search_prev, R.id.btn_search_next, R.id.btn_search_close)) {
            findViewById<ImageView>(id)?.setColorFilter(searchIconTint)
        }

        findViewById<LinearLayout>(R.id.symbol_bar)?.setBackgroundColor(symbolBarBg)

        findViewById<LinearLayout>(R.id.editor_drawer)?.setBackgroundColor(drawerBg)
        findViewById<LinearLayout>(R.id.drawer_header)?.setBackgroundColor(toolbarBg)
        for (id in listOf(R.id.btn_drawer_edit, R.id.btn_drawer_overflow)) {
            findViewById<ImageView>(id)?.setColorFilter(toolbarIconTint)
        }

        // Apply editor dark mode for syntax highlighting
        codeEditor?.setSyntaxDarkMode(isDark)
    }

    // ─── View init ────────────────────────────────────────────────────────────

    private fun initViews() {
        codeEditor = findViewById(R.id.code_editor)
        symbolInput = findViewById(R.id.symbol_input)
        filenameText = findViewById(R.id.textview_filename)
        lineNoEncodingText = findViewById(R.id.textview_lineno_encoding)
        searchBar = findViewById(R.id.search_bar)
        searchInput = findViewById(R.id.search_input)
        searchCount = findViewById(R.id.search_count)
        drawerLayout = findViewById(R.id.editor_drawer_layout)
        openFilesRecycler = findViewById(R.id.recycler_open_files)

        applyEditorSettings()
        setupSymbolBar()

        codeEditor?.setOnTextChangedListener(object : OnTextChangedListener {
            override fun onTextChanged() {
                if (!suppressContentChange) {
                    isModified = true
                    updateInfoBar()
                    handleUndoRedoState()
                }
            }
        })

        filenameText?.setOnClickListener {
            currentFilePath?.let { path ->
                val clip = android.content.ClipData.newPlainText("filename", File(path).name)
                getSystemService(android.content.ClipboardManager::class.java)?.setPrimaryClip(clip)
                Toast.makeText(this, "Filename copied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun applyEditorSettings() {
        val editor = codeEditor ?: return
        editor.setTextSize(prefs.getInt("font_size", 14).toFloat())
        editor.setTypeface(Typeface.MONOSPACE)
    }

    // ─── Symbol bar ───────────────────────────────────────────────────────────

    private fun setupSymbolBar() {
        val container = symbolInput ?: return
        container.removeAllViews()
        container.orientation = LinearLayout.HORIZONTAL

        val isDark = ThemeManager.isDarkMode(this)
        val textColor = if (isDark) 0xFFCCCCCC.toInt() else 0xFF333333.toInt()
        val bgColor = if (isDark) 0xFF1A1A1A.toInt() else 0xFFE8E8E8.toInt()
        val dividerColor = if (isDark) 0xFF333333.toInt() else 0xFFCCCCCC.toInt()

        container.setBackgroundColor(bgColor)

        // MH-TextEditor symbol bar: -> {} () , . ; " ? + - * / < > [ ] :
        val symbols = arrayOf("->", "{", "}", "(", ")", ",", ".", ";", "\"", "?",
                              "+", "-", "*", "/", "<", ">", "[", "]", ":")
        val insertTexts = arrayOf("\t", "{}", "}", "(", ")", ",", ".", ";", "\"", "?",
                                  "+", "-", "*", "/", "<", ">", "[", "]", ":")

        for (i in symbols.indices) {
            val btn = TextView(this).apply {
                text = symbols[i]
                setTextColor(textColor)
                textSize = 14f
                typeface = Typeface.MONOSPACE
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1f
                )
                isClickable = true
                isFocusable = true
                setBackgroundResource(android.R.drawable.list_selector_background)
                setOnClickListener {
                    codeEditor?.insertText(insertTexts[i])
                    codeEditor?.requestFocus()
                }
            }
            container.addView(btn)

            if (i < symbols.size - 1) {
                val divider = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        1,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(dividerColor)
                }
                container.addView(divider)
            }
        }
    }

    // ─── Drawer ───────────────────────────────────────────────────────────────

    private fun setupDrawer() {
        openFileAdapter = OpenFileAdapter { index ->
            switchToFile(index)
            drawerLayout?.closeDrawers()
        }
        openFilesRecycler?.layoutManager = LinearLayoutManager(this)
        openFilesRecycler?.adapter = openFileAdapter

        findViewById<android.widget.Button>(R.id.btn_minimize)?.setOnClickListener {
            drawerLayout?.closeDrawers()
        }

        val toggle = androidx.appcompat.app.ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close)
        drawerLayout?.addDrawerListener(toggle)
        toggle.syncState()

        findViewById<ImageView>(R.id.btn_drawer_edit)?.setOnClickListener {
            drawerLayout?.closeDrawers()
        }
        findViewById<ImageView>(R.id.btn_drawer_overflow)?.setOnClickListener { v ->
            showDrawerOverflowMenu(v)
        }
    }

    private fun showDrawerOverflowMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, "New file")
        popup.menu.add(0, 2, 1, "Open file")
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> { Toast.makeText(this, "New file", Toast.LENGTH_SHORT).show(); true }
                2 -> { Toast.makeText(this, "Open file", Toast.LENGTH_SHORT).show(); true }
                else -> false
            }
        }
        popup.show()
    }

    // ─── Toolbar ──────────────────────────────────────────────────────────────

    private fun setupToolbar() {
        findViewById<ImageView>(R.id.btn_menu)?.setOnClickListener { drawerLayout?.openDrawer(Gravity.START) }
        findViewById<ImageView>(R.id.btn_pin)?.setOnClickListener { toggleSearchBar() }
        findViewById<ImageView>(R.id.btn_undo)?.setOnClickListener { codeEditor?.undo(); handleUndoRedoState() }
        findViewById<ImageView>(R.id.btn_redo)?.setOnClickListener { codeEditor?.redo(); handleUndoRedoState() }
        findViewById<ImageView>(R.id.btn_save)?.setOnClickListener { saveFile() }
        findViewById<ImageView>(R.id.btn_edit_mode)?.setOnClickListener { showEditMenu(it) }
        findViewById<ImageView>(R.id.btn_overflow)?.setOnClickListener { showOverflowMenu(it) }
    }

    private fun handleUndoRedoState() {
        val undoEnabled = codeEditor?.canUndo() == true
        val redoEnabled = codeEditor?.canRedo() == true
        val dimAlpha = 0.28f
        findViewById<ImageView>(R.id.btn_undo).alpha = if (undoEnabled) 1f else dimAlpha
        findViewById<ImageView>(R.id.btn_redo).alpha = if (redoEnabled) 1f else dimAlpha
        findViewById<ImageView>(R.id.btn_save).alpha = if (isModified) 1f else dimAlpha
        updateFilenameTab()
    }

    private fun updateInfoBar() {
        updateFilenameTab()
    }

    private fun updateFilenameTab() {
        val name = currentFilePath?.let { File(it).name } ?: "untitled"
        filenameText?.text = if (isModified) "*$name" else name
    }

    // ─── Search bar ───────────────────────────────────────────────────────────

    private fun setupSearchBar() {
        findViewById<ImageView>(R.id.btn_search_prev)?.setOnClickListener {
            val query = searchInput?.text?.toString() ?: return@setOnClickListener
            if (query.isNotEmpty()) codeEditor?.find(query)
        }
        findViewById<ImageView>(R.id.btn_search_next)?.setOnClickListener {
            val query = searchInput?.text?.toString() ?: return@setOnClickListener
            if (query.isNotEmpty()) codeEditor?.find(query)
        }
        findViewById<ImageView>(R.id.btn_search_close)?.setOnClickListener { toggleSearchBar() }
    }

    private fun toggleSearchBar() {
        searchVisible = !searchVisible
        searchBar?.visibility = if (searchVisible) View.VISIBLE else View.GONE
    }

    // ─── Popup menus ─────────────────────────────────────────────────────────

    private fun showEditMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, "Copy")
        popup.menu.add(0, 2, 1, "Cut")
        popup.menu.add(0, 3, 2, "Paste")
        popup.menu.add(0, 4, 3, "Select all")
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> { codeEditor?.copy(); true }
                2 -> { codeEditor?.cut(); true }
                3 -> { codeEditor?.paste(); true }
                4 -> { codeEditor?.selectAll(); true }
                else -> false
            }
        }
        popup.show()
    }

    private fun showOverflowMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, "Search")
        popup.menu.add(0, 2, 1, "Jump to line")
        popup.menu.add(0, 3, 2, "Syntax")
        popup.menu.add(0, 4, 3, "Preferences")
        popup.menu.add(0, 5, 4, "Close file")
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> { toggleSearchBar(); true }
                2 -> { showJumpToLineDialog(); true }
                3 -> { showSyntaxSelector(); true }
                4 -> { showPreferencesDialog(); true }
                5 -> { confirmClose(); true }
                else -> false
            }
        }
        popup.show()
    }

    // ─── Dialogs ──────────────────────────────────────────────────────────────

    private fun showSyntaxSelector() { SyntaxSelectorFragment().show(supportFragmentManager, "syntax") }

    override fun onSyntaxSelected(syntaxName: String) {
        currentSyntax = syntaxName
        if (currentFileIndex >= 0 && currentFileIndex < openFiles.size)
            fileSyntaxes[openFiles[currentFileIndex]] = syntaxName
        codeEditor?.setSyntaxLanguageFileName(syntaxToFileName(syntaxName))
    }

    private fun syntaxToFileName(syntax: String): String? = when (syntax) {
        "smali" -> "smali.json"
        "xml" -> "xml.json"
        "java" -> "java.json"
        else -> null
    }

    private fun showPreferencesDialog() { EditorPreferencesFragment().show(supportFragmentManager, "prefs") }

    override fun onPreferencesApplied(
        fontSize: Int, wordWrap: Boolean, lineNumbers: Boolean,
        autoSave: Boolean, autoIndent: Boolean, syntaxHighlighting: Boolean
    ) { applyEditorSettings() }

    private fun showJumpToLineDialog() {
        val input = EditText(this)
        input.hint = "Line number"
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        AlertDialog.Builder(this)
            .setTitle("Jump to line")
            .setView(input)
            .setPositiveButton("Go") { _, _ ->
                input.text.toString().toIntOrNull()?.let { if (it > 0) codeEditor?.gotoLine(it - 1) }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ─── File I/O ─────────────────────────────────────────────────────────────

    private fun saveFile() {
        val path = currentFilePath ?: return
        val dialog = ProgressDialog.show(this, "Saving", "Writing file…", true)
        executor.execute {
            try {
                val content = codeEditor?.getBuffer()?.toString() ?: ""
                Files.write(Paths.get(path), content.toByteArray(mDefaultCharset))
                fileContents[path] = content
                mainHandler.post {
                    dialog.dismiss()
                    isModified = false
                    handleUndoRedoState()
                    updateInfoBar()
                    Toast.makeText(this@TextEditorActivity, "Saved", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                mainHandler.post { dialog.dismiss(); Toast.makeText(this@TextEditorActivity, "Save failed", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    // ─── Multi-file management ───────────────────────────────────────────────

    private fun openFile(path: String) {
        val existing = openFiles.indexOf(path)
        if (existing >= 0) { switchToFile(existing); return }
        if (currentFileIndex >= 0 && currentFileIndex < openFiles.size) {
            val old = openFiles[currentFileIndex]
            fileContents[old] = codeEditor?.getBuffer()?.toString() ?: ""
            fileSyntaxes[old] = currentSyntax
        }
        openFiles.add(path)
        currentFileIndex = openFiles.size - 1
        currentFilePath = path
        filenameText?.text = File(path).name
        loadFile()
        updateDrawerList()
    }

    private fun switchToFile(index: Int) {
        if (index < 0 || index >= openFiles.size || index == currentFileIndex) return
        if (currentFileIndex >= 0 && currentFileIndex < openFiles.size) {
            val old = openFiles[currentFileIndex]
            fileContents[old] = codeEditor?.getBuffer()?.toString() ?: ""
            fileSyntaxes[old] = currentSyntax
        }
        currentFileIndex = index
        val path = openFiles[index]
        currentFilePath = path
        currentSyntax = fileSyntaxes[path] ?: "text"
        suppressContentChange = true
        val content = fileContents[path]
        if (content != null) {
            codeEditor?.setText(content)
            codeEditor?.setSyntaxLanguageFileName(syntaxToFileName(currentSyntax))
            isModified = false
            handleUndoRedoState()
            updateInfoBar()
        } else {
            loadFile()
        }
        suppressContentChange = false
        updateDrawerList()
    }

    private fun closeFile(index: Int) {
        if (index < 0 || index >= openFiles.size) return
        val path = openFiles[index]
        fileContents.remove(path); fileSyntaxes.remove(path); filePositions.remove(path)
        openFiles.removeAt(index)
        if (openFiles.isEmpty()) { finish(); return }
        if (index == currentFileIndex) { currentFileIndex = minOf(index, openFiles.size - 1); switchToFile(currentFileIndex) }
        else if (index < currentFileIndex) currentFileIndex--
        updateDrawerList()
    }

    private fun updateDrawerList() { openFileAdapter?.setFiles(openFiles, currentFileIndex) }

    private fun loadFile() {
        val path = currentFilePath ?: return
        val dialog = ProgressDialog.show(this, "Loading", "Reading file…", true)
        executor.execute {
            try {
                val file = File(path)
                if (file.length() > MAX_FILE_SIZE_BYTES) {
                    mainHandler.post {
                        dialog.dismiss()
                        codeEditor?.setText("File too large (${file.length() / 1024} KB). Max supported: 2 MB.")
                        isModified = false
                        handleUndoRedoState()
                        updateInfoBar()
                    }
                    return@execute
                }

                // Detect charset
                val detectedCharset = UniversalDetector.detectCharset(file)
                if (detectedCharset != null) {
                    mDefaultCharset = Charset.forName(detectedCharset)
                }

                val sb = StringBuilder()
                BufferedReader(FileReader(path)).use { br ->
                    var line: String?
                    while (br.readLine().also { line = it } != null) sb.append(line).append("\n")
                }
                val content = sb.toString()
                currentSyntax = detectSyntaxForFile(path)
                fileSyntaxes[path] = currentSyntax

                mainHandler.post {
                    dialog.dismiss()
                    suppressContentChange = true
                    codeEditor?.setText(content)
                    codeEditor?.setSyntaxLanguageFileName(syntaxToFileName(currentSyntax))
                    suppressContentChange = false
                    fileContents[path] = content
                    isModified = false
                    handleUndoRedoState()
                    updateInfoBar()
                }
            } catch (e: Exception) {
                mainHandler.post { dialog.dismiss(); Toast.makeText(this@TextEditorActivity, "Failed to load file", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun detectSyntaxForFile(path: String): String {
        return when (path.substringAfterLast(".", "").lowercase()) {
            "smali" -> "smali"
            "xml", "html", "htm" -> "xml"
            "java" -> "java"
            else -> "text"
        }
    }

    // ─── Close guard ─────────────────────────────────────────────────────────

    private fun confirmClose() {
        if (isModified) {
            AlertDialog.Builder(this)
                .setTitle("Unsaved Changes")
                .setMessage("Do you want to save the changes?")
                .setPositiveButton("Save")    { _, _ -> saveFile(); finish() }
                .setNegativeButton("Discard") { _, _ -> finish() }
                .setNeutralButton("Cancel", null)
                .show()
        } else {
            finish()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (drawerLayout?.isDrawerOpen(Gravity.START) == true) drawerLayout?.closeDrawers()
        else confirmClose()
    }

    // ─── Static helpers ───────────────────────────────────────────────────────

    companion object {
        private const val MAX_FILE_SIZE_BYTES = 2L * 1024 * 1024

        @JvmStatic
        fun start(context: Activity, filePath: String) {
            context.startActivity(
                Intent(context, TextEditorActivity::class.java)
                    .putExtra("file_path", filePath)
            )
        }
    }
}
