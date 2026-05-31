package bin.mg.main

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.provider.Settings
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import bin.mg.main.model.FileItem
import bin.mg.main.ui.adapter.FileAdapter
import bin.mg.main.ui.editor.FileEditorActivity
import bin.mg.main.utils.file.FileSystemHelper
import java.io.File
import java.util.Locale
import java.util.Stack
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

class MainActivity : AppCompatActivity() {

    private var toolbarPath: TextView? = null
    private var topStats: TextView? = null
    private var diskInfo: TextView? = null
    private lateinit var recyclerLeft: RecyclerView
    private lateinit var recyclerRight: RecyclerView
    private var panelLeft: View? = null
    private var panelRight: View? = null
    private var btnBack: ImageButton? = null
    private var btnForward: ImageButton? = null
    private var btnNew: ImageButton? = null
    private var btnSwap: ImageButton? = null
    private var btnParent: ImageButton? = null
    private var drawerLayout: DrawerLayout? = null
    private var toolbar: Toolbar? = null
    private var drawerToggle: ActionBarDrawerToggle? = null

    private var sectionLocalContent: LinearLayout? = null
    private var sectionToolsContent: LinearLayout? = null
    private var sectionLocalArrow: ImageView? = null
    private var sectionToolsArrow: ImageView? = null
    private var rootProgress: ProgressBar? = null
    private var storageProgress: ProgressBar? = null
    private var rootStorageText: TextView? = null
    private var storageText: TextView? = null
    private var dynamicStorageContainer: LinearLayout? = null
    private var btnThemeToggle: ImageButton? = null
    private var btnDrawerMenu: ImageButton? = null

    private lateinit var adapterLeft: FileAdapter
    private lateinit var adapterRight: FileAdapter
    private var currentPathLeft = DEFAULT_PATH
    private var currentPathRight = DEFAULT_PATH
    private var activePanel = 0

    private val backStackLeft = Stack<String>()
    private val forwardStackLeft = Stack<String>()
    private val backStackRight = Stack<String>()
    private val forwardStackRight = Stack<String>()

    private val executor = Executors.newFixedThreadPool(2)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val lastClickTime = AtomicLong(0)

    @Volatile
    private var loadingPathLeft: String? = null
    @Volatile
    private var loadingPathRight: String? = null

    private var currentPalette = 0
    private var followSystemTheme = false
    private var currentPrimaryColor = 0xFF212121.toInt()
    private var buttonTint = 0xFF424242.toInt()
    private val customPaths = ArrayList<String>()
    private val greyColorFilter = android.graphics.PorterDuffColorFilter(0xFF9E9E9E.toInt(), android.graphics.PorterDuff.Mode.SRC_IN)

    private fun getPrimaryColor(): Int = PALETTES[currentPalette][0]
    private fun getSecondaryColor(): Int = PALETTES[currentPalette][1]

    private fun lightenColor(color: Int, factor: Float): Int {
        val a = (color shr 24) and 0xFF
        var r = (color shr 16) and 0xFF
        var g = (color shr 8) and 0xFF
        var b = color and 0xFF
        r = minOf(255, (r + (255 - r) * factor).toInt())
        g = minOf(255, (g + (255 - g) * factor).toInt())
        b = minOf(255, (b + (255 - b) * factor).toInt())
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun darkenColor(color: Int, factor: Float): Int {
        val a = (color shr 24) and 0xFF
        var r = (color shr 16) and 0xFF
        var g = (color shr 8) and 0xFF
        var b = color and 0xFF
        r = maxOf(0, (r * (1 - factor)).toInt())
        g = maxOf(0, (g * (1 - factor)).toInt())
        b = maxOf(0, (b * (1 - factor)).toInt())
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun getThemeColor(attrName: String, defaultColor: Int): Int {
        val primary = getPrimaryColor()
        val secondary = getSecondaryColor()

        return when (attrName) {
            "drawerBackground" -> lightenColor(primary, 0.92f)
            "headerBackground" -> primary
            "iconTint" -> 0xFFFFFFFF.toInt()
            "iconTintSecondary" -> lightenColor(primary, 0.6f)
            "textPrimary" -> darkenColor(primary, 0.5f)
            "textSecondary" -> lightenColor(primary, 0.35f)
            "sectionText" -> secondary
            "dividerColor" -> lightenColor(primary, 0.82f)
            "progressTint" -> secondary
            "mainBackground" -> lightenColor(primary, 0.92f)
            "toolbarBackground" -> primary
            "panelBg" -> 0xFFFFFFFF.toInt()
            "bottomBarBackground" -> 0xFFFFFFFF.toInt()
            "statsTextColor" -> lightenColor(primary, 0.5f)
            "buttonTint" -> darkenColor(primary, 0.3f)
            else -> defaultColor
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        loadThemePalette()
        applyTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initToolbar()
        initViews()
        initDrawerSections()
        registerFileHandlers()
        setupAdapters()
        setupListeners()
        loadCustomPaths()
        populateDynamicStorage()
        loadState()
        applyThemeColors()

        if (!hasPermissions()) {
            requestPermissions()
        } else {
            loadBothPanels()
            updateStorageInfo()
        }

        setActivePanel(0)
    }

    private fun loadThemePalette() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        currentPalette = prefs.getInt(KEY_THEME_PALETTE, 0)
        followSystemTheme = prefs.getBoolean(KEY_FOLLOW_SYSTEM_THEME, false)
    }

    private fun applyTheme() {
        if (followSystemTheme) {
            val currentNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
            if (currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                setTheme(R.style.AppTheme_Dark)
            } else {
                try {
                    val themeRes = R.style::class.java.getField(THEMES[currentPalette]).getInt(null)
                    setTheme(themeRes)
                } catch (e: Exception) {
                    setTheme(R.style.AppTheme)
                }
            }
        } else {
            try {
                val themeRes = R.style::class.java.getField(THEMES[currentPalette]).getInt(null)
                setTheme(themeRes)
            } catch (e: Exception) {
                setTheme(R.style.AppTheme)
            }
        }
    }

    private fun loadCustomPaths() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val pathsJson = prefs.getString(KEY_CUSTOM_PATHS, "")
        if (!pathsJson.isNullOrEmpty()) {
            val paths = pathsJson.split("|||")
            for (path in paths) {
                if (path.isNotEmpty()) {
                    customPaths.add(path)
                }
            }
        }
    }

    private fun saveCustomPaths() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val sb = StringBuilder()
        for (i in customPaths.indices) {
            if (i > 0) sb.append("|||")
            sb.append(customPaths[i])
        }
        prefs.edit().putString(KEY_CUSTOM_PATHS, sb.toString()).apply()
    }

    private fun toggleTheme() {
        currentPalette = (currentPalette + 1) % PALETTES.size
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit().putInt(KEY_THEME_PALETTE, currentPalette).apply()
        applyThemeColors()
        populateDynamicStorage()
    }

    private fun applyThemeColors() {
        val isDarkMode = followSystemTheme &&
                (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                        == android.content.res.Configuration.UI_MODE_NIGHT_YES)

        val mainBg: Int
        val panelBg: Int
        val bottomBarBg: Int
        val textPrimary: Int
        val textSecondary: Int
        val statsText: Int
        val dividerColor: Int

        if (isDarkMode) {
            currentPrimaryColor = 0xFF212121.toInt()
            mainBg = 0xFF212121.toInt()
            panelBg = 0xFF303030.toInt()
            bottomBarBg = 0xFF303030.toInt()
            textPrimary = 0xFFFFFFFF.toInt()
            textSecondary = 0xFFB0B0B0.toInt()
            statsText = 0xFF9E9E9E.toInt()
            buttonTint = 0xFFE0E0E0.toInt()
            dividerColor = 0xFF424242.toInt()
        } else {
            currentPrimaryColor = getPrimaryColor()
            mainBg = 0xFFF5F5F5.toInt()
            panelBg = 0xFFFFFFFF.toInt()
            bottomBarBg = 0xFFFFFFFF.toInt()
            textPrimary = 0xFF212121.toInt()
            textSecondary = 0xFF757575.toInt()
            statsText = 0xFF9E9E9E.toInt()
            buttonTint = 0xFF424242.toInt()
            dividerColor = 0xFFE0E0E0.toInt()
        }

        val window: Window = window
        window.statusBarColor = currentPrimaryColor

        findViewById<View?>(R.id.toolbar)?.setBackgroundColor(currentPrimaryColor)
        findViewById<LinearLayout?>(R.id.main_container)?.setBackgroundColor(mainBg)
        findViewById<FrameLayout?>(R.id.panel_left)?.setBackgroundColor(panelBg)
        findViewById<FrameLayout?>(R.id.panel_right)?.setBackgroundColor(panelBg)
        findViewById<View?>(R.id.divider)?.setBackgroundColor(dividerColor)
        findViewById<LinearLayout?>(R.id.bottom_bar)?.setBackgroundColor(bottomBarBg)

        findViewById<TextView?>(R.id.toolbar_path)?.setTextColor(0xFFFFFFFF.toInt())
        findViewById<TextView?>(R.id.stats_left)?.setTextColor(statsText)
        findViewById<TextView?>(R.id.disk_info)?.setTextColor(statsText)

        findViewById<ImageButton?>(R.id.btn_new)?.setColorFilter(buttonTint)
        findViewById<ImageButton?>(R.id.btn_swap)?.setColorFilter(buttonTint)
        findViewById<ImageButton?>(R.id.btn_parent)?.setColorFilter(buttonTint)

        if (::adapterLeft.isInitialized) adapterLeft.setThemeColor(currentPrimaryColor, isDarkMode)
        if (::adapterRight.isInitialized) adapterRight.setThemeColor(currentPrimaryColor, isDarkMode)
        updateDrawerColors(currentPrimaryColor, isDarkMode)
    }

    private fun updateDrawerColors(primaryColor: Int, isDarkMode: Boolean) {
        val drawerView = findViewById<View?>(R.id.nav_drawer) ?: return

        val textPrimary: Int
        val textSecondary: Int
        if (isDarkMode) {
            textPrimary = 0xFFFFFFFF.toInt()
            textSecondary = 0xFFB0B0B0.toInt()
        } else {
            textPrimary = 0xFF212121.toInt()
            textSecondary = 0xFF757575.toInt()
        }

        findViewById<View?>(R.id.header_background)?.setBackgroundColor(primaryColor)

        val appNameView = findViewById<TextView?>(R.id.header_app_name)
        if (appNameView != null) {
            appNameView.setTextColor(0xFFFFFFFF.toInt())
            val appNameStr = getString(R.string.app_name)
            val versionStr = BuildConfig.VERSION_NAME
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                appNameView.text = Html.fromHtml(
                    "$appNameStr <font color='#99FFFFFF'><small>v$versionStr</small></font>",
                    Html.FROM_HTML_MODE_COMPACT
                )
            } else {
                @Suppress("DEPRECATION")
                appNameView.text = Html.fromHtml(
                    "$appNameStr <font color='#99FFFFFF'><small>v$versionStr</small></font>"
                )
            }
        }

        val iconTint = 0xFFFFFFFF.toInt()
        findViewById<ImageButton?>(R.id.btn_theme_toggle)?.setColorFilter(iconTint)
        findViewById<ImageButton?>(R.id.btn_drawer_menu)?.setColorFilter(iconTint)

        findViewById<TextView?>(R.id.root_storage_text)?.setTextColor(textSecondary)
        findViewById<TextView?>(R.id.storage_text)?.setTextColor(textSecondary)

        updateDrawerIconColors(primaryColor)
    }

    private fun updateDrawerIconColors(primaryColor: Int) {
        val bgIds = intArrayOf(
            R.id.icon_root_bg,
            R.id.icon_storage_bg,
            R.id.icon_recycle_bin_bg,
            R.id.icon_plugins_bg,
            R.id.icon_screen_color_bg,
            R.id.icon_signing_key_bg,
            R.id.icon_passwords_bg,
            R.id.icon_text_editor_bg,
            R.id.icon_terminal_bg,
            R.id.icon_settings_bg,
            R.id.icon_about_bg
        )

        for (id in bgIds) {
            val v = findViewById<View?>(id) ?: continue
            val bg = v.background
            if (bg is GradientDrawable) {
                val shape = bg.mutate() as GradientDrawable
                shape.setColor(primaryColor)
            }
        }
    }

    private fun loadState() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val savedLeft = prefs.getString(KEY_PATH_LEFT, DEFAULT_PATH) ?: DEFAULT_PATH
        val savedRight = prefs.getString(KEY_PATH_RIGHT, DEFAULT_PATH) ?: DEFAULT_PATH
        val leftDir = File(savedLeft)
        val rightDir = File(savedRight)
        if (leftDir.isDirectory && leftDir.canRead()) {
            currentPathLeft = savedLeft
        }
        if (rightDir.isDirectory && rightDir.canRead()) {
            currentPathRight = savedRight
        }
    }

    private fun saveState() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_PATH_LEFT, currentPathLeft)
            .putString(KEY_PATH_RIGHT, currentPathRight)
            .putInt(KEY_SCROLL_LEFT, getCurrentScrollPosition(true))
            .putInt(KEY_SCROLL_RIGHT, getCurrentScrollPosition(false))
            .apply()
    }

    private fun getCurrentScrollPosition(isLeft: Boolean): Int {
        val recycler = if (isLeft) recyclerLeft else recyclerRight
        val lm = recycler.layoutManager as? LinearLayoutManager
        return lm?.findFirstVisibleItemPosition() ?: 0
    }

    private fun initToolbar() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        drawerLayout = findViewById(R.id.drawer_layout)
        drawerToggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout?.addDrawerListener(drawerToggle!!)
        drawerToggle?.syncState()

    }

    private fun initViews() {
        toolbarPath = findViewById(R.id.toolbar_path)
        topStats = findViewById(R.id.stats_left)
        diskInfo = findViewById(R.id.disk_info)
        recyclerLeft = findViewById(R.id.list_left)
        recyclerRight = findViewById(R.id.list_right)
        panelLeft = findViewById(R.id.panel_left)
        panelRight = findViewById(R.id.panel_right)

        btnBack = findViewById(R.id.btn_back)
        btnForward = findViewById(R.id.btn_forward)
        btnNew = findViewById(R.id.btn_new)
        btnSwap = findViewById(R.id.btn_swap)
        btnParent = findViewById(R.id.btn_parent)

        rootProgress = findViewById(R.id.root_progress)
        storageProgress = findViewById(R.id.storage_progress)
        rootStorageText = findViewById(R.id.root_storage_text)
        storageText = findViewById(R.id.storage_text)
        dynamicStorageContainer = findViewById(R.id.dynamic_storage_container)

        btnThemeToggle = findViewById(R.id.btn_theme_toggle)
        btnDrawerMenu = findViewById(R.id.btn_drawer_menu)
    }

    private fun initDrawerSections() {
        val sectionLocalHeader = findViewById<LinearLayout>(R.id.section_local_header)
        sectionLocalContent = findViewById(R.id.section_local_content)
        sectionLocalArrow = findViewById(R.id.section_local_arrow)

        val sectionToolsHeader = findViewById<LinearLayout>(R.id.section_tools_header)
        sectionToolsContent = findViewById(R.id.section_tools_content)
        sectionToolsArrow = findViewById(R.id.section_tools_arrow)

        sectionLocalHeader.setOnClickListener { toggleSection(sectionLocalContent!!, sectionLocalArrow!!) }
        sectionToolsHeader.setOnClickListener { toggleSection(sectionToolsContent!!, sectionToolsArrow!!) }

        findViewById<View>(R.id.nav_root_directory).setOnClickListener {
            drawerLayout?.closeDrawer(GravityCompat.START)
            navigateToPath("/", activePanel)
        }

        findViewById<View>(R.id.nav_storage_directory).setOnClickListener {
            drawerLayout?.closeDrawer(GravityCompat.START)
            navigateToPath(DEFAULT_PATH, activePanel)
        }

        findViewById<View>(R.id.nav_settings).setOnClickListener {
            drawerLayout?.closeDrawer(GravityCompat.START)
            Toast.makeText(this, "Settings not implemented", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.nav_about).setOnClickListener {
            drawerLayout?.closeDrawer(GravityCompat.START)
            showAboutDialog()
        }

        findViewById<View>(R.id.nav_plugins).setOnClickListener {
            drawerLayout?.closeDrawer(GravityCompat.START)
            Toast.makeText(this, "Plugin Manager", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.nav_recycle_bin).setOnClickListener {
            drawerLayout?.closeDrawer(GravityCompat.START)
            Toast.makeText(this, "Recycle Bin", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.nav_screen_color).setOnClickListener {
            drawerLayout?.closeDrawer(GravityCompat.START)
            Toast.makeText(this, "Screen Color Picker", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.nav_signing_key).setOnClickListener {
            drawerLayout?.closeDrawer(GravityCompat.START)
            Toast.makeText(this, "Signing Key", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.nav_passwords).setOnClickListener {
            drawerLayout?.closeDrawer(GravityCompat.START)
            Toast.makeText(this, "Common Passwords", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.nav_text_editor).setOnClickListener {
            drawerLayout?.closeDrawer(GravityCompat.START)
            Toast.makeText(this, "Text Editor", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.nav_terminal).setOnClickListener {
            drawerLayout?.closeDrawer(GravityCompat.START)
            Toast.makeText(this, "Terminal", Toast.LENGTH_SHORT).show()
        }

        btnThemeToggle?.setOnClickListener { toggleTheme() }
        btnDrawerMenu?.setOnClickListener { showDrawerMenu(it) }

        populateDynamicStorage()
    }

    private fun populateDynamicStorage() {
        dynamicStorageContainer?.removeAllViews()

        for (i in customPaths.indices) {
            val path = customPaths[i]
            val name = File(path).name.ifEmpty { path.substringAfterLast("/").ifEmpty { "Storage" } }

            val itemView = LayoutInflater.from(this).inflate(R.layout.item_storage_path, dynamicStorageContainer, false)

            val nameText = itemView.findViewById<TextView>(R.id.storage_name)
            val pathText = itemView.findViewById<TextView>(R.id.storage_path)
            val storageTextItem = itemView.findViewById<TextView>(R.id.storage_text)
            val bgView = itemView.findViewById<View>(R.id.custom_storage_bg)

            val bg = bgView?.background
            if (bg is android.graphics.drawable.GradientDrawable) {
                val shape = bg.mutate() as android.graphics.drawable.GradientDrawable
                shape.setColor(currentPrimaryColor)
            }

            nameText.text = name
            pathText.text = path
            storageTextItem.text = "Calculating..."

            itemView.tag = path
            itemView.setOnClickListener { v ->
                val selectedPath = v.tag as String
                drawerLayout?.closeDrawer(GravityCompat.START)
                navigateToPath(selectedPath, activePanel)
            }

            dynamicStorageContainer?.addView(itemView)

            updateDynamicStorageInfo(path, storageTextItem)
        }
    }

    private fun updateDynamicStorageInfo(path: String, storageText: TextView) {
        executor.execute {
            try {
                val storageDir = File(path)
                if (!storageDir.exists() || !storageDir.canRead()) {
                    mainHandler.post { storageText.text = "Cannot access" }
                    return@execute
                }

                val stat = StatFs(path)
                val total = stat.blockCountLong * stat.blockSizeLong
                val available = stat.availableBlocksLong * stat.blockSizeLong
                val used = total - available

                val usedStr = formatSizeCompact(used)
                val availStr = formatSizeCompact(available)

                mainHandler.post {
                    storageText.text = "$usedStr used, $availStr available"
                }
            } catch (e: Exception) {
                mainHandler.post { storageText.text = "Error reading" }
            }
        }
    }

    private fun showDrawerMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, "Theme follows system").isCheckable = true
        popup.menu.findItem(1).isChecked = followSystemTheme
        popup.menu.add(0, 2, 1, "Add local storage").setIcon(R.drawable.ic_local_storage)
        popup.menu.add(0, 3, 2, "Manage tools group").setIcon(R.drawable.ic_settings)
        popup.menu.add(0, 4, 3, "Preferences").setIcon(R.drawable.ic_settings)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    followSystemTheme = !followSystemTheme
                    val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    prefs.edit().putBoolean(KEY_FOLLOW_SYSTEM_THEME, followSystemTheme).apply()
                    if (followSystemTheme) {
                        currentPalette = 0
                        prefs.edit().putInt(KEY_THEME_PALETTE, currentPalette).apply()
                    }
                    recreate()
                    true
                }
                2 -> {
                    openSafPicker()
                    true
                }
                3 -> {
                    Toast.makeText(this, "Manage tools group", Toast.LENGTH_SHORT).show()
                    true
                }
                4 -> {
                    Toast.makeText(this, "Preferences", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> true
            }
        }
        popup.show()
    }

    private fun openSafPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        startActivityForResult(intent, REQUEST_CODE_SAF)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    loadBothPanels()
                    updateStorageInfo()
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        } else if (requestCode == REQUEST_CODE_SAF) {
            if (resultCode == RESULT_OK && data != null) {
                val treeUri = data.data

                try {
                    contentResolver.takePersistableUriPermission(
                        treeUri!!,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                } catch (ignored: Exception) {
                }

                val path = getPathFromTreeUri(treeUri)
                if (path != null && !customPaths.contains(path)) {
                    customPaths.add(path)
                    saveCustomPaths()
                    populateDynamicStorage()
                    Toast.makeText(this, "Storage added: $path", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Could not access this location", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getPathFromTreeUri(treeUri: Uri?): String? {
        if (treeUri == null) return null

        val docId = treeUri.lastPathSegment ?: return null

        if (docId.startsWith("primary:")) {
            return "/storage/emulated/0/" + docId.substring(8)
        }

        val volumeId = docId.substringBefore(":")
        val subPath = docId.substringAfter(":", "")

        val volumePath = getVolumePath(volumeId)
        if (volumePath != null) {
            return if (subPath.isNotEmpty()) "$volumePath/$subPath" else volumePath
        }

        return null
    }

    private fun getVolumePath(volumeId: String): String? {
        if (volumeId == "primary") return "/storage/emulated/0"

        val searchDirs = listOf("/storage", "/mnt/media_rw")
        for (dir in searchDirs) {
            val f = File(dir)
            if (!f.exists()) continue
            val volumes = f.listFiles() ?: continue
            for (vol in volumes) {
                if (vol.name == volumeId || vol.name.endsWith("_$volumeId")) {
                    return vol.absolutePath
                }
            }
        }

        val emulatedDir = File("/storage/emulated")
        if (emulatedDir.exists()) {
            val emulated = emulatedDir.listFiles()
            if (emulated != null) {
                for (vol in emulated) {
                    if (vol.name == volumeId) {
                        return vol.absolutePath
                    }
                }
            }
        }

        val guess = "/storage/$volumeId"
        return if (File(guess).exists()) guess else null
    }

    private fun toggleSection(content: LinearLayout, arrow: ImageView) {
        if (content.visibility == View.VISIBLE) {
            content.visibility = View.GONE
            arrow.rotation = 0f
        } else {
            content.visibility = View.VISIBLE
            arrow.rotation = 180f
        }
    }

    private fun registerFileHandlers() {
        // Click handlers can be registered here
    }

    private fun setupAdapters() {
        adapterLeft = FileAdapter(this)
        adapterRight = FileAdapter(this)

        recyclerLeft.layoutManager = LinearLayoutManager(this)
        recyclerLeft.setHasFixedSize(true)
        recyclerLeft.itemAnimator = null

        recyclerRight.layoutManager = LinearLayoutManager(this)
        recyclerRight.setHasFixedSize(true)
        recyclerRight.itemAnimator = null

        recyclerLeft.adapter = adapterLeft
        recyclerRight.adapter = adapterRight

        setupAdapterCallbacks(adapterLeft, true)
        setupAdapterCallbacks(adapterRight, false)
    }

    private fun setupAdapterCallbacks(adapter: FileAdapter, isLeft: Boolean) {
        adapter.setOnItemClickListener(object : FileAdapter.OnItemClickListener {
            override fun onItemClick(view: View, position: Int, item: FileItem) {
                setActivePanel(if (isLeft) 0 else 1)

                when {
                    item.isParentDirectory() -> goUp(isLeft)
                    item.isDirectory -> navigateToPath(item.path, if (isLeft) 0 else 1)
                    else -> openFile(item)
                }
            }
        })
    }

    private fun debounceClick(): Boolean {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime.get() < DEBOUNCE_DELAY) {
            return true
        }
        lastClickTime.set(currentTime)
        return false
    }

    private fun setupListeners() {
        panelLeft?.setOnClickListener { setActivePanel(0) }
        panelRight?.setOnClickListener { setActivePanel(1) }

        recyclerLeft.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING || newState == RecyclerView.SCROLL_STATE_SETTLING) {
                    setActivePanel(0)
                }
            }
        })

        recyclerRight.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING || newState == RecyclerView.SCROLL_STATE_SETTLING) {
                    setActivePanel(1)
                }
            }
        })

        btnBack?.setOnClickListener { goBack() }
        btnForward?.setOnClickListener { goForward() }
        btnNew?.setOnClickListener { showNewDialog() }
        btnSwap?.setOnClickListener {
            if (activePanel == 0) {
                navigateToPath(currentPathLeft, 1)
            } else {
                navigateToPath(currentPathRight, 0)
            }
        }
        btnParent?.setOnClickListener { goUp(activePanel == 0) }
    }

    private fun navigateToPath(path: String, panelIndex: Int) {
        if (debounceClick()) return

        val isLeft = panelIndex == 0
        val currentPath = if (isLeft) currentPathLeft else currentPathRight

        if (path == currentPath) {
            loadPanel(isLeft)
            return
        }

        if (isLeft) {
            backStackLeft.push(currentPathLeft)
            forwardStackLeft.clear()
            currentPathLeft = path
        } else {
            backStackRight.push(currentPathRight)
            forwardStackRight.clear()
            currentPathRight = path
        }

        loadPanel(isLeft)
        updateTopBar()
        saveState()
    }

    private fun loadBothPanels() {
        loadPanel(true)
        loadPanel(false)
    }

    private fun loadPanel(isLeft: Boolean) {
        val path = if (isLeft) currentPathLeft else currentPathRight

        if (isLeft) {
            if (loadingPathLeft != null) return
            loadingPathLeft = path
        } else {
            if (loadingPathRight != null) return
            loadingPathRight = path
        }

        val targetPath = path

        executor.execute {
            val items = FileUtils.listFiles(targetPath)

            val parentPath = FileUtils.getParentPath(targetPath)
            if (parentPath != null) {
                items.add(0, FileItem.createParentItem(parentPath))
            }

            val finalItems = ArrayList(items)
            val finalPath = targetPath

            mainHandler.post {
                if (isLeft) {
                    if (finalPath != currentPathLeft) {
                        loadingPathLeft = null
                        return@post
                    }
                    loadingPathLeft = null
                    adapterLeft.updateData(finalItems)
                } else {
                    if (finalPath != currentPathRight) {
                        loadingPathRight = null
                        return@post
                    }
                    loadingPathRight = null
                    adapterRight.updateData(finalItems)
                }
                updateTopBar()
                saveState()
            }
        }
    }

    private fun goUp(isLeft: Boolean) {
        val current = if (isLeft) currentPathLeft else currentPathRight
        val parent = FileUtils.getParentPath(current)
        if (parent != null) {
            navigateToPath(parent, if (isLeft) 0 else 1)
        }
    }

    private fun goBack() {
        val backStack = if (activePanel == 0) backStackLeft else backStackRight
        val forwardStack = if (activePanel == 0) forwardStackLeft else forwardStackRight

        if (backStack.isNotEmpty()) {
            forwardStack.push(if (activePanel == 0) currentPathLeft else currentPathRight)
            val previous = backStack.pop()
            val isLeft = activePanel == 0
            if (isLeft) {
                currentPathLeft = previous
            } else {
                currentPathRight = previous
            }
            loadPanel(isLeft)
            updateTopBar()
            saveState()
        }
    }

    private fun goForward() {
        val forwardStack = if (activePanel == 0) forwardStackLeft else forwardStackRight

        if (forwardStack.isNotEmpty()) {
            if (activePanel == 0) {
                backStackLeft.push(currentPathLeft)
            } else {
                backStackRight.push(currentPathRight)
            }
            val next = forwardStack.pop()
            val isLeft = activePanel == 0
            if (isLeft) {
                currentPathLeft = next
            } else {
                currentPathRight = next
            }
            loadPanel(isLeft)
            updateTopBar()
            saveState()
        }
    }

    private fun setActivePanel(panel: Int) {
        activePanel = panel
        updateSwapIcon()
        updateTopBar()
    }

    private fun updateSwapIcon() {
        btnSwap?.setImageResource(if (activePanel == 0) R.drawable.ic_swap_left_active else R.drawable.ic_swap_right_active)
    }

    private fun updateBottomBarIcons() {
        val backStack = if (activePanel == 0) backStackLeft else backStackRight
        val forwardStack = if (activePanel == 0) forwardStackLeft else forwardStackRight
        btnBack?.setColorFilter(if (backStack.isEmpty()) greyColorFilter else android.graphics.PorterDuffColorFilter(buttonTint, android.graphics.PorterDuff.Mode.SRC_IN))
        btnForward?.setColorFilter(if (forwardStack.isEmpty()) greyColorFilter else android.graphics.PorterDuffColorFilter(buttonTint, android.graphics.PorterDuff.Mode.SRC_IN))
    }

    private fun updateTopBar() {
        val path = if (activePanel == 0) currentPathLeft else currentPathRight
        toolbarPath?.text = path

        updateBottomBarIcons()

        val adapter = if (activePanel == 0) adapterLeft else adapterRight
        val items = adapter.getItems()

        var folderCount = 0
        var fileCount = 0
        for (item in items) {
            if (!item.isParentDirectory()) {
                if (item.isDirectory) folderCount++ else fileCount++
            }
        }

        val finalStats = "Folders: $folderCount  Files: $fileCount"
        topStats?.text = finalStats

        executor.execute {
            var available = 0L
            var total = 0L
            try {
                var statPath = path
                while (!File(statPath).canRead() && statPath.lastIndexOf('/') > 0) {
                    statPath = statPath.substring(0, statPath.lastIndexOf('/'))
                }
                if (statPath.isEmpty()) statPath = "/"
                val stat = StatFs(statPath)
                available = stat.availableBlocksLong * stat.blockSizeLong
                total = stat.blockCountLong * stat.blockSizeLong
            } catch (ignored: Exception) {
            }

            val finalDisk = String.format(
                "Disk: %.2fG/%.2fG",
                available / (1024.0 * 1024 * 1024),
                total / (1024.0 * 1024 * 1024)
            )

            mainHandler.post {
                diskInfo?.text = finalDisk
            }
        }
    }

    private fun updateStorageInfo() {
        executor.execute {
            try {
                val rootStat = StatFs("/")
                val rootTotal = rootStat.blockCountLong * rootStat.blockSizeLong
                val rootAvail = rootStat.availableBlocksLong * rootStat.blockSizeLong
                val rootUsed = rootTotal - rootAvail
                val rootPercent = ((rootUsed * 100) / rootTotal).toInt()

                val storageDir = Environment.getExternalStorageDirectory()
                val storageStat = StatFs(storageDir.path)
                val storageTotal = storageStat.blockCountLong * storageStat.blockSizeLong
                val storageAvail = storageStat.availableBlocksLong * storageStat.blockSizeLong
                val storageUsed = storageTotal - storageAvail
                val storagePercent = ((storageUsed * 100) / storageTotal).toInt()

                val rootUsedStr = formatSizeCompact(rootUsed)
                val rootAvailStr = formatSizeCompact(rootAvail)
                val storageUsedStr = formatSizeCompact(storageUsed)
                val storageAvailStr = formatSizeCompact(storageAvail)

                mainHandler.post {
                    rootProgress?.progress = rootPercent
                    rootStorageText?.text = "$rootUsedStr used, $rootAvailStr available"

                    storageProgress?.progress = storagePercent
                    storageText?.text = "$storageUsedStr used, $storageAvailStr available"
                }
            } catch (e: Exception) {
                mainHandler.post {
                    rootStorageText?.text = "Unable to calculate"
                    storageText?.text = "Unable to calculate"
                }
            }
        }
    }

    private fun formatSizeCompact(size: Long): String {
        var s = size
        if (s < 0) s = 0
        if (s < 1024) return "$s B"
        if (s < 1024 * 1024) return String.format(Locale.getDefault(), "%.0fK", s / 1024.0)
        if (s < 1024 * 1024 * 1024) return String.format(Locale.getDefault(), "%.2fM", s / (1024.0 * 1024))
        return String.format(Locale.getDefault(), "%.2fG", s / (1024.0 * 1024 * 1024))
    }

    private fun formatSize(size: Long): String {
        var s = size
        if (s < 0) s = 0
        if (s < 1024) return "$s B"
        if (s < 1024 * 1024) return String.format(Locale.getDefault(), "%.1f KB", s / 1024.0)
        if (s < 1024 * 1024 * 1024) return String.format(Locale.getDefault(), "%.1f MB", s / (1024.0 * 1024))
        return String.format(Locale.getDefault(), "%.1f GB", s / (1024.0 * 1024 * 1024))
    }

    private fun showNewDialog() {
        val input = EditText(this)
        input.hint = "Name"

        AlertDialog.Builder(this)
            .setTitle("Create")
            .setView(input)
            .setNegativeButton("File") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) createFile(name)
            }
            .setPositiveButton("Folder") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) createFolder(name)
            }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun createFolder(name: String) {
        val path = if (activePanel == 0) currentPathLeft else currentPathRight
        executor.execute {
            val success = FileUtils.createFolder(path, name)
            mainHandler.post {
                if (success) {
                    loadPanel(activePanel == 0)
                } else {
                    Toast.makeText(this, "Failed to create folder", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun createFile(name: String) {
        val path = if (activePanel == 0) currentPathLeft else currentPathRight
        executor.execute {
            try {
                val file = File(path, name)
                val created = file.createNewFile()
                mainHandler.post {
                    if (created) {
                        loadPanel(activePanel == 0)
                    } else {
                        Toast.makeText(this, "File already exists", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                mainHandler.post {
                    Toast.makeText(this, "Failed to create file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openFile(file: FileItem) {
        val path = file.path
        if (FileUtils.isTextFile(path)) {
            FileEditorActivity.start(this, path)
        } else if (FileUtils.isApkFile(path)) {
            val intent = Intent(Intent.ACTION_VIEW)
            val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val fileUri = androidx.core.content.FileProvider.getUriForFile(
                    this,
                    "$packageName.fileprovider",
                    File(path)
                )
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                fileUri
            } else {
                Uri.fromFile(File(path))
            }
            intent.setDataAndType(uri, FileUtils.getMimeType(path))
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "No app found to install APK", Toast.LENGTH_SHORT).show()
            }
        } else {
            val intent = Intent(Intent.ACTION_VIEW)
            val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val fileUri = androidx.core.content.FileProvider.getUriForFile(
                    this,
                    "$packageName.fileprovider",
                    File(path)
                )
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                fileUri
            } else {
                Uri.fromFile(File(path))
            }
            intent.setDataAndType(uri, FileUtils.getMimeType(path))
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "No app found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("MM Manager")
            .setMessage("MM Manager v1.0\n\nDual Panel File Manager")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun hasPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data = Uri.parse("package:$packageName")
                startActivityForResult(intent, REQUEST_CODE_PERMISSION)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivityForResult(intent, REQUEST_CODE_PERMISSION)
            }
        } else {
            @Suppress("DEPRECATION")
            requestPermissions(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ), REQUEST_CODE_PERMISSION
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSION) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                loadBothPanels()
                updateStorageInfo()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        applyThemeColors()

        if (hasPermissions()) {
            if (adapterLeft.itemCount == 0 && adapterRight.itemCount == 0) {
                loadBothPanels()
                updateStorageInfo()
            } else {
                loadPanel(activePanel == 0)
                loadPanel(activePanel == 1)
                updateTopBar()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        saveState()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (drawerLayout?.isDrawerOpen(GravityCompat.START) == true) {
            drawerLayout?.closeDrawer(GravityCompat.START)
            return
        }

        val backStack = if (activePanel == 0) backStackLeft else backStackRight
        if (backStack.isNotEmpty()) {
            goBack()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        saveState()
        executor.shutdown()
    }

    companion object {
        private const val REQUEST_CODE_PERMISSION = 100
        private const val REQUEST_CODE_SAF = 101
        private const val DEFAULT_PATH = "/storage/emulated/0/"
        private const val PREFS_NAME = "MagicManagerPrefs"
        private const val KEY_PATH_LEFT = "pathLeft"
        private const val KEY_PATH_RIGHT = "pathRight"
        private const val KEY_SCROLL_LEFT = "scrollLeft"
        private const val KEY_SCROLL_RIGHT = "scrollRight"
        private const val KEY_THEME_PALETTE = "themePalette"
        private const val KEY_FOLLOW_SYSTEM_THEME = "followSystemTheme"
        private const val KEY_CUSTOM_PATHS = "customPaths"
        private const val DEBOUNCE_DELAY = 200L

        private val THEMES = arrayOf(
            "AppTheme",
            "AppTheme.Palette1",
            "AppTheme.Palette2",
            "AppTheme.Palette3",
            "AppTheme.Palette4",
            "AppTheme.Palette5",
            "AppTheme.Palette6",
            "AppTheme.Palette7",
            "AppTheme.Palette8",
            "AppTheme.Palette9",
            "AppTheme.Palette10"
        )

        private val PALETTES = arrayOf(
            intArrayOf(0xFF212121.toInt(), 0xFF424242.toInt()),
            intArrayOf(0xFF1565C0.toInt(), 0xFF1E88E5.toInt()),
            intArrayOf(0xFF6A1B9A.toInt(), 0xFF8E24AA.toInt()),
            intArrayOf(0xFF2E7D32.toInt(), 0xFF43A047.toInt()),
            intArrayOf(0xFFE65100.toInt(), 0xFFF57C00.toInt()),
            intArrayOf(0xFFC2185B.toInt(), 0xFFD81B60.toInt()),
            intArrayOf(0xFF00838F.toInt(), 0xFF00ACC1.toInt()),
            intArrayOf(0xFF4527A0.toInt(), 0xFF5E35B1.toInt()),
            intArrayOf(0xFFB71C1C.toInt(), 0xFFE53935.toInt()),
            intArrayOf(0xFF1A237E.toInt(), 0xFF3949AB.toInt()),
            intArrayOf(0xFF00695C.toInt(), 0xFF00897B.toInt()),
            intArrayOf(0xFF0277BD.toInt(), 0xFF00838F.toInt()),
            intArrayOf(0xFF4A148C.toInt(), 0xFF6A1B9A.toInt()),
            intArrayOf(0xFFBF360C.toInt(), 0xFFE64A19.toInt()),
            intArrayOf(0xFF1B5E20.toInt(), 0xFF2E7D32.toInt()),
        )
    }
}
