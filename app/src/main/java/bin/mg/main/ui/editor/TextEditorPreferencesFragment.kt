package bin.mg.main.ui.editor

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import bin.mg.main.R

class TextEditorPreferencesFragment : DialogFragment() {

    private var fontSize = 13
    private var wordWrap = true
    private var showLineNumbers = true
    private var autoSave = false
    private var autoIndent = true
    private var highlightSyntax = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.EditorPreferencesDialog)
        loadPrefs()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_editor_preferences, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ImageButton>(R.id.btn_close_prefs)?.setOnClickListener {
            dismiss()
        }

        val fontSizeText = view.findViewById<TextView>(R.id.text_font_size)
        val fontSizeSeek = view.findViewById<SeekBar>(R.id.seek_font_size)

        fontSizeText.text = "${fontSize}sp"
        fontSizeSeek.progress = fontSize - 8
        fontSizeSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                fontSize = progress + 8
                fontSizeText.text = "${fontSize}sp"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val wordWrapSwitch = view.findViewById<Switch>(R.id.switch_word_wrap)
        wordWrapSwitch.isChecked = wordWrap
        wordWrapSwitch.setOnCheckedChangeListener { _, checked -> wordWrap = checked }

        val lineNumbersSwitch = view.findViewById<Switch>(R.id.switch_line_numbers)
        lineNumbersSwitch.isChecked = showLineNumbers
        lineNumbersSwitch.setOnCheckedChangeListener { _, checked -> showLineNumbers = checked }

        val autoSaveSwitch = view.findViewById<Switch>(R.id.switch_auto_save)
        autoSaveSwitch.isChecked = autoSave
        autoSaveSwitch.setOnCheckedChangeListener { _, checked -> autoSave = checked }

        val autoIndentSwitch = view.findViewById<Switch>(R.id.switch_auto_indent)
        autoIndentSwitch.isChecked = autoIndent
        autoIndentSwitch.setOnCheckedChangeListener { _, checked -> autoIndent = checked }

        val syntaxSwitch = view.findViewById<Switch>(R.id.switch_syntax_highlight)
        syntaxSwitch.isChecked = highlightSyntax
        syntaxSwitch.setOnCheckedChangeListener { _, checked -> highlightSyntax = checked }

        view.findViewById<View>(R.id.btn_apply_prefs)?.setOnClickListener {
            savePrefs()
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun loadPrefs() {
        val prefs = requireActivity().getSharedPreferences("editor_prefs", 0)
        fontSize = prefs.getInt("font_size", 13)
        wordWrap = prefs.getBoolean("word_wrap", true)
        showLineNumbers = prefs.getBoolean("show_line_numbers", true)
        autoSave = prefs.getBoolean("auto_save", false)
        autoIndent = prefs.getBoolean("auto_indent", true)
        highlightSyntax = prefs.getBoolean("highlight_syntax", true)
    }

    private fun savePrefs() {
        requireActivity().getSharedPreferences("editor_prefs", 0).edit().apply {
            putInt("font_size", fontSize)
            putBoolean("word_wrap", wordWrap)
            putBoolean("show_line_numbers", showLineNumbers)
            putBoolean("auto_save", autoSave)
            putBoolean("auto_indent", autoIndent)
            putBoolean("highlight_syntax", highlightSyntax)
            apply()
        }
    }
}
