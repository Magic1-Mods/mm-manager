package bin.mg.main.ui.editor

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ExpandableListView
import android.widget.ImageView
import android.widget.SimpleExpandableListAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import bin.mg.main.R
import io.github.rosemoe.sora.langs.java.JavaLanguage
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.skylot.jadx.core.JadxDecompiler
import java.io.File
import java.util.concurrent.Executors

class DexEditorActivity : AppCompatActivity() {

    private var codeEditor: CodeEditor? = null
    private var classListView: ExpandableListView? = null
    private var editorContainer: View? = null
    private var listContainer: View? = null
    private var filenameText: TextView? = null
    private var lineNoText: TextView? = null

    private var dexFilePath: String? = null
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var jadxInstance: JadxDecompiler? = null
    private var classNames = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dex_editor)

        codeEditor = findViewById(R.id.dex_editor)
        classListView = findViewById(R.id.class_list)
        editorContainer = findViewById(R.id.editor_container)
        listContainer = findViewById(R.id.list_container)
        filenameText = findViewById(R.id.textview_filename)
        lineNoText = findViewById(R.id.textview_lineno_encoding)

        findViewById<ImageView>(R.id.btn_back).setOnClickListener { onBackPressed() }
        findViewById<ImageView>(R.id.btn_toggle_view).setOnClickListener { toggleView() }

        dexFilePath = intent.getStringExtra("file_path")

        if (dexFilePath != null && File(dexFilePath!!).exists()) {
            loadDexFile()
        } else {
            Toast.makeText(this, "Invalid DEX file", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadDexFile() {
        val dialog = ProgressDialog.show(this, "Loading", "Loading DEX file...", true)
        executor.execute {
            try {
                val jadx = JadxDecompiler(File(dexFilePath!!))
                jadx.load()
                jadxInstance = jadx

                val nodes = mutableListOf<String>()
                for (cls in jadx.dex_classes.classNodes) {
                    nodes.add(cls.type.className)
                }
                classNames = nodes

                mainHandler.post {
                    dialog.dismiss()
                    filenameText?.text = File(dexFilePath!!).name

                    val adapter = object : SimpleExpandableListAdapter(
                        this@DexEditorActivity,
                        listOf(mapOf("name" to "Classes (${classNames.size})")),
                        android.R.layout.simple_expandable_list_item_1,
                        arrayOf("name"),
                        intArrayOf(android.R.id.text1),
                        listOf(classNames.map { mapOf("name" to it.substringAfterLast('.')) }),
                        android.R.layout.simple_list_item_1,
                        arrayOf("name"),
                        intArrayOf(android.R.id.text1)
                    ) {}

                    classListView?.setAdapter(adapter)
                    classListView?.setOnChildClickListener { _, _, _, childPos, _ ->
                        if (childPos < classNames.size) {
                            showClassSource(classNames[childPos])
                        }
                        true
                    }
                    classListView?.expandGroup(0)
                }
            } catch (e: Exception) {
                mainHandler.post {
                    dialog.dismiss()
                    Toast.makeText(this@DexEditorActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showClassSource(className: String) {
        val dialog = ProgressDialog.show(this, "Loading", "Loading class...", true)
        executor.execute {
            try {
                val jadx = jadxInstance ?: return@execute
                val classNode = jadx.dex_classes.classNodes.find { it.type.className == className }

                val source = if (classNode != null) {
                    try {
                        val codeWriter = jadx.getCodeWriter()
                        jadx.codeGen.generateClass(codeWriter, classNode)
                        codeWriter.toString()
                    } catch (_: Exception) {
                        "// Could not decompile $className\n// Class may contain complex structures"
                    }
                } else {
                    "// Class not found: $className"
                }

                mainHandler.post {
                    dialog.dismiss()
                    codeEditor?.setText(source)
                    try {
                        codeEditor?.setEditorLanguage(JavaLanguage())
                    } catch (_: Exception) {}
                    toggleView()
                }
            } catch (e: Exception) {
                mainHandler.post {
                    dialog.dismiss()
                    Toast.makeText(this@DexEditorActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun toggleView() {
        val showEditor = editorContainer?.visibility == View.GONE
        editorContainer?.visibility = if (showEditor) View.VISIBLE else View.GONE
        listContainer?.visibility = if (showEditor) View.GONE else View.VISIBLE
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (editorContainer?.visibility == View.VISIBLE) {
            toggleView()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { jadxInstance?.close() } catch (_: Exception) {}
    }

    companion object {
        @JvmStatic
        fun start(context: Activity, filePath: String) {
            val intent = android.content.Intent(context, DexEditorActivity::class.java)
            intent.putExtra("file_path", filePath)
            context.startActivity(intent)
        }
    }
}
