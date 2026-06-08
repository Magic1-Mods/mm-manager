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
import android.view.Menu
import android.view.View
import android.view.WindowManager
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
import bin.mg.main.editor.EditView
import bin.mg.main.editor.OnTextChangedListener
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
    SyntaxSelectorFragment.OnSyntaxSelectedListener {

    private var codeEditor: EditView? = null
    private var symbolPanel: SymbolPanel? = null
    private var filenameText: TextView? = null
    private var lineNoEncodingText: TextView? = null
    private var searchBar: LinearLayout? = null
    private var searchInput: EditText? = null
    private var searchCount: TextView? = null
    private var drawerLayout: DrawerLayout? = null
    private var openFilesRecycler: RecyclerView? = null

    private var currentFilePath: String? = null
    private var isModified = false
    private var isReadOnly = false
    private var currentSyntax = "text"
    private var searchVisible = false
    private var mDefaultCharset: Charset = StandardCharsets.UTF_8
    private var positionHistory = mutableListOf<Pair<Int, Int>>()
    private var positionIndex = -1

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_editor)

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        initViews()
        applyTheme()
        setupDrawer()
        setupToolbar()
        setupSearchBar()
        setupSymbolPanel()

        val intentPath = intent.getStringExtra("file_path")
        if (intentPath != null && File(intentPath).exists()) {
            openFile(intentPath)
        } else {
            codeEditor?.setText("// No file loaded\n// Tap the menu to open a file")
            filenameText?.text = "untitled"
        }
    }

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

        symbolPanel?.setBackgroundColor(symbolBarBg)

        findViewById<LinearLayout>(R.id.editor_drawer)?.setBackgroundColor(drawerBg)
        findViewById<LinearLayout>(R.id.drawer_header)?.setBackgroundColor(toolbarBg)
        for (id in listOf(R.id.btn_drawer_edit, R.id.btn_drawer_overflow)) {
            findViewById<ImageView>(id)?.setColorFilter(toolbarIconTint)
        }

        codeEditor?.setSyntaxDarkMode(isDark)
    }

    private fun initViews() {
        codeEditor = findViewById(R.id.code_editor)
        symbolPanel = findViewById(R.id.symbol_panel)
        filenameText = findViewById(R.id.textview_filename)
        lineNoEncodingText = findViewById(R.id.textview_lineno_encoding)
        searchBar = findViewById(R.id.search_bar)
        searchInput = findViewById(R.id.search_input)
        searchCount = findViewById(R.id.search_count)
        drawerLayout = findViewById(R.id.editor_drawer_layout)
        openFilesRecycler = findViewById(R.id.recycler_open_files)

        applyEditorSettings()

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

        val fontStyle = prefs.getString("font_style", "monospace") ?: "monospace"
        val typeface = if (fontStyle == "monospace") Typeface.MONOSPACE else Typeface.DEFAULT
        editor.setTypeface(typeface)

        editor.setTextSize(prefs.getInt("font_size", 14).toFloat())

        editor.setWordWrap(prefs.getBoolean("soft_wrap", false))
        editor.setReadOnly(prefs.getBoolean("read_only", false))
        editor.setSmoothScrollEnabled(prefs.getBoolean("smooth_mode", true))
        editor.setAutoCompleteEnabled(prefs.getBoolean("code_completion", true))
        editor.setMagnifierEnabled(prefs.getBoolean("enable_magnifier", true))
        editor.setAutoIndentEnabled(prefs.getBoolean("auto_indent", true))
    }

    private fun setupSymbolPanel() {
        symbolPanel?.setOnSymbolClickListener(object : SymbolPanel.OnSymbolClickListener {
            override fun onSymbolClick(symbol: String) {
                codeEditor?.insertText(symbol)
                codeEditor?.requestFocus()
            }
        })
    }

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

    private fun showEditMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        addMenuItem(popup, 10, "Copy line", R.drawable.ic_copy_line)
        addMenuItem(popup, 11, "Cut line", R.drawable.ic_cut_line)
        addMenuItem(popup, 12, "Delete line", R.drawable.ic_delete_line)
        addMenuItem(popup, 13, "Empty line", R.drawable.ic_empty_line)
        addMenuItem(popup, 14, "Replace line", R.drawable.ic_replace_line)
        addMenuItem(popup, 15, "Duplicate line", R.drawable.ic_duplicate_line)
        addMenuItem(popup, 20, "Convert to uppercase", R.drawable.ic_uppercase)
        addMenuItem(popup, 21, "Convert to lowercase", R.drawable.ic_lowercase)
        addMenuItem(popup, 30, "Increase indent", R.drawable.ic_indent_increase)
        addMenuItem(popup, 31, "Decrease indent", R.drawable.ic_indent_decrease)
        addMenuItem(popup, 40, "Toggle comment", R.drawable.ic_toggle_comment)
        addMenuItem(popup, 41, "Reformat code", R.drawable.ic_reformat_code)
        addMenuItem(popup, 50, "Select all", null)
        addMenuItem(popup, 51, "Paste", null)

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
                41 -> { Toast.makeText(this, "Reformat (TODO)", Toast.LENGTH_SHORT).show(); true }
                50 -> { codeEditor?.selectAll(); true }
                51 -> { codeEditor?.paste(); true }
                else -> false
            }
        }
        popup.show()
    }

    private fun addMenuItem(popup: PopupMenu, id: Int, title: String, iconRes: Int?) {
        val item = popup.menu.add(0, id, Menu.NONE, title)
        if (iconRes != null) {
            item.setIcon(iconRes)
        }
    }

    private fun getCommentPrefixForSyntax(syntax: String): String = when (syntax) {
        "java", "kotlin", "javascript", "typescript", "c", "cpp", "rust",
        "go", "swift", "dart", "scala", "groovy", "gradle",
        "css", "php" -> "//"
        "python", "ruby", "perl", "r", "julia", "shell", "yaml", "toml",
        "sql", "lua", "smali" -> if (syntax == "lua") "--" else "#"
        "html", "xml" -> "<!--"
        else -> "//"
    }

    private fun showOverflowMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        val fileSubmenu = popup.menu.addSubMenu(0, 100, 0, "File")
        fileSubmenu.add(0, 101, 0, "Save")

        addMenuItem(popup, 2, "Search", R.drawable.ic_search)
        addMenuItem(popup, 3, "Syntax", R.drawable.ic_syntax)
        addMenuItem(popup, 4, "Previous position", R.drawable.ic_navigate_before)
        addMenuItem(popup, 5, "Next position", R.drawable.ic_navigate_next)
        addMenuItem(popup, 6, "Jump to line", R.drawable.ic_jump_to_line)

        val softWrapItem = popup.menu.add(0, 7, 7, "Soft wrap")
        softWrapItem.setCheckable(true)
        softWrapItem.setChecked(codeEditor?.isWordWrapEnabled == true)

        val readOnlyItem = popup.menu.add(0, 8, 8, "Read-only mode")
        readOnlyItem.setCheckable(true)
        readOnlyItem.setChecked(codeEditor?.isReadOnly == true)

        val smoothItem = popup.menu.add(0, 9, 9, "Smooth mode")
        smoothItem.setCheckable(true)
        smoothItem.setChecked(codeEditor?.isSmoothScrollEnabled == true)

        val codeCompItem = popup.menu.add(0, 10, 10, "Code completion")
        codeCompItem.setCheckable(true)
        codeCompItem.setChecked(codeEditor?.isAutoCompleteEnabled == true)

        addMenuItem(popup, 11, "Preferences", R.drawable.ic_settings)
        addMenuItem(popup, 12, "Close file", R.drawable.ic_close_file)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                101 -> { saveFile(); true }
                2  -> { toggleSearchBar(); true }
                3  -> { showSyntaxSelector(); true }
                4  -> { navigatePositionBack(); true }
                5  -> { navigatePositionForward(); true }
                6  -> { showJumpToLineDialog(); true }
                7  -> { toggleWordWrap(); true }
                8  -> { toggleReadOnly(); true }
                9  -> { toggleSmoothMode(); true }
                10 -> { toggleCodeCompletion(); true }
                11 -> { showPreferences(); true }
                12 -> { confirmClose(); true }
                else -> false
            }
        }
        popup.show()
    }

    private fun toggleWordWrap() {
        val enabled = codeEditor?.isWordWrapEnabled != true
        codeEditor?.setWordWrap(enabled)
        prefs.edit().putBoolean("soft_wrap", enabled).apply()
    }

    private fun toggleReadOnly() {
        val readOnly = codeEditor?.isReadOnly != true
        codeEditor?.setReadOnly(readOnly)
        isReadOnly = readOnly
        prefs.edit().putBoolean("read_only", readOnly).apply()
        handleUndoRedoState()
    }

    private fun toggleSmoothMode() {
        val smooth = codeEditor?.isSmoothScrollEnabled != true
        codeEditor?.setSmoothScrollEnabled(smooth)
        prefs.edit().putBoolean("smooth_mode", smooth).apply()
    }

    private fun toggleCodeCompletion() {
        val enabled = codeEditor?.isAutoCompleteEnabled != true
        codeEditor?.setAutoCompleteEnabled(enabled)
        prefs.edit().putBoolean("code_completion", enabled).apply()
    }

    private fun pushCurrentPosition() {
        val line = codeEditor?.getCursorLine() ?: return
        val col  = codeEditor?.getCursorColumn() ?: return
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

    private fun showSyntaxSelector() { SyntaxSelectorFragment().show(supportFragmentManager, "syntax") }

    override fun onSyntaxSelected(syntaxName: String) {
        currentSyntax = syntaxName
        if (currentFileIndex >= 0 && currentFileIndex < openFiles.size)
            fileSyntaxes[openFiles[currentFileIndex]] = syntaxName
        codeEditor?.setSyntaxLanguageFileName(syntaxToFileName(syntaxName))
    }

    private fun syntaxToFileName(syntax: String): String? = when (syntax) {
        "java" -> "java.json"
        "xml" -> "xml.json"
        "smali" -> "smali.json"
        "kotlin" -> "kotlin.json"
        "python" -> "python.json"
        "javascript" -> "javascript.json"
        "typescript" -> "typescript.json"
        "html" -> "html.json"
        "css" -> "css.json"
        "json" -> "json.json"
        "yaml" -> "yaml.json"
        "toml" -> "toml.json"
        "shell" -> "shell.json"
        "sql" -> "sql.json"
        "markdown" -> "markdown.json"
        "c" -> "c.json"
        "cpp" -> "cpp.json"
        "rust" -> "rust.json"
        "go" -> "go.json"
        "swift" -> "swift.json"
        "lua" -> "lua.json"
        "dart" -> "dart.json"
        "php" -> "php.json"
        "ruby" -> "ruby.json"
        "perl" -> "perl.json"
        "scala" -> "scala.json"
        "groovy" -> "groovy.json"
        "r" -> "r.json"
        "julia" -> "julia.json"
        "gradle" -> "gradle.json"
        else -> null
    }

    private fun showPreferences() {
        startActivity(Intent(this, PreferencesActivity::class.java))
    }

    override fun onResume() {
        super.onResume()
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
                input.text.toString().toIntOrNull()?.let { line ->
                    if (line > 0) {
                        pushCurrentPosition()
                        codeEditor?.gotoLine(line - 1)
                        pushCurrentPosition()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

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
            "smali", "class" -> "smali"
            "xml", "html", "htm", "xhtml", "plist", "kml" -> "xml"
            "java", "jsp" -> "java"
            "kt", "kts", "ktm" -> "kotlin"
            "py", "pyw", "pyx", "pyi" -> "python"
            "js", "jsx", "mjs", "cjs" -> "javascript"
            "ts", "tsx", "mts", "cts" -> "typescript"
            "css", "scss", "less", "sass" -> "css"
            "json", "jsonc", "json5" -> "json"
            "yaml", "yml" -> "yaml"
            "toml" -> "toml"
            "sh", "bash", "zsh", "fish", "ksh" -> "shell"
            "sql", "ddl", "dml" -> "sql"
            "md", "markdown", "mdown" -> "markdown"
            "c", "h", "i" -> "c"
            "cpp", "cc", "cxx", "c++", "hpp", "hh", "hxx", "h++", "ipp" -> "cpp"
            "rs", "rlib" -> "rust"
            "go" -> "go"
            "swift" -> "swift"
            "lua", "luac" -> "lua"
            "dart" -> "dart"
            "php", "phtml", "php3", "php4", "php5", "php7", "php8" -> "php"
            "rb", "ruby", "erb", "rake" -> "ruby"
            "pl", "pm", "t" -> "perl"
            "scala", "sc" -> "scala"
            "groovy", "gvy", "gy", "gsh" -> "groovy"
            "r", "R", "rmd", "Rmd" -> "r"
            "jl" -> "julia"
            "gradle" -> "gradle"
            else -> "text"
        }
    }

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

    companion object {
        private const val MAX_FILE_SIZE_BYTES = 3L * 1024 * 1024

        @JvmStatic
        fun start(context: Activity, filePath: String) {
            context.startActivity(
                Intent(context, TextEditorActivity::class.java)
                    .putExtra("file_path", filePath)
            )
        }
    }
}
