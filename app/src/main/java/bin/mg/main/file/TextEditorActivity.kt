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
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import android.widget.ImageButton
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import bin.mg.editor.rendering.view.CodeEditorView
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
    private var codeEditor: CodeEditorView? = null
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
    private lateinit var syntaxEngine: MmsxSyntaxEngine
    private var syntaxLoadPending = false

    private val prefs by lazy { getSharedPreferences("editor_prefs", MODE_PRIVATE) }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_editor)

        syntaxEngine = MmsxSyntaxEngine(this)
        syntaxEngine.onSyntaxesLoaded = {
            if (syntaxLoadPending) {
                syntaxLoadPending = false
                val path = currentFilePath
                if (path != null) {
                    currentSyntax = syntaxEngine.getSyntaxForFile(path) ?: detectSyntaxFallback(path)
                    fileSyntaxes[path] = currentSyntax
                    syntaxEngine.highlight(codeEditor!!, currentSyntax)
                }
            }
        }
        executor.execute { syntaxEngine.loadAllSyntaxes() }

        initViews()
        applyTheme()
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

        val toolbarIconTint = 0xFFFFFFFF.toInt()
        val filenameColor   = if (isDark) 0xFFCCCCCC.toInt() else ThemeManager.lighten(primary, 0.80f)
        val statsColor      = ThemeManager.statsText(this)
        val symbolBarBg     = if (isDark) 0xFF1A1A1A.toInt() else ThemeManager.lighten(primary, 0.94f)
        val searchBg        = if (isDark) 0xFF1E1E1E.toInt() else ThemeManager.lighten(primary, 0.96f)
        val searchTextColor = if (isDark) 0xFFDDDDDD.toInt() else 0xFF222222.toInt()
        val searchHintColor = if (isDark) 0xFF666666.toInt() else ThemeManager.lighten(primary, 0.55f)
        val searchIconTint  = if (isDark) 0xFF888888.toInt() else ThemeManager.lighten(primary, 0.45f)
        val editorBodyBg    = if (isDark) 0xFF1E1E1E.toInt() else 0xFFFAFAFA.toInt()

        val mainContent = findViewById<LinearLayout>(R.id.editor_main_content)
        mainContent?.setBackgroundColor(editorBodyBg)

        val toolbar = findViewById<LinearLayout>(R.id.editor_toolbar)
        toolbar?.setBackgroundColor(toolbarBg)

        for (id in listOf(R.id.btn_menu, R.id.btn_pin, R.id.btn_undo, R.id.btn_redo,
                          R.id.btn_save, R.id.btn_edit_mode, R.id.btn_overflow)) {
            findViewById<ImageView>(id)?.setColorFilter(toolbarIconTint)
        }

        val infoBar = findViewById<LinearLayout>(R.id.editor_info_bar)
        infoBar?.setBackgroundColor(infoBg)
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

        val symbolBar = findViewById<LinearLayout>(R.id.symbol_bar)
        symbolBar?.setBackgroundColor(symbolBarBg)

        val drawer = findViewById<LinearLayout>(R.id.editor_drawer)
        val drawerBodyBg = if (isDark) 0xFF242424.toInt() else 0xFFFFFFFF.toInt()
        drawer?.setBackgroundColor(drawerBodyBg)

        val drawerHeader = findViewById<LinearLayout>(R.id.drawer_header)
        drawerHeader?.setBackgroundColor(toolbarBg)

        for (id in listOf(R.id.btn_drawer_edit, R.id.btn_drawer_overflow)) {
            findViewById<ImageView>(id)?.setColorFilter(toolbarIconTint)
        }

        // Apply editor colors to our custom CodeEditorView
        applyEditorColors(isDark)
    }

    private fun applyEditorColors(isDark: Boolean) {
        val editor = codeEditor ?: return
        if (isDark) {
            editor.renderer.backgroundColor = Color.parseColor("#1E1E1E")
            editor.renderer.textColor = Color.parseColor("#A9B7C6")
            editor.renderer.lineNumberColor = Color.parseColor("#606366")
            editor.renderer.lineNumberCurrentColor = Color.parseColor("#A0A0A0")
            editor.renderer.gutterBgColor = Color.parseColor("#1E1E1E")
            editor.renderer.currentLineColor = Color.parseColor("#2A2D2E")
            editor.renderer.selectionColor = Color.parseColor("#214283")
            editor.renderer.cursorColor = Color.parseColor("#A9B7C6")
            editor.renderer.separatorColor = Color.parseColor("#333333")
        } else {
            editor.renderer.backgroundColor = Color.parseColor("#FAFAFA")
            editor.renderer.textColor = Color.parseColor("#212121")
            editor.renderer.lineNumberColor = Color.parseColor("#AAAAAA")
            editor.renderer.lineNumberCurrentColor = Color.parseColor("#666666")
            editor.renderer.gutterBgColor = Color.parseColor("#F5F5F5")
            editor.renderer.currentLineColor = Color.parseColor("#F0F4FF")
            editor.renderer.selectionColor = Color.parseColor("#BBDEFB")
            editor.renderer.cursorColor = Color.parseColor("#212121")
            editor.renderer.separatorColor = Color.parseColor("#E0E0E0")
        }
    }

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

        // Wire symbol bar buttons
        setupSymbolBar()

        // Content change callback
        codeEditor?.onContentChanged = { _ ->
            if (!justSaved && !syntaxEngine.isHighlighting) {
                isModified = true
                updateInfoBar()
            }
            justSaved = false
            handleUndoRedoState()
            if (currentSyntax != "text" && !syntaxEngine.isHighlighting) {
                syntaxEngine.highlight(codeEditor!!, currentSyntax)
            }
        }

        // Cursor move callback
        codeEditor?.onCursorMoved = { line, col ->
            lineNoEncodingText?.text = "${line + 1}:${col + 1}   UTF-8"
            saveCursorPosition()
        }

        // Keyboard visibility listener
        setupKeyboardVisibility()

        filenameText?.setOnClickListener {
            currentFilePath?.let { path ->
                val clip = android.content.ClipData.newPlainText("filename", File(path).name)
                getSystemService(android.content.ClipboardManager::class.java)?.setPrimaryClip(clip)
                Toast.makeText(this, "Filename copied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupKeyboardVisibility() {
        val rootView = findViewById<View>(android.R.id.content)
        rootView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val heightDiff = rootView.rootView.height - rootView.height
                val isKeyboardVisible = heightDiff > 150
                val symbolBar = findViewById<LinearLayout>(R.id.symbol_bar) ?: return
                symbolBar.visibility = if (isKeyboardVisible) View.GONE else View.VISIBLE
            }
        })
    }

    // ─── Symbol Drawer (bottom sheet with 3 rows) ──────────────────────────────

    private var symbolBarExpanded = false
    private lateinit var symbolRow1: android.widget.LinearLayout
    private lateinit var symbolRow2: android.widget.LinearLayout
    private lateinit var symbolRow3: android.widget.LinearLayout
    private var symbolDragStartY = 0f

    private fun setupSymbolBar() {
        val container = symbolInput ?: return
        container.removeAllViews()
        container.orientation = android.widget.LinearLayout.VERTICAL
        container.setPadding(0, 0, 0, 0)

        val isDark = ThemeManager.isDarkMode(this)
        val textColor = if (isDark) 0xFFCCCCCC.toInt() else 0xFF333333.toInt()
        val bgColor = if (isDark) 0xFF1A1A1A.toInt() else 0xFFE8E8E8.toInt()
        val dividerColor = if (isDark) 0xFF333333.toInt() else 0xFFCCCCCC.toInt()
        val handleColor = if (isDark) 0xFF666666.toInt() else 0xFF999999.toInt()

        container.setBackgroundColor(bgColor)

        // Drag handle indicator at top
        val handle = View(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(40.dp, 4.dp).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = 6.dp
                bottomMargin = 4.dp
            }
            setBackgroundColor(handleColor)
            val radius = 2.dp.toFloat()
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                setColor(handleColor)
                cornerRadius = radius
            }
        }
        container.addView(handle)

        // Drag handle touch area (larger touch target)
        val handleTouch = View(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                24.dp
            )
            setOnTouchListener { _, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        symbolDragStartY = event.rawY
                        true
                    }
                    android.view.MotionEvent.ACTION_UP -> {
                        val dy = symbolDragStartY - event.rawY
                        if (dy > 30.dp) {
                            // Swiped up → expand
                            expandSymbolBar()
                        } else if (dy < -30.dp) {
                            // Swiped down → collapse
                            collapseSymbolBar()
                        }
                        true
                    }
                    else -> false
                }
            }
        }
        container.addView(handleTouch)

        // Row 1: → / + - * = <  (→ = tab)
        val row1Symbols = arrayOf("\u2192", "/", "+", "-", "*", "=", "<")
        val row1Actions = arrayOf<Runnable?>({
            codeEditor?.buffer?.insertText("\t")
            codeEditor?.requestFocus()
        }, null, null, null, null, null, null)
        symbolRow1 = createSymbolRow(row1Symbols, row1Actions, textColor, dividerColor)
        container.addView(symbolRow1)

        // Row 2: > " ' ; | \ _
        val row2Symbols = arrayOf(">", "\"", "'", ";", "|", "\\", "_")
        symbolRow2 = createSymbolRow(row2Symbols, textColor, dividerColor)
        symbolRow2.visibility = View.GONE
        container.addView(symbolRow2)

        // Row 3: ( ) [ ] { } ...
        val row3Symbols = arrayOf("(", ")", "[", "]", "{", "}", "...")
        symbolRow3 = createSymbolRow(row3Symbols, textColor, dividerColor)
        symbolRow3.visibility = View.GONE
        container.addView(symbolRow3)

        // Divider at bottom
        val bottomDivider = View(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1
            )
            setBackgroundColor(dividerColor)
        }
        container.addView(bottomDivider)
    }

    private fun expandSymbolBar() {
        if (symbolBarExpanded) return
        symbolBarExpanded = true
        symbolRow2.visibility = View.VISIBLE
        symbolRow3.visibility = View.VISIBLE
        symbolInput?.invalidate()
    }

    private fun collapseSymbolBar() {
        if (!symbolBarExpanded) return
        symbolBarExpanded = false
        symbolRow2.visibility = View.GONE
        symbolRow3.visibility = View.GONE
        symbolInput?.invalidate()
    }

    private fun createSymbolRow(symbols: Array<String>, actions: Array<Runnable?>? = null, textColor: Int, dividerColor: Int): android.widget.LinearLayout {
        val row = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                40.dp
            )
        }

        for ((index, sym) in symbols.withIndex()) {
            val btn = android.widget.TextView(this).apply {
                text = sym
                setTextColor(textColor)
                textSize = 16f
                typeface = Typeface.MONOSPACE
                gravity = Gravity.CENTER
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    0,
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    1f
                )
                isClickable = true
                isFocusable = true
                setBackgroundResource(android.R.drawable.list_selector_background)
                setOnClickListener {
                    val custom = actions?.getOrNull(index)
                    if (custom != null) {
                        custom.run()
                    } else {
                        codeEditor?.buffer?.insertText(sym)
                        codeEditor?.requestFocus()
                    }
                }
            }
            row.addView(btn)

            if (index < symbols.size - 1) {
                val divider = View(this).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        1,
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(dividerColor)
                }
                row.addView(divider)
            }
        }

        return row
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()

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
            val text = codeEditor?.getText() ?: return@execute
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
        val text = editor.getText()

        if (forward) {
            val idx = text.indexOf(query, editor.buffer.cursorManager.cursor.column).takeIf { it >= 0 }
                ?: text.indexOf(query, 0)
            if (idx >= 0) {
                val line = editor.buffer.offsetToLine(idx)
                val col = editor.buffer.offsetToColumn(idx)
                editor.setSelection(line, col, line, col + query.length)
            }
        } else {
            val idx = text.lastIndexOf(query, maxOf(0, editor.buffer.cursorManager.cursor.column - 1)).takeIf { it >= 0 }
                ?: text.lastIndexOf(query)
            if (idx >= 0) {
                val line = editor.buffer.offsetToLine(idx)
                val col = editor.buffer.offsetToColumn(idx)
                editor.setSelection(line, col, line, col + query.length)
            }
        }
    }

    // ─── Cursor / position history ────────────────────────────────────────────

    private fun saveCursorPosition() {
        val editor = codeEditor ?: return
        val cursor = editor.buffer.cursorManager.cursor
        val pos = Pair(cursor.line, cursor.column)
        if (currentFileIndex >= 0 && currentFileIndex < openFiles.size)
            filePositions[openFiles[currentFileIndex]] = pos
        if (positionIndex < positionHistory.size - 1)
            positionHistory = positionHistory.subList(0, positionIndex + 1).toMutableList()
        positionHistory.add(pos)
        positionIndex = positionHistory.size - 1
        if (positionHistory.size > 50) { positionHistory.removeAt(0); positionIndex-- }
    }

    private fun navigateToPreviousPosition() {
        if (positionIndex > 0) {
            positionIndex--
            val pos = positionHistory[positionIndex]
            codeEditor?.setCursorPosition(pos.first, pos.second)
        }
    }
    private fun navigateToNextPosition() {
        if (positionIndex < positionHistory.size - 1) {
            positionIndex++
            val pos = positionHistory[positionIndex]
            codeEditor?.setCursorPosition(pos.first, pos.second)
        }
    }

    // ─── Popup menus ─────────────────────────────────────────────────────────

    private fun showEditMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, "Copy line")
        popup.menu.add(0, 2, 1, "Cut line")
        popup.menu.add(0, 3, 2, "Delete line")
        popup.menu.add(0, 4, 3, "Empty line")
        popup.menu.add(0, 5, 4, "Replace line")
        popup.menu.add(0, 6, 5, "Duplicate line")
        popup.menu.add(0, 7, 6, "Convert to uppercase")
        popup.menu.add(0, 8, 7, "Convert to lowercase")
        popup.menu.add(0, 9, 8, "Increase indent")
        popup.menu.add(0, 10, 9, "Decrease indent")
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> { copyCurrentLine(); true }
                2 -> { cutCurrentLine(); true }
                3 -> { deleteCurrentLine(); true }
                4 -> { emptyCurrentLine(); true }
                5 -> { replaceCurrentLine(); true }
                6 -> { duplicateCurrentLine(); true }
                7 -> { convertCase(true); true }
                8 -> { convertCase(false); true }
                9 -> { indentLine(); true }
                10 -> { unindentLine(); true }
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
                5  -> { item.isChecked = !item.isChecked; prefs.edit().putBoolean("word_wrap", item.isChecked).apply(); codeEditor?.setWordWrap(item.isChecked); true }
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
        syntaxEngine.highlight(codeEditor!!, currentSyntax)
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
                input.text.toString().toIntOrNull()?.let { if (it > 0) codeEditor?.scrollToLine(it - 1) }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ─── Editor settings ──────────────────────────────────────────────────────

    private fun applyEditorSettings() {
        val editor = codeEditor ?: return
        editor.setFontSize(prefs.getInt("font_size", 14).toFloat())
        editor.setWordWrap(prefs.getBoolean("word_wrap", false))
        editor.setLineNumbers(prefs.getBoolean("show_line_numbers", true))
        editor.setLineSpacing(2.0f, 1.1f)
    }

    // ─── Undo / redo state ────────────────────────────────────────────────────

    private fun handleUndoRedoState() {
        val undoEnabled = codeEditor?.canUndo() == true
        val redoEnabled = codeEditor?.canRedo() == true
        val dimAlpha = 0.28f
        findViewById<ImageView>(R.id.btn_undo).alpha = if (undoEnabled) 1f else dimAlpha
        findViewById<ImageView>(R.id.btn_redo).alpha = if (redoEnabled) 1f else dimAlpha
        findViewById<ImageView>(R.id.btn_save).alpha = if (isModified) 1f else dimAlpha
        updateFilenameTab()
    }

    // ─── Info bar ─────────────────────────────────────────────────────────────

    private fun updateCursorPosition() {
        val cursor = codeEditor?.buffer?.cursorManager?.cursor ?: return
        lineNoEncodingText?.text = "${cursor.line + 1}:${cursor.column + 1}   UTF-8"
    }

    private fun updateInfoBar() { updateCursorPosition(); updateFilenameTab() }

    private fun updateFilenameTab() {
        val name = currentFilePath?.let { File(it).name } ?: "untitled"
        filenameText?.text = if (isModified) "*$name" else name
    }

    // ─── File I/O ─────────────────────────────────────────────────────────────

    private fun saveFile() {
        val path = currentFilePath ?: return
        val dialog = ProgressDialog.show(this, "Saving", "Writing file…", true)
        executor.execute {
            try {
                val content = codeEditor?.getText() ?: ""
                Files.write(Paths.get(path), content.toByteArray(StandardCharsets.UTF_8))
                fileContents[path] = content
                mainHandler.post {
                    dialog.dismiss()
                    justSaved = true; isModified = false
                    handleUndoRedoState()
                    updateInfoBar()
                    Toast.makeText(this@TextEditorActivity, "Saved", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                mainHandler.post { dialog.dismiss(); Toast.makeText(this@TextEditorActivity, "Save failed", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    // ─── Line editing helpers ────────────────────────────────────────────────

    private fun copyCurrentLine() {
        val editor = codeEditor ?: return
        val buf = editor.buffer
        val line = buf.cursorManager.cursor.line
        val lineText = buf.getLineText(line)
        val clip = android.content.ClipData.newPlainText("line", lineText)
        getSystemService(android.content.ClipboardManager::class.java)?.setPrimaryClip(clip)
        Toast.makeText(this, "Line copied", Toast.LENGTH_SHORT).show()
    }

    private fun cutCurrentLine() {
        val editor = codeEditor ?: return
        val buf = editor.buffer
        val line = buf.cursorManager.cursor.line
        val lineText = buf.getLineText(line)
        val clip = android.content.ClipData.newPlainText("line", lineText)
        getSystemService(android.content.ClipboardManager::class.java)?.setPrimaryClip(clip)
        deleteCurrentLine()
    }

    private fun deleteCurrentLine() {
        val editor = codeEditor ?: return
        val buf = editor.buffer
        val line = buf.cursorManager.cursor.line
        val lineCount = buf.getLineCount()
        when {
            lineCount <= 1 -> {
                editor.setText("")
            }
            line == lineCount - 1 -> {
                val prevLine = line - 1
                val prevLen = buf.getLineText(prevLine).length
                val lineStart = buf.lineStartOffset(line)
                val remaining = buf.length() - lineStart
                if (remaining > 0) {
                    buf.cursorManager.moveTo(prevLine, prevLen)
                    buf.delete(lineStart, remaining)
                }
            }
            else -> {
                val lineStart = buf.lineStartOffset(line)
                val nextStart = buf.lineStartOffset(line + 1)
                val remaining = nextStart - lineStart
                if (remaining > 0) {
                    buf.delete(lineStart, remaining)
                }
            }
        }
        editor.invalidate()
        handleUndoRedoState()
    }

    private fun emptyCurrentLine() {
        val editor = codeEditor ?: return
        val buf = editor.buffer
        val line = buf.cursorManager.cursor.line
        val lineStart = buf.lineStartOffset(line)
        val lineText = buf.getLineText(line)
        if (lineText.isNotEmpty()) {
            buf.delete(lineStart, lineText.length)
            buf.cursorManager.moveTo(line, 0)
        }
        editor.invalidate()
        handleUndoRedoState()
    }

    private fun replaceCurrentLine() {
        val editor = codeEditor ?: return
        val buf = editor.buffer
        val line = buf.cursorManager.cursor.line
        val input = EditText(this)
        input.hint = "New line content"
        input.setText(buf.getLineText(line))
        input.selectAll()
        AlertDialog.Builder(this)
            .setTitle("Replace line")
            .setView(input)
            .setPositiveButton("Replace") { _, _ ->
                val newText = input.text.toString()
                val lineStart = buf.lineStartOffset(line)
                val oldLen = buf.getLineText(line).length
                if (oldLen > 0) buf.delete(lineStart, oldLen)
                buf.insert(lineStart, newText)
                buf.cursorManager.moveTo(line, newText.length)
                editor.invalidate()
                handleUndoRedoState()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun duplicateCurrentLine() {
        val editor = codeEditor ?: return
        val buf = editor.buffer
        val line = buf.cursorManager.cursor.line
        val lineText = buf.getLineText(line)
        val lineStart = buf.lineStartOffset(line)
        val insertAt = lineStart + lineText.length
        buf.insert(insertAt, "\n$lineText")
        editor.invalidate()
    }

    private fun convertCase(toUpper: Boolean) {
        val editor = codeEditor ?: return
        val buf = editor.buffer
        val selection = buf.cursorManager.selection

        if (selection.isValid && !selection.isCollapsed()) {
            val s = selection.normalizedStart()
            val e = selection.normalizedEnd()
            val startOff = buf.lineColumnToOffset(s.line, s.column)
            val endOff = buf.lineColumnToOffset(e.line, e.column)
            val len = endOff - startOff
            val selected = buf.substring(startOff, len)
            val changed = if (toUpper) selected.uppercase() else selected.lowercase()
            buf.replace(startOff, len, changed)
        } else {
            val line = buf.cursorManager.cursor.line
            val lineText = buf.getLineText(line)
            val lineStart = buf.lineStartOffset(line)
            val changed = if (toUpper) lineText.uppercase() else lineText.lowercase()
            buf.replace(lineStart, lineText.length, changed)
        }
        editor.invalidate()
        handleUndoRedoState()
    }

    private fun indentLine() {
        val editor = codeEditor ?: return
        val buf = editor.buffer
        val line = buf.cursorManager.cursor.line
        val lineStart = buf.lineStartOffset(line)
        buf.insert(lineStart, "    ")
        editor.invalidate()
    }

    private fun unindentLine() {
        val editor = codeEditor ?: return
        val buf = editor.buffer
        val line = buf.cursorManager.cursor.line
        val lineText = buf.getLineText(line)
        val lineStart = buf.lineStartOffset(line)
        if (lineText.startsWith("    ")) {
            buf.delete(lineStart, 4)
        } else if (lineText.startsWith("\t")) {
            buf.delete(lineStart, 1)
        }
        editor.invalidate()
    }

    // ─── Multi-file management ───────────────────────────────────────────────

    private fun openFile(path: String) {
        val existing = openFiles.indexOf(path)
        if (existing >= 0) { switchToFile(existing); return }
        if (currentFileIndex >= 0 && currentFileIndex < openFiles.size) {
            val old = openFiles[currentFileIndex]
            fileContents[old] = codeEditor?.getText() ?: ""
            val c = codeEditor?.buffer?.cursorManager?.cursor
            if (c != null) filePositions[old] = Pair(c.line, c.column)
            fileSyntaxes[old] = currentSyntax
        }
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
            fileContents[old] = codeEditor?.getText() ?: ""
            val c = codeEditor?.buffer?.cursorManager?.cursor
            if (c != null) filePositions[old] = Pair(c.line, c.column)
            fileSyntaxes[old] = currentSyntax
        }
        currentFileIndex = index
        val path = openFiles[index]
        currentFilePath = path
        currentSyntax = fileSyntaxes[path] ?: detectSyntaxForFile(path)
        filenameText?.text = File(path).name
        justSaved = true
        val content = fileContents[path]
        if (content != null) {
            codeEditor?.setText(content)
            filePositions[path]?.let { pos ->
                val lineCount = codeEditor?.buffer?.getLineCount() ?: 0
                if (pos.first < lineCount) {
                    val maxCol = codeEditor?.buffer?.getLineText(pos.first)?.length ?: 0
                    codeEditor?.setCursorPosition(pos.first, pos.second.coerceAtMost(maxCol))
                }
            }
            isModified = false
            handleUndoRedoState()
            updateInfoBar()
            syntaxEngine.highlight(codeEditor!!, currentSyntax)
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
        return detectSyntaxFallback(path)
    }

    private fun detectSyntaxFallback(path: String): String {
        return when (path.substringAfterLast(".", "").lowercase()) {
            "java"              -> "java"
            "kt", "kts"         -> "kotlin"
            "py", "pyw"         -> "python"
            "js", "es", "mjs"   -> "javascript"
            "ts", "tsx"          -> "typescript"
            "html", "htm"       -> "html"
            "css", "scss", "less" -> "css"
            "xml", "svg"        -> "xml"
            "json"              -> "json"
            "sh", "bash", "zsh" -> "shell"
            "c", "h"            -> "c"
            "cpp", "cc", "cxx", "hpp" -> "cpp"
            "cs"                -> "cs"
            "go"                -> "go"
            "rs"                -> "rust"
            "swift"             -> "swift"
            "rb"                -> "ruby"
            "php"               -> "php"
            "sql"               -> "sql"
            "lua"               -> "lua"
            "dart"              -> "dart"
            "groovy"            -> "groovy"
            "toml"              -> "toml"
            "yml", "yaml"       -> "yml"
            "md", "markdown"    -> "markdown"
            "smali"             -> "smali"
            "nix"               -> "nix"
            "zig"               -> "zig"
            "bat", "cmd"        -> "bat"
            "diff"              -> "diff"
            "glsl", "hlsl"      -> "glsl"
            "asm", "s", "S"     -> "asm"
            else                -> "text"
        }
    }

    private fun detectSyntaxAndLoad() {
        val path = currentFilePath ?: return
        if (syntaxEngine.isLoaded) {
            currentSyntax = detectSyntaxForFile(path)
            fileSyntaxes[path] = currentSyntax
            loadFile()
        } else {
            currentSyntax = detectSyntaxFallback(path)
            fileSyntaxes[path] = currentSyntax
            syntaxLoadPending = true
            loadFile()
        }
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
                    justSaved = true
                    codeEditor?.setText(content)
                    fileContents[path] = content
                    isModified = false
                    handleUndoRedoState()
                    updateInfoBar()
                    syntaxEngine.highlight(codeEditor!!, currentSyntax)
                }
            } catch (e: Exception) {
                mainHandler.post { dialog.dismiss(); Toast.makeText(this@TextEditorActivity, "Failed to load file", Toast.LENGTH_SHORT).show() }
            }
        }
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
