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

    // ─── Symbol bar (bottom bar with keyboard awareness) ──────────────────────

    private var symbolBarExpanded = false
    private var isKeyboardVisible = false
    private lateinit var symbolRow1: LinearLayout
    private lateinit var symbolRow2: LinearLayout
    private lateinit var symbolRow3: LinearLayout
    private lateinit var symbolBarContainer: LinearLayout
    private var symbolDragStartY = 0f

    private fun setupSymbolBar() {
        val container = symbolInput ?: return
        container.removeAllViews()

        val isDark = ThemeManager.isDarkMode(this)
        val textColor = if (isDark) 0xFFCCCCCC.toInt() else 0xFF333333.toInt()
        val bgColor = if (isDark) 0xFF1A1A1A.toInt() else 0xFFE8E8E8.toInt()
        val dividerColor = if (isDark) 0xFF333333.toInt() else 0xFFCCCCCC.toInt()
        val handleColor = if (isDark) 0xFF666666.toInt() else 0xFF999999.toInt()

        // Main vertical container
        symbolBarContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgColor)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        container.addView(symbolBarContainer)

        // Drag handle area at top (only used when keyboard is hidden to expand)
        val handleArea = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (12 * resources.displayMetrics.density).toInt()
            )
            setOnTouchListener { _, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        symbolDragStartY = event.rawY
                        true
                    }
                    android.view.MotionEvent.ACTION_UP -> {
                        val dy = symbolDragStartY - event.rawY
                        if (dy > 40 && !symbolBarExpanded && !isKeyboardVisible) {
                            expandSymbolBar()
                        } else if (dy < -40 && symbolBarExpanded) {
                            collapseSymbolBar()
                        }
                        true
                    }
                    else -> false
                }
            }
        }
        symbolBarContainer.addView(handleArea)

        // Handle bar visual (small line indicator)
        val handleBar = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                (40 * resources.displayMetrics.density).toInt(),
                (3 * resources.displayMetrics.density).toInt()
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = (4 * resources.displayMetrics.density).toInt()
                bottomMargin = (4 * resources.displayMetrics.density).toInt()
            }
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                setColor(handleColor)
                cornerRadius = (2 * resources.displayMetrics.density)
            }
        }
        symbolBarContainer.addView(handleBar)

        // Row 1: → / + - * = <   (always visible)
        val row1Symbols = arrayOf("\u2192", "/", "+", "-", "*", "=", "<")
        val row1Inserts = arrayOf("\t", "/", "+", "-", "*", "=", "<")
        symbolRow1 = createSymbolRow(row1Symbols, row1Inserts, textColor, dividerColor)
        symbolBarContainer.addView(symbolRow1)

        // Row 2: > " ' ; | \ -   (visible when keyboard open or expanded)
        val row2Symbols = arrayOf(">", "\"", "'", ";", "|", "\\", "-")
        val row2Inserts = arrayOf(">", "\"", "'", ";", "|", "\\", "-")
        symbolRow2 = createSymbolRow(row2Symbols, row2Inserts, textColor, dividerColor)
        symbolRow2.visibility = View.GONE
        symbolBarContainer.addView(symbolRow2)

        // Row 3: () [] {} ...     (visible when keyboard open or expanded)
        val row3Symbols = arrayOf("()", "[]", "{}", "...")
        val row3Inserts = arrayOf("()", "[]", "{}", "...")
        symbolRow3 = createSymbolRow(row3Symbols, row3Inserts, textColor, dividerColor)
        symbolRow3.visibility = View.GONE
        symbolBarContainer.addView(symbolRow3)

        // Watch keyboard visibility
        setupKeyboardListener()
    }

    private fun setupKeyboardListener() {
        val rootView = findViewById<View>(android.R.id.content)
        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = android.graphics.Rect()
            rootView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = rootView.rootView.height
            val visibleHeight = rect.height()
            val heightDiff = screenHeight - visibleHeight
            val keyboardVisible = heightDiff > screenHeight * 0.15

            if (keyboardVisible != isKeyboardVisible) {
                isKeyboardVisible = keyboardVisible
                updateSymbolBarForKeyboard()
            }
        }
    }

    private fun updateSymbolBarForKeyboard() {
        if (isKeyboardVisible) {
            // Keyboard visible: show all 3 rows above keyboard
            symbolRow2.visibility = View.VISIBLE
            symbolRow3.visibility = View.VISIBLE
        } else {
            // Keyboard hidden: show row 1, optionally rows 2 and 3 if expanded
            symbolRow2.visibility = if (symbolBarExpanded) View.VISIBLE else View.GONE
            symbolRow3.visibility = if (symbolBarExpanded) View.VISIBLE else View.GONE
        }
    }

    private fun expandSymbolBar() {
        if (symbolBarExpanded) return
        symbolBarExpanded = true
        symbolRow2.visibility = View.VISIBLE
        symbolRow3.visibility = View.VISIBLE
    }

    private fun collapseSymbolBar() {
        if (!symbolBarExpanded) return
        symbolBarExpanded = false
        symbolRow2.visibility = View.GONE
        symbolRow3.visibility = View.GONE
    }

    private fun createSymbolRow(
        symbols: Array<String>,
        insertTexts: Array<String>,
        textColor: Int,
        dividerColor: Int
    ): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (44 * resources.displayMetrics.density).toInt()
            )
        }

        for ((index, sym) in symbols.withIndex()) {
            val btn = TextView(this).apply {
                text = sym
                setTextColor(textColor)
                textSize = 15f
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
                    codeEditor?.insertText(insertTexts[index])
                    codeEditor?.requestFocus()
                }
            }
            row.addView(btn)

            if (index < symbols.size - 1) {
                val divider = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        1,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(dividerColor)
                }
                row.addView(divider)
            }
        }

        return row
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
        val lineNoEncoding = lineNoEncodingText ?: return
        val line = codeEditor?.getCursorLine() ?: 1
        val col = codeEditor?.getCursorColumn() ?: 1
        val encoding = mDefaultCharset?.name()?.uppercase() ?: "UTF-8"
        lineNoEncoding.text = "$line:$col   $encoding"
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
        // Line operations
        popup.menu.add(0, 10, 0, "Copy line")
        popup.menu.add(0, 11, 1, "Cut line")
        popup.menu.add(0, 12, 2, "Delete line")
        popup.menu.add(0, 13, 3, "Empty line")
        popup.menu.add(0, 14, 4, "Replace line")
        popup.menu.add(0, 15, 5, "Duplicate line")
        // Case conversion
        popup.menu.add(0, 20, 6, "Convert to uppercase")
        popup.menu.add(0, 21, 7, "Convert to lowercase")
        // Indentation
        popup.menu.add(0, 30, 8, "Increase indent")
        popup.menu.add(0, 31, 9, "Decrease indent")
        // Comment
        popup.menu.add(0, 40, 10, "Toggle comment")
        // Classic clipboard
        popup.menu.add(0, 50, 11, "Select all")
        popup.menu.add(0, 51, 12, "Paste")
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                10 -> { codeEditor?.copyLine(); true }
                11 -> { codeEditor?.cutLine(); true }
                12 -> { codeEditor?.deleteLine(); true }
                13 -> { codeEditor?.emptyLine(); true }
                14 -> { codeEditor?.replaceLine(); true }
                15 -> { codeEditor?.duplicateLine(); true }
                20 -> { codeEditor?.convertToUppercase(); true }
                21 -> { codeEditor?.convertToLowercase(); true }
                30 -> { codeEditor?.increaseIndent(); true }
                31 -> { codeEditor?.decreaseIndent(); true }
                40 -> { codeEditor?.toggleComment(getCommentPrefixForSyntax(currentSyntax)); true }
                50 -> { codeEditor?.selectAll(); true }
                51 -> { codeEditor?.paste(); true }
                else -> false
            }
        }
        popup.show()
    }

    /** Returns the appropriate single-line comment prefix for the current syntax. */
    private fun getCommentPrefixForSyntax(syntax: String): String = when (syntax) {
        "java", "xml", "smali" -> "//"
        "python", "smali" -> "#"
        "html" -> "<!--"   // simplified — block comments in HTML, kept short
        else -> "//"
    }

    private fun showOverflowMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, "Search")
        popup.menu.add(0, 2, 1, "Syntax")
        val prevLabel = "Previous position"
        val nextLabel = "Next position"
        popup.menu.add(0, 3, 2, prevLabel).isEnabled = positionIndex > 0
        popup.menu.add(0, 4, 3, nextLabel).isEnabled = positionIndex < positionHistory.size - 1
        popup.menu.add(0, 5, 4, "Jump to line")
        // Toggle items — show current state in title
        val wrapState  = if (codeEditor?.isWordWrapEnabled == true) "✓" else " "
        val roState    = if (codeEditor?.isReadOnly == true)        "✓" else " "
        val smoothState = if (codeEditor?.isSmoothScrollEnabled == true) "✓" else " "
        val acState    = if (codeEditor?.isAutoCompleteEnabled == true)  "✓" else " "
        popup.menu.add(0, 6,  5, "[$wrapState]  Soft wrap")
        popup.menu.add(0, 7,  6, "[$roState]    Read-only mode")
        popup.menu.add(0, 8,  7, "[$smoothState] Smooth mode")
        popup.menu.add(0, 9,  8, "[$acState]    Code completion")
        popup.menu.add(0, 10, 9, "Preferences")
        popup.menu.add(0, 11, 10, "Close file")
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1  -> { toggleSearchBar(); true }
                2  -> { showSyntaxSelector(); true }
                3  -> { navigatePositionBack(); true }
                4  -> { navigatePositionForward(); true }
                5  -> { showJumpToLineDialog(); true }
                6  -> { toggleWordWrap(); true }
                7  -> { toggleReadOnly(); true }
                8  -> { toggleSmoothMode(); true }
                9  -> { toggleCodeCompletion(); true }
                10 -> { showPreferencesDialog(); true }
                11 -> { confirmClose(); true }
                else -> false
            }
        }
        popup.show()
    }

    // ─── Editor mode toggles ──────────────────────────────────────────────────

    private fun toggleWordWrap() {
        val enabled = codeEditor?.isWordWrapEnabled != true
        codeEditor?.setWordWrap(enabled)
        Toast.makeText(this, if (enabled) "Soft wrap ON" else "Soft wrap OFF", Toast.LENGTH_SHORT).show()
    }

    private fun toggleReadOnly() {
        val readOnly = codeEditor?.isReadOnly != true
        codeEditor?.setReadOnly(readOnly)
        isReadOnly = readOnly
        // Dim the save/edit buttons when in read-only mode
        handleUndoRedoState()
        Toast.makeText(this, if (readOnly) "Read-only ON" else "Read-only OFF", Toast.LENGTH_SHORT).show()
    }

    private fun toggleSmoothMode() {
        val smooth = codeEditor?.isSmoothScrollEnabled != true
        codeEditor?.setSmoothScrollEnabled(smooth)
        Toast.makeText(this, if (smooth) "Smooth mode ON" else "Smooth mode OFF", Toast.LENGTH_SHORT).show()
    }

    private fun toggleCodeCompletion() {
        val enabled = codeEditor?.isAutoCompleteEnabled != true
        codeEditor?.setAutoCompleteEnabled(enabled)
        Toast.makeText(this, if (enabled) "Code completion ON" else "Code completion OFF", Toast.LENGTH_SHORT).show()
    }

    // ─── Position history navigation ─────────────────────────────────────────

    private fun pushCurrentPosition() {
        val line = codeEditor?.getCursorLine() ?: return
        val col  = codeEditor?.getCursorColumn() ?: return
        // Trim forward history on new push
        if (positionIndex < positionHistory.size - 1) {
            positionHistory = positionHistory.subList(0, positionIndex + 1).toMutableList()
        }
        positionHistory.add(Pair(line, col))
        positionIndex = positionHistory.size - 1
    }

    private fun navigatePositionBack() {
        if (positionIndex > 0) {
            positionIndex--
            val (line, _) = positionHistory[positionIndex]
            codeEditor?.gotoLine(line - 1)
        }
    }

    private fun navigatePositionForward() {
        if (positionIndex < positionHistory.size - 1) {
            positionIndex++
            val (line, _) = positionHistory[positionIndex]
            codeEditor?.gotoLine(line - 1)
        }
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
                input.text.toString().toIntOrNull()?.let { line ->
                    if (line > 0) {
                        pushCurrentPosition()   // remember where we came from
                        codeEditor?.gotoLine(line - 1)
                        pushCurrentPosition()   // record the destination too
                    }
                }
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
                    codeEditor?.requestFocus()
                    codeEditor?.showSoftInput(true)
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
