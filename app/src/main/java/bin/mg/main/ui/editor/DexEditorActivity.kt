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
import jadx.api.JadxArgs
import jadx.api.JadxDecompiler
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
    private var allClassNames = mutableListOf<String>()

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
                val args = JadxArgs()
                args.setInputFiles(listOf(File(dexFilePath!!)))
                args.setSkipResources(true)
                args.isShowInconsistentCode = true

                val jadx = JadxDecompiler(args)
                jadx.load()
                jadxInstance = jadx

                val nodes = mutableListOf<String>()
                for (cls in jadx.classes) {
                    nodes.add(cls.fullName)
                }
                allClassNames = nodes
                classNames = nodes

                mainHandler.post {
                    dialog.dismiss()
                    filenameText?.text = File(dexFilePath!!).name + " (${classNames.size} classes)"

                    showClassList()
                }
            } catch (e: Exception) {
                mainHandler.post {
                    dialog.dismiss()
                    Toast.makeText(this@DexEditorActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showClassList() {
        val groups = mutableListOf<List<Map<String, String>>>()
        val children = mutableListOf<List<Map<String, String>>>()

        val packages = linkedMapOf<String, MutableList<String>>()
        for (cls in classNames) {
            val lastDot = cls.lastIndexOf('.')
            val pkg = if (lastDot > 0) cls.substring(0, lastDot) else "(default)"
            val simpleName = if (lastDot > 0) cls.substring(lastDot + 1) else cls
            packages.getOrPut(pkg) { mutableListOf() }.add(simpleName)
        }

        for ((pkg, classes) in packages) {
            groups.add(listOf(mapOf("name" to "$pkg (${classes.size})")))
            children.add(classes.map { mapOf("name" to it) })
        }

        if (groups.isEmpty()) {
            groups.add(listOf(mapOf("name" to "All Classes (${classNames.size})")))
            children.add(classNames.map { mapOf("name" to it.substringAfterLast('.')) })
        }

        val adapter = object : SimpleExpandableListAdapter(
            this@DexEditorActivity,
            groups,
            android.R.layout.simple_expandable_list_item_1,
            arrayOf("name"),
            intArrayOf(android.R.id.text1),
            children,
            android.R.layout.simple_list_item_1,
            arrayOf("name"),
            intArrayOf(android.R.id.text1)
        ) {}

        classListView?.setAdapter(adapter)
        classListView?.setOnChildClickListener { _, _, groupPos, childPos, _ ->
            val pkgNames = packages.keys.toList()
            if (groupPos < pkgNames.size) {
                val pkg = pkgNames[groupPos]
                val classes = packages[pkg]
                if (classes != null && childPos < classes.size) {
                    val simpleName = classes[childPos]
                    val fullName = if (pkg == "(default)") simpleName else "$pkg.$simpleName"
                    showClassSource(fullName)
                }
            }
            true
        }

        for (i in groups.indices) {
            classListView?.expandGroup(i)
        }
    }

    private fun showClassSource(className: String) {
        val dialog = ProgressDialog.show(this, "Loading", "Loading class...", true)
        executor.execute {
            try {
                val jadx = jadxInstance ?: return@execute
                val javaClass = jadx.searchJavaClassByOrigFullName("L${className.replace('.', '/')};")

                val source = if (javaClass != null) {
                    try {
                        javaClass.code
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
                    codeEditor?.setEditable(false)
                    lineNoText?.text = className
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
