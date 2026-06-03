package bin.mg.main.file

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class SyntaxSelectorFragment : DialogFragment() {

    interface OnSyntaxSelectedListener {
        fun onSyntaxSelected(syntaxName: String)
    }

    private val syntaxNames = arrayOf(
        "Plain Text", "Java", "Kotlin", "Python", "JavaScript",
        "HTML", "CSS", "XML", "JSON", "Shell", "Smali"
    )

    private val syntaxValues = arrayOf(
        "text", "java", "kotlin", "python", "javascript",
        "html", "css", "xml", "json", "shell", "smali"
    )

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setTitle("Select Syntax")
            .setItems(syntaxNames) { _, which ->
                (activity as? OnSyntaxSelectedListener)?.onSyntaxSelected(syntaxValues[which])
            }
            .setNegativeButton("Cancel", null)
            .create()
    }
}
