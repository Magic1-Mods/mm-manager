package bin.mg.main.file

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import bin.mg.main.utils.theme.ThemeManager
import com.google.android.material.switchmaterial.SwitchMaterial

/**
 * Full-screen preferences activity matching MT Manager's design.
 * Organized into sections: Appearance, Highlight, Completion, Customize, Function, Other.
 */
class PreferencesActivity : AppCompatActivity() {

    private lateinit var prefs: android.content.SharedPreferences
    private var isDark = false
    private var bgColor = Color.WHITE
    private var textColor = Color.parseColor("#212121")
    private var descColor = Color.parseColor("#757575")
    private var sectionColor = Color.parseColor("#2196F3")
    private var dividerColor = Color.parseColor("#E0E0E0")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences("editor_prefs", MODE_PRIVATE)
        isDark = ThemeManager.isDarkMode(this)
        bgColor = if (isDark) Color.parseColor("#1E1E1E") else Color.WHITE
        textColor = if (isDark) Color.parseColor("#DDDDDD") else Color.parseColor("#212121")
        descColor = if (isDark) Color.parseColor("#888888") else Color.parseColor("#757575")
        dividerColor = if (isDark) Color.parseColor("#333333") else Color.parseColor("#E0E0E0")

        val scrollView = ScrollView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(bgColor)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, dp(16), 0, dp(32))
        }

        // ── Appearance Section ──
        addSectionTitle(container, "Appearance")

        addPrefItem(container, "Font", getFontName(), false) { showFontDialog() }
        addPrefItem(container, "Font size", prefs.getInt("font_size", 14).toString(), false) { showFontSizeDialog() }
        addPrefItem(container, "Tab size", prefs.getInt("tab_size", 4).toString(), false) { showTabSizeDialog() }

        addPrefSwitch(container, "Show indent guides", prefs.getBoolean("show_indent_guides", true)) { checked ->
            prefs.edit().putBoolean("show_indent_guides", checked).apply()
        }
        addPrefSwitch(container, "Show ASCII control characters", prefs.getBoolean("show_ascii_control", false)) { checked ->
            prefs.edit().putBoolean("show_ascii_control", checked).apply()
        }
        addPrefSwitch(container, "Show Unicode control characters", prefs.getBoolean("show_unicode_control", true)) { checked ->
            prefs.edit().putBoolean("show_unicode_control", checked).apply()
        }
        addPrefSwitch(container, "Show soft wrap arrows", prefs.getBoolean("show_soft_wrap_arrows", true)) { checked ->
            prefs.edit().putBoolean("show_soft_wrap_arrows", checked).apply()
        }
        addPrefSwitch(container, "Show line numbers", prefs.getBoolean("show_line_numbers", true)) { checked ->
            prefs.edit().putBoolean("show_line_numbers", checked).apply()
        }
        addPrefSwitchWithDesc(container, "Fixed line numbers",
            "When soft wrapping is not enabled, line numbers are always displayed fixedly to the left.",
            prefs.getBoolean("fixed_line_numbers", false)) { checked ->
            prefs.edit().putBoolean("fixed_line_numbers", checked).apply()
        }
        addPrefSwitchWithDesc(container, "Show blank symbol",
            "Display spaces as dots and tabs as horizontal lines.",
            prefs.getBoolean("show_blank_symbol", false)) { checked ->
            prefs.edit().putBoolean("show_blank_symbol", checked).apply()
        }
        addPrefSwitchWithDesc(container, "Hide single space",
            "Don't display spaces which are not adjacent to other blank symbols.",
            prefs.getBoolean("hide_single_space", false)) { checked ->
            prefs.edit().putBoolean("hide_single_space", checked).apply()
        }

        addDivider(container)

        // ── Highlight Section ──
        addSectionTitle(container, "Highlight")

        addPrefSwitch(container, "Syntax highlight", prefs.getBoolean("syntax_highlighting", true)) { checked ->
            prefs.edit().putBoolean("syntax_highlighting", checked).apply()
        }
        addPrefItem(container, "Syntax files manager", "", false) {
            Toast.makeText(this, "Syntax files manager (TODO)", Toast.LENGTH_SHORT).show()
        }

        addDivider(container)

        // ── Completion Section ──
        addSectionTitle(container, "Completion")

        addPrefSwitchWithDesc(container, "Apply completion on Enter",
            "Apply the first completion item on Enter",
            prefs.getBoolean("apply_completion_on_enter", true)) { checked ->
            prefs.edit().putBoolean("apply_completion_on_enter", checked).apply()
        }
        addPrefItem(container, "Max height of completion window",
            prefs.getInt("max_completion_height", 3).toString(), false) {
            showMaxCompletionHeightDialog()
        }

        addDivider(container)

        // ── Customize Section ──
        addSectionTitle(container, "Customize")

        addPrefItem(container, "Edit function bar", "", false) {
            Toast.makeText(this, "Edit function bar (TODO)", Toast.LENGTH_SHORT).show()
        }
        addPrefItem(container, "Edit floating menus", "", false) {
            Toast.makeText(this, "Edit floating menus (TODO)", Toast.LENGTH_SHORT).show()
        }
        addPrefItem(container, "Edit tool menus", "", false) {
            Toast.makeText(this, "Edit tool menus (TODO)", Toast.LENGTH_SHORT).show()
        }

        addDivider(container)

        // ── Function Section ──
        addSectionTitle(container, "Function")

        addPrefSwitch(container, "Function bar", prefs.getBoolean("function_bar", true)) { checked ->
            prefs.edit().putBoolean("function_bar", checked).apply()
        }
        addPrefSwitch(container, "Enable magnifier", prefs.getBoolean("enable_magnifier", true)) { checked ->
            prefs.edit().putBoolean("enable_magnifier", checked).apply()
        }
        addPrefSwitch(container, "Pinch to zoom", prefs.getBoolean("pinch_to_zoom", true)) { checked ->
            prefs.edit().putBoolean("pinch_to_zoom", checked).apply()
        }
        addPrefSwitch(container, "Auto indent", prefs.getBoolean("auto_indent", true)) { checked ->
            prefs.edit().putBoolean("auto_indent", checked).apply()
        }
        addPrefSwitchWithDesc(container, "Use tabs for indentation",
            "Use \"\\t\" instead of spaces for indentation.",
            prefs.getBoolean("use_tabs", false)) { checked ->
            prefs.edit().putBoolean("use_tabs", checked).apply()
        }
        addPrefSwitchWithDesc(container, "Keep word when soft wrapping",
            "Prevent words from being cut off to the next line when soft wrapping.",
            prefs.getBoolean("keep_word_wrap", true)) { checked ->
            prefs.edit().putBoolean("keep_word_wrap", checked).apply()
        }
        addPrefItem(container, "Threshold for enabling smooth mode",
            "When opening the file, if the text length exceeds the specified length, the smooth mode will be automatically turned on.",
            prefs.getInt("smooth_threshold", 10000).toString(), false) {
            showSmoothThresholdDialog()
        }

        addDivider(container)

        // ── Other Section ──
        addSectionTitle(container, "Other")

        addPrefItem(container, "Preference for keeping files",
            "For newly opened files, whether to keep them in the text editor on exit for next editing.",
            false) {
            Toast.makeText(this, "Keep files preference (TODO)", Toast.LENGTH_SHORT).show()
        }
        addPrefSwitchWithDesc(container, "Check if file has been modified",
            "After detecting that the file has been modified, you will be prompted whether to reload the file.",
            prefs.getBoolean("check_file_modified", true)) { checked ->
            prefs.edit().putBoolean("check_file_modified", checked).apply()
        }
        addPrefSwitchWithDesc(container, "Check if file has been deleted",
            "After detecting that the file is deleted, you will be prompted whether to remove the file from the editor.",
            prefs.getBoolean("check_file_deleted", true)) { checked ->
            prefs.edit().putBoolean("check_file_deleted", checked).apply()
        }
        addPrefSwitchWithDesc(container, "Double confirm before exit",
            "If enabled, you need to click the back button twice to exit",
            prefs.getBoolean("double_confirm_exit", false)) { checked ->
            prefs.edit().putBoolean("double_confirm_exit", checked).apply()
        }

        scrollView.addView(container)
        setContentView(scrollView)
    }

    // ── UI Helpers ────────────────────────────────────────────────────────────

    private fun addSectionTitle(parent: LinearLayout, title: String) {
        val tv = TextView(this).apply {
            text = title
            setTextColor(sectionColor)
            textSize = 14f
            setPadding(dp(16), dp(24), dp(16), dp(8))
            typeface = Typeface.DEFAULT_BOLD
        }
        parent.addView(tv)
    }

    private fun addPrefItem(parent: LinearLayout, title: String, subtitle: String, clickable: Boolean, onClick: () -> Unit) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(dp(16), dp(14), dp(16), dp(14))
            if (clickable) {
                isClickable = true
                isFocusable = true
                setBackgroundResource(android.R.drawable.list_selector_background)
                setOnClickListener { onClick() }
            }
        }

        val titleTv = TextView(this).apply {
            text = title
            setTextColor(textColor)
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        container.addView(titleTv)

        if (subtitle.isNotEmpty()) {
            val descTv = TextView(this).apply {
                text = subtitle
                setTextColor(descColor)
                textSize = 13f
                setPadding(0, dp(4), 0, 0)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            container.addView(descTv)
        }

        parent.addView(container)
    }

    private fun addPrefSwitch(parent: LinearLayout, title: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(dp(16), dp(10), dp(16), dp(10))
        }

        val titleTv = TextView(this).apply {
            text = title
            setTextColor(textColor)
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }
        container.addView(titleTv)

        val switch = SwitchMaterial(this).apply {
            isChecked = checked
            setOnCheckedChangeListener { _, isChecked -> onChecked(isChecked) }
        }
        container.addView(switch)

        parent.addView(container)
    }

    private fun addPrefSwitchWithDesc(parent: LinearLayout, title: String, desc: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(dp(16), dp(10), dp(16), dp(10))
        }

        val textContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val titleTv = TextView(this).apply {
            text = title
            setTextColor(textColor)
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        textContainer.addView(titleTv)

        val descTv = TextView(this).apply {
            text = desc
            setTextColor(descColor)
            textSize = 13f
            setPadding(0, dp(4), 0, 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        textContainer.addView(descTv)

        container.addView(textContainer)

        val switch = SwitchMaterial(this).apply {
            isChecked = checked
            setOnCheckedChangeListener { _, isChecked -> onChecked(isChecked) }
        }
        container.addView(switch)

        parent.addView(container)
    }

    private fun addDivider(parent: LinearLayout) {
        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
            )
            setBackgroundColor(dividerColor)
        }
        parent.addView(divider)
    }

    // ── Dialog Helpers ────────────────────────────────────────────────────────

    private fun getFontName(): String {
        val fontStyle = prefs.getString("font_style", "monospace") ?: "monospace"
        return if (fontStyle == "monospace") "Monospace" else "Normal"
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
        val input = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(prefs.getInt("font_size", 14).toString())
            setSelection(text.length)
        }
        AlertDialog.Builder(this)
            .setTitle("Font size")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val size = input.text.toString().toIntOrNull() ?: 14
                prefs.edit().putInt("font_size", size.coerceIn(8, 30)).apply()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showTabSizeDialog() {
        val options = arrayOf("2", "4", "8")
        val currentTabSize = prefs.getInt("tab_size", 4)
        val currentIndex = options.indexOf(currentTabSize.toString()).coerceAtLeast(1)
        AlertDialog.Builder(this)
            .setTitle("Tab size")
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                prefs.edit().putInt("tab_size", options[which].toInt()).apply()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showMaxCompletionHeightDialog() {
        val input = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(prefs.getInt("max_completion_height", 3).toString())
            setSelection(text.length)
        }
        AlertDialog.Builder(this)
            .setTitle("Max height of completion window")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val height = input.text.toString().toIntOrNull() ?: 3
                prefs.edit().putInt("max_completion_height", height.coerceIn(1, 10)).apply()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSmoothThresholdDialog() {
        val input = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(prefs.getInt("smooth_threshold", 10000).toString())
            setSelection(text.length)
        }
        AlertDialog.Builder(this)
            .setTitle("Threshold for enabling smooth mode")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val threshold = input.text.toString().toIntOrNull() ?: 10000
                prefs.edit().putInt("smooth_threshold", threshold.coerceIn(1000, 100000)).apply()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
