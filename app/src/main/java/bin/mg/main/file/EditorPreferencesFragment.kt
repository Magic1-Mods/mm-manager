package bin.mg.main.file

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import bin.mg.main.R
import com.google.android.material.switchmaterial.SwitchMaterial

class EditorPreferencesFragment : DialogFragment() {

    interface OnPreferencesAppliedListener {
        fun onPreferencesApplied(
            fontSize: Int,
            wordWrap: Boolean,
            lineNumbers: Boolean,
            autoSave: Boolean,
            autoIndent: Boolean,
            syntaxHighlighting: Boolean
        )
    }

    companion object {
        private const val PREFS_NAME = "editor_prefs"
        private const val KEY_FONT_SIZE = "font_size"
        private const val KEY_WORD_WRAP = "word_wrap"
        private const val KEY_LINE_NUMBERS = "show_line_numbers"
        private const val KEY_AUTO_SAVE = "auto_save"
        private const val KEY_AUTO_INDENT = "auto_indent"
        private const val KEY_SYNTAX_HIGHLIGHTING = "syntax_highlighting"

        fun getFontSize(context: android.content.Context): Int {
            return context.getSharedPreferences(PREFS_NAME, AppCompatActivity.MODE_PRIVATE)
                .getInt(KEY_FONT_SIZE, 14)
        }

        fun isWordWrap(context: android.content.Context): Boolean {
            return context.getSharedPreferences(PREFS_NAME, AppCompatActivity.MODE_PRIVATE)
                .getBoolean(KEY_WORD_WRAP, false)
        }

        fun isLineNumbers(context: android.content.Context): Boolean {
            return context.getSharedPreferences(PREFS_NAME, AppCompatActivity.MODE_PRIVATE)
                .getBoolean(KEY_LINE_NUMBERS, true)
        }

        fun isAutoSave(context: android.content.Context): Boolean {
            return context.getSharedPreferences(PREFS_NAME, AppCompatActivity.MODE_PRIVATE)
                .getBoolean(KEY_AUTO_SAVE, false)
        }

        fun isAutoIndent(context: android.content.Context): Boolean {
            return context.getSharedPreferences(PREFS_NAME, AppCompatActivity.MODE_PRIVATE)
                .getBoolean(KEY_AUTO_INDENT, true)
        }

        fun isSyntaxHighlighting(context: android.content.Context): Boolean {
            return context.getSharedPreferences(PREFS_NAME, AppCompatActivity.MODE_PRIVATE)
                .getBoolean(KEY_SYNTAX_HIGHLIGHTING, true)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_editor_preferences, null)

        val prefs = requireContext().getSharedPreferences(PREFS_NAME, AppCompatActivity.MODE_PRIVATE)

        val fontSizeValue = view.findViewById<TextView>(R.id.font_size_value)
        val fontSizeSeekBar = view.findViewById<SeekBar>(R.id.font_size_seekbar)
        val switchWordWrap = view.findViewById<SwitchMaterial>(R.id.switch_word_wrap)
        val switchLineNumbers = view.findViewById<SwitchMaterial>(R.id.switch_line_numbers)
        val switchAutoSave = view.findViewById<SwitchMaterial>(R.id.switch_auto_save)
        val switchAutoIndent = view.findViewById<SwitchMaterial>(R.id.switch_auto_indent)
        val switchSyntax = view.findViewById<SwitchMaterial>(R.id.switch_syntax)
        val btnApply = view.findViewById<View>(R.id.btn_apply)

        val currentFontSize = prefs.getInt(KEY_FONT_SIZE, 14)
        fontSizeSeekBar.progress = currentFontSize
        fontSizeValue.text = "${currentFontSize}sp"

        fontSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                fontSizeValue.text = "${progress}sp"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        switchWordWrap.isChecked = prefs.getBoolean(KEY_WORD_WRAP, false)
        switchLineNumbers.isChecked = prefs.getBoolean(KEY_LINE_NUMBERS, true)
        switchAutoSave.isChecked = prefs.getBoolean(KEY_AUTO_SAVE, false)
        switchAutoIndent.isChecked = prefs.getBoolean(KEY_AUTO_INDENT, true)
        switchSyntax.isChecked = prefs.getBoolean(KEY_SYNTAX_HIGHLIGHTING, true)

        btnApply.setOnClickListener {
            prefs.edit().apply {
                putInt(KEY_FONT_SIZE, fontSizeSeekBar.progress)
                putBoolean(KEY_WORD_WRAP, switchWordWrap.isChecked)
                putBoolean(KEY_LINE_NUMBERS, switchLineNumbers.isChecked)
                putBoolean(KEY_AUTO_SAVE, switchAutoSave.isChecked)
                putBoolean(KEY_AUTO_INDENT, switchAutoIndent.isChecked)
                putBoolean(KEY_SYNTAX_HIGHLIGHTING, switchSyntax.isChecked)
                apply()
            }

            (activity as? OnPreferencesAppliedListener)?.onPreferencesApplied(
                fontSizeSeekBar.progress,
                switchWordWrap.isChecked,
                switchLineNumbers.isChecked,
                switchAutoSave.isChecked,
                switchAutoIndent.isChecked,
                switchSyntax.isChecked
            )
            dismiss()
        }

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .create()
    }
}
