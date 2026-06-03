package bin.mg.main.file

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.EventReceiver
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.event.Unsubscribe
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.SymbolInputView
import io.github.rosemoe.sora.widget.component.EditorTextActionWindow
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import bin.mg.main.R
import bin.mg.main.utils.theme.ThemeManager
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.Executors

class TextEditorActivity : AppCompatActivity(),
    EditorPreferencesFragment.OnPreferencesAppliedListener,
    SyntaxSelectorFragment.OnSyntaxSelectedListener {

    // ─── Views ────────────────────────────────────────────────────────────────
    private var codeEditor: CodeEditor? = null
    private var symbolInput: SymbolInputView? = null
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
    private var justSaved = false
    private var isReadOnly = false
    private var isSmoothMode = false
    private var isCodeCompletion = true
    private var currentSyntax = "text"
    private var searchVisible = false
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
    private lateinit var syntaxEngine: MtsxSyntaxEngine

    private val prefs by lazy { getSharedPreferences("editor_prefs", MODE_PRIVATE) }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_editor)

        syntaxEngine = MtsxSyntaxEngine(this)
        executor.execute { syntaxEngine.loadAllSyntaxes() }

        initViews()
        applyTheme()          // ← single call wires all colours
        setupDrawer()
        setupToolbar()
        setupSearchBar()

        val intentPath = intent.getStringExtra("file_path")
        if (intentPath != null && File(intentPath).exists()) {
            openFile(intentPath)
        } else {
            codeEditor?.setText("// No file loaded")
            filenameText?.text = "untitled"
        }
    }

    // ─── Theme ────────────────────────────────────────────────────────────────

    /**
     * Apply ThemeManager colours to every surface.
     *
     * Rules:
     *  • followSystemTheme == true  →  isDarkMode() drives everything (dark bg or light bg)
     *  • followSystemTheme == false →  use the selected palette's primary/secondary colours
     *    for the toolbar & drawer header; the editor body gets dark-mode colours always
     *    (standard code-editor look) unless the system is in light mode AND no palette chosen.
     */
    private fun applyTheme() {
        val isDark       = ThemeManager.isDarkMode(this)
        val followSystem = ThemeManager.followSystemTheme(this)
        val primary      = ThemeManager.primary(this)
        val secondary    = ThemeManager.secondary(this)
        val toolbarBg    = ThemeManager.toolbarBackground(this)
        val drawerBg     = ThemeManager.drawerBackground(this)
        val infoBg       = if (isDark) darkenSurface(toolbarBg, 0.15f)
                           else darkenSurface(toolbarBg, 0.08f)
        val dividerColor = ThemeManager.dividerColor(this)
        
        val window: Window = window
        window.statusBarColor = toolbarBg

        // Icon / text tint on toolbar — always white (toolbar is always coloured/dark)
        val toolbarIconTint = 0xFFFFFFFF.toInt()
        // Filename text on info bar
        val filenameColor   = if (isDark) 0xFFCCCCCC.toInt() else ThemeManager.lighten(primary, 0.80f)
        val statsColor      = ThemeManager.statsText(this)
        // Symbol bar
        val symbolBarBg     = if (isDark) 0xFF1A1A1A.toInt() else ThemeManager.lighten(primary, 0.94f)
        // Search bar
        val searchBg        = if (isDark) 0xFF1E1E1E.toInt() else ThemeManager.lighten(primary, 0.96f)
        val searchTextColor = if (isDark) 0xFFDDDDDD.toInt() else 0xFF222222.toInt()
        val searchHintColor = if (isDark) 0xFF666666.toInt() else ThemeManager.lighten(primary, 0.55f)
        val searchIconTint  = if (isDark) 0xFF888888.toInt() else ThemeManager.lighten(primary, 0.45f)
        // Main editor body bg
        val editorBodyBg    = if (isDark) 0xFF1E1E1E.toInt() else 0xFFFAFAFA.toInt()

        // ── Root / main content background ──────────────────────────────────
        val mainContent = findViewById<LinearLayout>(R.id.editor_main_content)
        mainContent?.setBackgroundColor(editorBodyBg)

        // ── Toolbar ──────────────────────────────────────────────────────────
        val toolbar = findViewById<LinearLayout>(R.id.editor_toolbar)
        toolbar?.setBackgroundColor(toolbarBg)

        for (id in listOf(R.id.btn_menu, R.id.btn_pin, R.id.btn_undo, R.id.btn_redo,
                          R.id.btn_save, R.id.btn_edit_mode, R.id.btn_overflow)) {
            findViewById<ImageView>(id)?.setColorFilter(toolbarIconTint)
        }

        // ── Info bar ─────────────────────────────────────────────────────────
        val infoBar = findViewById<LinearLayout>(R.id.editor_info_bar)
        infoBar?.setBackgroundColor(infoBg)
        filenameText?.setTextColor(filenameColor)
        lineNoEncodingText?.setTextColor(statsColor)

        // ── Divider ──────────────────────────────────────────────────────────
        findViewById<View>(R.id.editor_divider)?.setBackgroundColor(dividerColor)

        // ── Search bar ───────────────────────────────────────────────────────
        searchBar?.setBackgroundColor(searchBg)
        searchInput?.setTextColor(searchTextColor)
        searchInput?.setHintTextColor(searchHintColor)
        searchCount?.setTextColor(searchHintColor)
        for (id in listOf(R.id.btn_search_prev, R.id.btn_search_next, R.id.btn_search_close)) {
            findViewById<ImageView>(id)?.setColorFilter(searchIconTint)
        }

        // ── Symbol bar ───────────────────────────────────────────────────────
        val symbolBar = findViewById<LinearLayout>(R.id.symbol_bar)
        symbolBar?.setBackgroundColor(symbolBarBg)
        symbolInput?.let { siv ->
            siv.setBackgroundColor(symbolBarBg)
            siv.setTextColor(if (isDark) 0xFFCCCCCC.toInt() else ThemeManager.darken(primary, 0.3f))
        }

        // ── Drawer ───────────────────────────────────────────────────────────
        val drawer = findViewById<LinearLayout>(R.id.editor_drawer)
        // Drawer body is lighter than header
        val drawerBodyBg = if (isDark) 0xFF242424.toInt() else 0xFFFFFFFF.toInt()
        drawer?.setBackgroundColor(drawerBodyBg)

        val drawerHeader = findViewById<LinearLayout>(R.id.drawer_header)
        drawerHeader?.setBackgroundColor(toolbarBg)          // same colour as toolbar

        for (id in listOf(R.id.btn_drawer_edit, R.id.btn_drawer_overflow)) {
            findViewById<ImageView>(id)?.setColorFilter(toolbarIconTint)
        }

        // ── Editor colour scheme ─────────────────────────────────────────────
        applyEditorColorScheme(isDark)
    }

    /**
     * Build and apply an EditorColorScheme.
     * Dark  → VS-Dark / JetBrains palette (matches screenshot).
     * Light → clean light palette.
     */
    private fun applyEditorColorScheme(isDark: Boolean) {
        val scheme = EditorColorScheme()
        if (isDark) {
            scheme.setColor(EditorColorScheme.WHOLE_BACKGROUND,           Color.parseColor("#1E1E1E"))
            scheme.setColor(EditorColorScheme.CURRENT_LINE,               Color.parseColor("#2A2D2E"))
            scheme.setColor(EditorColorScheme.LINE_NUMBER_BACKGROUND,     Color.parseColor("#1E1E1E"))
            scheme.setColor(EditorColorScheme.LINE_NUMBER,                Color.parseColor("#606366"))
            scheme.setColor(EditorColorScheme.LINE_NUMBER_CURRENT,        Color.parseColor("#A0A0A0"))
            scheme.setColor(EditorColorScheme.TEXT_NORMAL,                Color.parseColor("#A9B7C6"))
            scheme.setColor(EditorColorScheme.TEXT_SELECTED,              Color.parseColor("#214283"))
            scheme.setColor(EditorColorScheme.SELECTED_TEXT_BACKGROUND,   Color.parseColor("#214283"))
            scheme.setColor(EditorColorScheme.MATCHED_TEXT_BACKGROUND,    Color.parseColor("#32593A"))
            scheme.setColor(EditorColorScheme.FUNCTION_NAME,              Color.parseColor("#FFC66D"))
            scheme.setColor(EditorColorScheme.KEYWORD,                    Color.parseColor("#CC7832"))
            scheme.setColor(EditorColorScheme.LITERAL,                    Color.parseColor("#6A8759"))
            scheme.setColor(EditorColorScheme.ANNOTATION,                 Color.parseColor("#BBB529"))
            scheme.setColor(EditorColorScheme.COMMENT,                    Color.parseColor("#808080"))
        } else {
            // Light scheme
            scheme.setColor(EditorColorScheme.WHOLE_BACKGROUND,           Color.parseColor("#FAFAFA"))
            scheme.setColor(EditorColorScheme.CURRENT_LINE,               Color.parseColor("#F0F4FF"))
            scheme.setColor(EditorColorScheme.LINE_NUMBER_BACKGROUND,     Color.parseColor("#F5F5F5"))
            scheme.setColor(EditorColorScheme.LINE_NUMBER,                Color.parseColor("#AAAAAA"))
            scheme.setColor(EditorColorScheme.LINE_NUMBER_CURRENT,        Color.parseColor("#666666"))
            scheme.setColor(EditorColorScheme.TEXT_NORMAL,                Color.parseColor("#212121"))
            scheme.setColor(EditorColorScheme.TEXT_SELECTED,              Color.parseColor("#BBDEFB"))
            scheme.setColor(EditorColorScheme.SELECTED_TEXT_BACKGROUND,   Color.parseColor("#BBDEFB"))
            scheme.setColor(EditorColorScheme.MATCHED_TEXT_BACKGROUND,    Color.parseColor("#C8E6C9"))
            scheme.setColor(EditorColorScheme.FUNCTION_NAME,              Color.parseColor("#795548"))
            scheme.setColor(EditorColorScheme.KEYWORD,                    Color.parseColor("#0000FF"))
            scheme.setColor(EditorColorScheme.LITERAL,                    Color.parseColor("#2E7D32"))
            scheme.setColor(EditorColorScheme.ANNOTATION,                 Color.parseColor("#B0681E"))
            scheme.setColor(EditorColorScheme.COMMENT,                    Color.parseColor("#888888"))
        }
        codeEditor?.setColorScheme(scheme)
    }

    /** Darken a packed ARGB colour by [factor] (0..1). */
    private fun darkenSurface(color: Int, factor: Float): Int {
        val a = (color shr 24) and 0xFF
        val r = maxOf(0, ((color shr 16 and 0xFF) * (1 - factor)).toInt())
        val g = maxOf(0, ((color shr  8 and 0xFF) * (1 - factor)).toInt())
        val b = maxOf(0, (( color       and 0xFF) * (1 - factor)).toInt())
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    // ─── View init ────────────────────────────────────────────────────────────

    private fun initViews() {
        codeEditor          = findViewById(R.id.code_editor)
        symbolInput         = findViewById(R.id.symbol_input)
        filenameText        = findViewById(R.id.textview_filename)
        lineNoEncodingText  = findViewById(R.id.textview_lineno_encoding)
        searchBar           = findViewById(R.id.search_bar)
        searchInput         = findViewById(R.id.search_input)
        searchCount         = findViewById(R.id.search_count)
        drawerLayout        = findViewById(R.id.editor_drawer_layout)
        openFilesRecycler   = findViewById(R.id.recycler_open_files)

        applyEditorSettings()

        codeEditor?.subscribeEvent(ContentChangeEvent::class.java,
            object : EventReceiver<ContentChangeEvent> {
                override fun onReceive(event: ContentChangeEvent, unsubscribe: Unsubscribe) {
                    if (!justSaved) { isModified = true; updateInfoBar() }
                    justSaved = false
                    handleUndoRedoState()
                    if (currentSyntax != "text") syntaxEngine.highlight(codeEditor!!, currentSyntax)
                }
            })

        codeEditor?.subscribeEvent(SelectionChangeEvent::class.java,
            object : EventReceiver<SelectionChangeEvent> {
                override fun onReceive(event: SelectionChangeEvent, unsubscribe: Unsubscribe) {
                    updateCursorPosition()
                    saveCursorPosition()
                }
            })

        symbolInput?.bindEditor(codeEditor)
        symbolInput?.addSymbols(
            arrayOf("\u2192", "/", "+", "-", "*", "=", "<", ">"),
            arrayOf("\u2192", "/", "+", "-", "*", "=", "<", ">")
        )
    }

    // ─── Drawer ───────────────────────────────────────────────────────────────

    private fun setupDrawer() {
        openFileAdapter = OpenFileAdapter { index ->
            switchToFile(index)
            drawerLayout?.closeDrawers()
        }
        openFilesRecycler?.layoutManager = LinearLayoutManager(this)
        openFilesRecycler?.adapter = openFileAdapter

        findViewById<Button>(R.id.btn_minimize)?.setOnClickListener {
            drawerLayout?.closeDrawers()
        }

        val toggle = ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close)
        drawerLayout?.addDrawerListener(toggle)
        toggle.syncState()

        // Wire up drawer header buttons
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
        findViewById<ImageView>(R.id.btn_menu).setOnClickListener {
            drawerLayout?.openDrawer(Gravity.START)
        }
        findViewById<ImageView>(R.id.btn_pin).setOnClickListener      { toggleSearchBar() }
        findViewById<ImageView>(R.id.btn_undo).setOnClickListener     { codeEditor?.undo() }
        findViewById<ImageView>(R.id.btn_redo).setOnClickListener     { codeEditor?.redo() }
        findViewById<ImageView>(R.id.btn_save).setOnClickListener     { saveFile() }
        findViewById<ImageView>(R.id.btn_edit_mode).setOnClickListener { showEditMenu(it) }
        findViewById<ImageView>(R.id.btn_overflow).setOnClickListener  { showOverflowMenu(it) }
    }

    // ─── Search ───────────────────────────────────────────────────────────────

    private fun setupSearchBar() {
        searchInput?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { performSearch(s?.toString() ?: "") }
        })
        findViewById<ImageView>(R.id.btn_search_prev).setOnClickListener  { searchNext(false) }
        findViewById<ImageView>(R.id.btn_search_next).setOnClickListener  { searchNext(true) }
        findViewById<ImageView>(R.id.btn_search_close).setOnClickListener { toggleSearchBar() }
    }

    private fun toggleSearchBar() {
        searchVisible = !searchVisible
        searchBar?.visibility = if (searchVisible) View.VISIBLE else View.GONE
        if (searchVisible) searchInput?.requestFocus() else searchInput?.setText("")
    }

    private var lastSearchQuery = ""

    private fun performSearch(query: String) {
        lastSearchQuery = query
        if (query.isEmpty()) { searchCount?.text = ""; return }
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

        if (forward) {
            val idx = text.indexOf(query, editor.cursor.right).takeIf { it >= 0 }
                ?: text.indexOf(query, 0)
            if (idx >= 0) {
                val lc = offsetToLineCol(editor, idx)
                editor.setSelectionRegion(lc.first, lc.second, lc.first, lc.second + query.length)
            }
        } else {
            val idx = text.lastIndexOf(query, maxOf(0, editor.cursor.left - 1)).takeIf { it >= 0 }
                ?: text.lastIndexOf(query)
            if (idx >= 0) {
                val lc = offsetToLineCol(editor, idx)
                editor.setSelectionRegion(lc.first, lc.second, lc.first, lc.second + query.length)
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

    // ─── Cursor / position history ────────────────────────────────────────────

    private fun saveCursorPosition() {
        val editor = codeEditor ?: return
        val pos = Pair(editor.cursor.leftLine, editor.cursor.leftColumn)
        if (currentFileIndex >= 0 && currentFileIndex < openFiles.size)
            filePositions[openFiles[currentFileIndex]] = pos
        if (positionIndex < positionHistory.size - 1)
            positionHistory = positionHistory.subList(0, positionIndex + 1).toMutableList()
        positionHistory.add(pos)
        positionIndex = positionHistory.size - 1
        if (positionHistory.size > 50) { positionHistory.removeAt(0); positionIndex-- }
    }

    private fun navigateToPreviousPosition() {
        if (positionIndex > 0) { positionIndex--; codeEditor?.setSelection(positionHistory[positionIndex].first, positionHistory[positionIndex].second) }
    }
    private fun navigateToNextPosition() {
        if (positionIndex < positionHistory.size - 1) { positionIndex++; codeEditor?.setSelection(positionHistory[positionIndex].first, positionHistory[positionIndex].second) }
    }

    // ─── Popup menus ─────────────────────────────────────────────────────────

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
        popup.menu.add(0,  1, 0,  "Search")
        popup.menu.add(0,  2, 1,  "Previous position").isEnabled = positionIndex > 0
        popup.menu.add(0,  3, 2,  "Next position").isEnabled     = positionIndex < positionHistory.size - 1
        popup.menu.add(0,  4, 3,  "Jump to line")

        val wrapItem  = popup.menu.add(0, 5, 4, "Soft wrap");   wrapItem.isCheckable  = true; wrapItem.isChecked  = prefs.getBoolean("word_wrap", false)
        val roItem    = popup.menu.add(0, 6, 5, "Read-only mode"); roItem.isCheckable  = true; roItem.isChecked    = isReadOnly
        val smItem    = popup.menu.add(0, 7, 6, "Smooth mode"); smItem.isCheckable    = true; smItem.isChecked    = isSmoothMode
        val ccItem    = popup.menu.add(0, 8, 7, "Code completion"); ccItem.isCheckable = true; ccItem.isChecked   = isCodeCompletion

        popup.menu.add(0,  9, 8,  "Syntax")
        popup.menu.add(0, 10, 9,  "Preferences")
        popup.menu.add(0, 11, 10, "Close file")

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1  -> { toggleSearchBar(); true }
                2  -> { navigateToPreviousPosition(); true }
                3  -> { navigateToNextPosition(); true }
                4  -> { showJumpToLineDialog(); true }
                5  -> { item.isChecked = !item.isChecked; prefs.edit().putBoolean("word_wrap", item.isChecked).apply(); codeEditor?.setWordwrap(item.isChecked); true }
                6  -> { item.isChecked = !item.isChecked; isReadOnly = item.isChecked; codeEditor?.setEditable(!isReadOnly); true }
                7  -> { item.isChecked = !item.isChecked; isSmoothMode = item.isChecked; true }
                8  -> { item.isChecked = !item.isChecked; isCodeCompletion = item.isChecked; true }
                9  -> { showSyntaxSelector(); true }
                10 -> { showPreferencesDialog(); true }
                11 -> { confirmClose(); true }
                else -> false
            }
        }
        popup.show()
    }

    // ─── Syntax / preferences dialogs ────────────────────────────────────────

    private fun showSyntaxSelector() { SyntaxSelectorFragment().show(supportFragmentManager, "syntax") }

    override fun onSyntaxSelected(syntaxName: String) {
        currentSyntax = syntaxName
        if (currentFileIndex >= 0 && currentFileIndex < openFiles.size)
            fileSyntaxes[openFiles[currentFileIndex]] = syntaxName
        loadLanguageForSyntax(syntaxName)
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
                input.text.toString().toIntOrNull()?.let { if (it > 0) codeEditor?.jumpToLine(it - 1) }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ─── Editor settings ──────────────────────────────────────────────────────

    private fun applyEditorSettings() {
        val editor = codeEditor ?: return
        editor.setTextSize(prefs.getInt("font_size", 14).toFloat())
        editor.setWordwrap(prefs.getBoolean("word_wrap", false))
        editor.setLineNumberEnabled(prefs.getBoolean("show_line_numbers", true))
        editor.setLineNumberMarginLeft(2f)
        editor.setLineSpacing(2.0f, 1.1f)
        editor.setHighlightCurrentLine(true)
        editor.setTypefaceText(Typeface.MONOSPACE)
        editor.setTypefaceLineNumber(Typeface.MONOSPACE)
        try { editor.getComponent(EditorTextActionWindow::class.java).setEnabled(false) } catch (_: Exception) {}
    }

    // ─── Undo / redo state ────────────────────────────────────────────────────

    private fun handleUndoRedoState() {
        val undoEnabled = codeEditor?.canUndo() == true
        val redoEnabled = codeEditor?.canRedo() == true
        // Dim icons when action unavailable
        val dimAlpha = 0.28f
        findViewById<ImageView>(R.id.btn_undo).alpha = if (undoEnabled) 1f else dimAlpha
        findViewById<ImageView>(R.id.btn_redo).alpha = if (redoEnabled) 1f else dimAlpha
    }

    // ─── Info bar ─────────────────────────────────────────────────────────────

    private fun updateCursorPosition() {
        val cursor = codeEditor?.cursor ?: return
        lineNoEncodingText?.text = "${cursor.leftLine + 1}:${cursor.leftColumn + 1}   UTF-8"
    }

    private fun updateInfoBar() { updateCursorPosition() }

    // ─── File I/O ─────────────────────────────────────────────────────────────

    private fun saveFile() {
        val path = currentFilePath ?: return
        val dialog = ProgressDialog.show(this, "Saving", "Writing file…", true)
        executor.execute {
            try {
                val content = codeEditor?.text?.toString() ?: ""
                Files.write(Paths.get(path), content.toByteArray(StandardCharsets.UTF_8))
                fileContents[path] = content
                mainHandler.post {
                    dialog.dismiss()
                    justSaved = true; isModified = false
                    updateInfoBar()
                    Toast.makeText(this@TextEditorActivity, "Saved", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                mainHandler.post { dialog.dismiss(); Toast.makeText(this@TextEditorActivity, "Save failed", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    // ─── Line editing helpers ────────────────────────────────────────────────

    private fun deleteCurrentLine() {
        val editor = codeEditor ?: return
        val line = editor.cursor.leftLine
        val lc   = editor.lineCount
        when {
            lc <= 1 -> { editor.setSelection(0, 0); editor.setText("") }
            line == lc - 1 -> { editor.setSelectionRegion(line-1, editor.text.getColumnCount(line-1), line, editor.text.getColumnCount(line)); editor.deleteText() }
            else           -> { editor.setSelectionRegion(line, 0, line+1, 0); editor.deleteText() }
        }
    }

    private fun duplicateCurrentLine() {
        val editor = codeEditor ?: return
        val line = editor.cursor.leftLine
        editor.text.insert(line, editor.text.getColumnCount(line), "\n${editor.text.getLineString(line)}")
    }

    private fun toggleComment() {
        val editor = codeEditor ?: return
        val line = editor.cursor.leftLine
        val s = editor.text.getLineString(line)
        val i = s.indexOfFirst { !it.isWhitespace() }
        if (i < 0) return
        if (s[i] == '#') {
            val end = if (i + 1 < s.length && s[i + 1] == ' ') i + 2 else i + 1
            editor.setSelectionRegion(line, i, line, end); editor.deleteText()
        } else {
            editor.setSelection(line, i); editor.commitText("# ")
        }
    }

    private fun indentLine() {
        val editor = codeEditor ?: return
        editor.setSelection(editor.cursor.leftLine, 0); editor.commitText("    ")
    }

    private fun unindentLine() {
        val editor = codeEditor ?: return
        val line = editor.cursor.leftLine
        val s = editor.text.getLineString(line)
        when {
            s.startsWith("    ") -> { editor.setSelectionRegion(line, 0, line, 4); editor.deleteText() }
            s.startsWith("\t")   -> { editor.setSelectionRegion(line, 0, line, 1); editor.deleteText() }
        }
    }

    // ─── Multi-file management ───────────────────────────────────────────────

    private fun openFile(path: String) {
        val existing = openFiles.indexOf(path)
        if (existing >= 0) { switchToFile(existing); return }
        openFiles.add(path)
        currentFileIndex = openFiles.size - 1
        currentFilePath  = path
        filenameText?.text = File(path).name
        detectSyntaxAndLoad()
        updateDrawerList()
    }

    private fun switchToFile(index: Int) {
        if (index < 0 || index >= openFiles.size || index == currentFileIndex) return
        if (currentFileIndex >= 0 && currentFileIndex < openFiles.size) {
            val old = openFiles[currentFileIndex]
            fileContents[old] = codeEditor?.text?.toString() ?: ""
            val c = codeEditor?.cursor
            if (c != null) filePositions[old] = Pair(c.leftLine, c.leftColumn)
        }
        currentFileIndex = index
        val path = openFiles[index]
        currentFilePath = path
        currentSyntax   = fileSyntaxes[path] ?: detectSyntaxForFile(path)
        filenameText?.text = File(path).name
        val content = fileContents[path]
        if (content != null) {
            codeEditor?.setText(content)
            filePositions[path]?.let { codeEditor?.setSelection(it.first, it.second) }
            loadLanguageForSyntax(currentSyntax)
            isModified = false; justSaved = true
            updateInfoBar(); handleUndoRedoState()
        } else {
            loadFile()
        }
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

    private fun detectSyntaxForFile(path: String): String {
        syntaxEngine.getSyntaxForFile(path)?.let { return it }
        return when (path.substringAfterLast(".", "").lowercase()) {
            "java"              -> "java"
            "kt"                -> "kotlin"
            "py"                -> "python"
            "js"                -> "javascript"
            "html", "htm"       -> "html"
            "css"               -> "css"
            "xml"               -> "xml"
            "json"              -> "json"
            "sh", "bash"        -> "shell"
            "smali"             -> "smali"
            else                -> "text"
        }
    }

    private fun detectSyntaxAndLoad() {
        val path = currentFilePath ?: return
        currentSyntax = detectSyntaxForFile(path)
        if (currentFileIndex >= 0 && currentFileIndex < openFiles.size)
            fileSyntaxes[path] = currentSyntax
        loadFile()
    }

    private fun loadFile() {
        val path = currentFilePath ?: return
        val dialog = ProgressDialog.show(this, "Loading", "Reading file…", true)
        executor.execute {
            try {
                val sb = StringBuilder()
                BufferedReader(FileReader(path)).use { br ->
                    var line: String?
                    while (br.readLine().also { line = it } != null) sb.append(line).append("\n")
                }
                val content = sb.toString()
                mainHandler.post {
                    dialog.dismiss()
                    codeEditor?.setText(content)
                    fileContents[path] = content
                    loadLanguageForSyntax(currentSyntax)
                    isModified = false; justSaved = true
                    updateInfoBar(); handleUndoRedoState()
                }
            } catch (e: Exception) {
                mainHandler.post { dialog.dismiss(); Toast.makeText(this@TextEditorActivity, "Failed to load file", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun loadLanguageForSyntax(syntax: String) {
        val editor = codeEditor ?: return
        if (!prefs.getBoolean("syntax_highlighting", true) || syntax == "text") {
            editor.setEditorLanguage(EmptyLanguage()); return
        }
        try {
            if (syntaxEngine.getDef(syntax) != null) {
                editor.setEditorLanguage(EmptyLanguage())
                syntaxEngine.highlight(editor, syntax)
            } else {
                editor.setEditorLanguage(EmptyLanguage())
            }
        } catch (_: Exception) { editor.setEditorLanguage(EmptyLanguage()) }
    }

    // ─── Close guard ─────────────────────────────────────────────────────────

    private fun confirmClose() {
        if (isModified) {
            AlertDialog.Builder(this)
                .setTitle("Unsaved Changes")
                .setMessage("Do you want to save the changes?")
                .setPositiveButton("Save")    { _, _ -> saveAndFinish() }
                .setNegativeButton("Discard") { _, _ -> finish() }
                .setNeutralButton("Cancel", null)
                .show()
        } else {
            finish()
        }
    }

    private fun saveAndFinish() { saveFile(); finish() }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (drawerLayout?.isDrawerOpen(Gravity.START) == true) drawerLayout?.closeDrawers()
        else confirmClose()
    }

    // ─── Static helpers ───────────────────────────────────────────────────────

    companion object {
        @JvmStatic
        fun start(context: Activity, filePath: String) {
            context.startActivity(
                Intent(context, TextEditorActivity::class.java)
                    .putExtra("file_path", filePath)
            )
        }
    }
}
