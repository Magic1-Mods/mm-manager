package bin.mg.main.file

import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import bin.mg.main.utils.theme.ThemeManager
import com.google.android.material.switchmaterial.SwitchMaterial

class PreferencesActivity : AppCompatActivity() {

    private lateinit var prefs: android.content.SharedPreferences
    private var isDark = false
    private var bgColor = Color.WHITE
    private var textColor = Color.parseColor("#212121")
    private var descColor = Color.parseColor("#757575")
    private var sectionColor = Color.parseColor("#2196F3")
    private var dividerColor = Color.parseColor("#E0E0E0")

    private var toolbarBg = Color.parseColor("#212121")
    private val toolbarText = Color.WHITE

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyActivityTheme(this)
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences("editor_prefs", MODE_PRIVATE)

        val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        isDark = (nightMode == Configuration.UI_MODE_NIGHT_YES) || ThemeManager.isDarkMode(this)

        toolbarBg = ThemeManager.toolbarBackground(this)
        bgColor      = if (isDark) Color.parseColor("#1E1E1E") else Color.WHITE
        textColor    = if (isDark) Color.parseColor("#DDDDDD") else Color.parseColor("#212121")
        descColor    = if (isDark) Color.parseColor("#888888") else Color.parseColor("#757575")
        dividerColor = if (isDark) Color.parseColor("#333333") else Color.parseColor("#E0E0E0")
        sectionColor = ThemeManager.secondary(this)

        window.statusBarColor = toolbarBg

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(bgColor)
        }

        root.addView(buildToolbar())

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0, 1f
            )
            setBackgroundColor(bgColor)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, dp(8), 0, dp(24))
        }

        addSectionTitle(container, "Appearance")
        addPrefItem(container, "Font", getFontName()) { showFontDialog() }
        addPrefItem(container, "Font size", prefs.getInt("font_size", 14).toString()) { showFontSizeDialog() }
        addPrefItem(container, "Tab size", prefs.getInt("tab_size", 4).toString()) { showTabSizeDialog() }

        addPrefSwitch(container, "Show indent guides",
            prefs.getBoolean("show_indent_guides", true)) { v -> prefs.edit().putBoolean("show_indent_guides", v).apply() }
        addPrefSwitch(container, "Show ASCII control characters",
            prefs.getBoolean("show_ascii_control", false)) { v -> prefs.edit().putBoolean("show_ascii_control", v).apply() }
        addPrefSwitch(container, "Show Unicode control characters",
            prefs.getBoolean("show_unicode_control", true)) { v -> prefs.edit().putBoolean("show_unicode_control", v).apply() }
        addPrefSwitch(container, "Show soft wrap arrows",
            prefs.getBoolean("show_soft_wrap_arrows", true)) { v -> prefs.edit().putBoolean("show_soft_wrap_arrows", v).apply() }
        addPrefSwitch(container, "Show line numbers",
            prefs.getBoolean("show_line_numbers", true)) { v -> prefs.edit().putBoolean("show_line_numbers", v).apply() }
        addPrefSwitchWithDesc(container, "Fixed line numbers",
            "When soft wrapping is not enabled, line numbers are always displayed fixedly to the left.",
            prefs.getBoolean("fixed_line_numbers", false)) { v -> prefs.edit().putBoolean("fixed_line_numbers", v).apply() }
        addPrefSwitchWithDesc(container, "Show blank symbol",
            "Display spaces as dots and tabs as horizontal lines.",
            prefs.getBoolean("show_blank_symbol", false)) { v -> prefs.edit().putBoolean("show_blank_symbol", v).apply() }
        addPrefSwitchWithDesc(container, "Hide single space",
            "Don't display spaces which are not adjacent to other blank symbols.",
            prefs.getBoolean("hide_single_space", false)) { v -> prefs.edit().putBoolean("hide_single_space", v).apply() }

        addDivider(container)

        addSectionTitle(container, "Highlight")
        addPrefSwitch(container, "Syntax highlight",
            prefs.getBoolean("syntax_highlighting", true)) { v -> prefs.edit().putBoolean("syntax_highlighting", v).apply() }
        addPrefItem(container, "Syntax files manager", "") {
            Toast.makeText(this, "Syntax files manager (TODO)", Toast.LENGTH_SHORT).show()
        }

        addDivider(container)

        addSectionTitle(container, "Completion")
        addPrefSwitchWithDesc(container, "Apply completion on Enter",
            "Apply the first completion item on Enter",
            prefs.getBoolean("apply_completion_on_enter", true)) { v -> prefs.edit().putBoolean("apply_completion_on_enter", v).apply() }
        addPrefItem(container, "Max height of completion window",
            prefs.getInt("max_completion_height", 3).toString()) { showMaxCompletionHeightDialog() }

        addDivider(container)

        addSectionTitle(container, "Customize")
        addPrefItem(container, "Edit function bar", "") {
            Toast.makeText(this, "Edit function bar (TODO)", Toast.LENGTH_SHORT).show()
        }
        addPrefItem(container, "Edit floating menus", "") {
            Toast.makeText(this, "Edit floating menus (TODO)", Toast.LENGTH_SHORT).show()
        }
        addPrefItem(container, "Edit tool menus", "") {
            Toast.makeText(this, "Edit tool menus (TODO)", Toast.LENGTH_SHORT).show()
        }

        addDivider(container)

        addSectionTitle(container, "Function")
        addPrefSwitch(container, "Function bar",
            prefs.getBoolean("function_bar", true)) { v -> prefs.edit().putBoolean("function_bar", v).apply() }
        addPrefSwitch(container, "Enable magnifier",
            prefs.getBoolean("enable_magnifier", true)) { v -> prefs.edit().putBoolean("enable_magnifier", v).apply() }
        addPrefSwitch(container, "Pinch to zoom",
            prefs.getBoolean("pinch_to_zoom", true)) { v -> prefs.edit().putBoolean("pinch_to_zoom", v).apply() }
        addPrefSwitch(container, "Auto indent",
            prefs.getBoolean("auto_indent", true)) { v -> prefs.edit().putBoolean("auto_indent", v).apply() }
        addPrefSwitchWithDesc(container, "Use tabs for indentation",
            "Use \"\\t\" instead of spaces for indentation.",
            prefs.getBoolean("use_tabs", false)) { v -> prefs.edit().putBoolean("use_tabs", v).apply() }
        addPrefSwitchWithDesc(container, "Keep word when soft wrapping",
            "Prevent words from being cut off to the next line when soft wrapping.",
            prefs.getBoolean("keep_word_wrap", true)) { v -> prefs.edit().putBoolean("keep_word_wrap", v).apply() }
        addPrefItem(container, "Threshold for enabling smooth mode",
            "Current: ${prefs.getInt("smooth_threshold", 10000)}") { showSmoothThresholdDialog() }

        addDivider(container)

        addSectionTitle(container, "Other")
        addPrefItem(container, "Preference for keeping files",
            "For newly opened files, whether to keep them on exit for next editing.") {
            Toast.makeText(this, "Keep files preference (TODO)", Toast.LENGTH_SHORT).show()
        }
        addPrefSwitchWithDesc(container, "Check if file has been modified",
            "After detecting that the file has been modified, you will be prompted whether to reload.",
            prefs.getBoolean("check_file_modified", true)) { v -> prefs.edit().putBoolean("check_file_modified", v).apply() }
        addPrefSwitchWithDesc(container, "Check if file has been deleted",
            "After detecting that the file is deleted, you will be prompted whether to remove it.",
            prefs.getBoolean("check_file_deleted", true)) { v -> prefs.edit().putBoolean("check_file_deleted", v).apply() }
        addPrefSwitchWithDesc(container, "Double confirm before exit",
            "If enabled, you need to click the back button twice to exit.",
            prefs.getBoolean("double_confirm_exit", false)) { v -> prefs.edit().putBoolean("double_confirm_exit", v).apply() }

        scrollView.addView(container)
        root.addView(scrollView)
        setContentView(root)
    }

    
    private fun buildToolbar(): LinearLayout {
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(toolbarBg)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56)
            )
            setPadding(dp(4), 0, dp(16), 0)
        }

        val backBtn = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_revert)
            setColorFilter(toolbarText)
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48)).also {
                it.marginEnd = dp(4)
            }
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            isClickable = true
            isFocusable = true
            setOnClickListener { onBackPressedDispatcher.onBackPressed() }
            val outValue = android.util.TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
            setBackgroundResource(outValue.resourceId)
        }

        val titleView = TextView(this).apply {
            text = "Preferences"
            setTextColor(toolbarText)
            textSize = 19f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }

        toolbar.addView(backBtn)
        toolbar.addView(titleView)
        return toolbar
    }

    private fun addSectionTitle(parent: LinearLayout, title: String) {
        parent.addView(TextView(this).apply {
            text = title
            setTextColor(sectionColor)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(16), dp(16), dp(16), dp(4))
        })
    }

    private fun addPrefItem(parent: LinearLayout, title: String, subtitle: String, onClick: () -> Unit) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(dp(16), dp(10), dp(16), dp(10))
            isClickable = true
            isFocusable = true
            setBackgroundResource(android.R.drawable.list_selector_background)
            setOnClickListener { onClick() }
        }
        row.addView(TextView(this).apply {
            text = title
            setTextColor(textColor)
            textSize = 15f
        })
        if (subtitle.isNotEmpty()) {
            row.addView(TextView(this).apply {
                text = subtitle
                setTextColor(descColor)
                textSize = 12.5f
                setPadding(0, dp(2), 0, 0)
            })
        }
        parent.addView(row)
    }

    private fun addPrefSwitch(parent: LinearLayout, title: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(dp(16), dp(8), dp(12), dp(8))
        }
        row.addView(TextView(this).apply {
            text = title
            setTextColor(textColor)
            textSize = 15f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        row.addView(SwitchMaterial(this).apply {
            isChecked = checked
            setOnCheckedChangeListener { _, c -> onChecked(c) }
        })
        parent.addView(row)
    }

    private fun addPrefSwitchWithDesc(parent: LinearLayout, title: String, desc: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(dp(16), dp(8), dp(12), dp(8))
        }
        val textCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        textCol.addView(TextView(this).apply {
            text = title
            setTextColor(textColor)
            textSize = 15f
        })
        textCol.addView(TextView(this).apply {
            text = desc
            setTextColor(descColor)
            textSize = 12.5f
            setPadding(0, dp(2), 0, 0)
        })
        row.addView(textCol)
        row.addView(SwitchMaterial(this).apply {
            isChecked = checked
            setOnCheckedChangeListener { _, c -> onChecked(c) }
        })
        parent.addView(row)
    }

    private fun addDivider(parent: LinearLayout) {
        parent.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
            setBackgroundColor(dividerColor)
        })
    }

    private fun getFontName(): String {
        return if (prefs.getString("font_style", "monospace") == "monospace") "Monospace" else "Normal"
    }

    private fun showFontDialog() {
        val options = arrayOf("Normal", "Monospace")
        val current = if (getFontName() == "Monospace") 1 else 0
        AlertDialog.Builder(this)
            .setTitle("Font")
            .setSingleChoiceItems(options, current) { dialog, which ->
                prefs.edit().putString("font_style", if (which == 1) "monospace" else "normal").apply()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showFontSizeDialog() {
        val currentSize = prefs.getInt("font_size", 14)
        val input = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(currentSize.toString())
            setSelection(text.length)
        }
        val wrapper = LinearLayout(this).apply {
            setPadding(dp(20), dp(8), dp(20), dp(4))
            addView(input)
        }
        AlertDialog.Builder(this)
            .setTitle("Font size")
            .setView(wrapper)
            .setPositiveButton("OK") { _, _ ->
                val size = input.text.toString().toIntOrNull() ?: currentSize
                prefs.edit().putInt("font_size", size.coerceIn(8, 36)).apply()
                Toast.makeText(this, "Font size set to $size", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showTabSizeDialog() {
        val currentTab = prefs.getInt("tab_size", 4)
        val input = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(currentTab.toString())
            setSelection(text.length)
            hint = "e.g. 2, 4, 8"
        }
        val wrapper = LinearLayout(this).apply {
            setPadding(dp(20), dp(8), dp(20), dp(4))
            addView(input)
        }
        AlertDialog.Builder(this)
            .setTitle("Tab size")
            .setView(wrapper)
            .setPositiveButton("OK") { _, _ ->
                val size = input.text.toString().toIntOrNull() ?: currentTab
                prefs.edit().putInt("tab_size", size.coerceIn(1, 16)).apply()
                Toast.makeText(this, "Tab size set to $size", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showMaxCompletionHeightDialog() {
        val current = prefs.getInt("max_completion_height", 3)
        val input = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(current.toString())
            setSelection(text.length)
        }
        val wrapper = LinearLayout(this).apply {
            setPadding(dp(20), dp(8), dp(20), dp(4))
            addView(input)
        }
        AlertDialog.Builder(this)
            .setTitle("Max height of completion window")
            .setView(wrapper)
            .setPositiveButton("OK") { _, _ ->
                val h = input.text.toString().toIntOrNull() ?: current
                prefs.edit().putInt("max_completion_height", h.coerceIn(1, 10)).apply()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSmoothThresholdDialog() {
        val current = prefs.getInt("smooth_threshold", 10000)
        val input = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(current.toString())
            setSelection(text.length)
        }
        val wrapper = LinearLayout(this).apply {
            setPadding(dp(20), dp(8), dp(20), dp(4))
            addView(input)
        }
        AlertDialog.Builder(this)
            .setTitle("Smooth mode threshold")
            .setView(wrapper)
            .setPositiveButton("OK") { _, _ ->
                val t = input.text.toString().toIntOrNull() ?: current
                prefs.edit().putInt("smooth_threshold", t.coerceIn(1000, 100000)).apply()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
