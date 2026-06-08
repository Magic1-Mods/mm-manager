package bin.mg.main.utils.icon

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import bin.mg.main.R
import bin.mg.main.model.FileType

object IconManager {

    private val plugins = mutableListOf<IconPlugin>()
    private var initialized = false

    fun initialize(context: Context) {
        if (initialized) return
        initialized = true
        plugins.add(ComposeIconPlugin(context))
        plugins.add(DefaultIconPlugin())
    }

    private val resourceFallback = HashMap<FileType, Int>()
    private val tintFallback = HashMap<FileType, Int>()

    init {
        resourceFallback[FileType.FOLDER] = R.drawable.ic_folder
        resourceFallback[FileType.APK] = R.drawable.ic_android
        resourceFallback[FileType.XAPK] = R.drawable.ic_android
        resourceFallback[FileType.APKS] = R.drawable.ic_android
        resourceFallback[FileType.IMAGE] = R.drawable.ic_image
        resourceFallback[FileType.ARCHIVE] = R.drawable.ic_archived
        resourceFallback[FileType.JAR] = R.drawable.ic_java
        resourceFallback[FileType.LUA] = R.drawable.ic_lua
        resourceFallback[FileType.TEXT] = R.drawable.ic_text
        resourceFallback[FileType.SCRIPT] = R.drawable.ic_script
        resourceFallback[FileType.JAVA] = R.drawable.ic_java
        resourceFallback[FileType.KOTLIN] = R.drawable.ic_kotlin
        resourceFallback[FileType.PYTHON] = R.drawable.ic_python
        resourceFallback[FileType.HTML] = R.drawable.ic_html
        resourceFallback[FileType.PHP] = R.drawable.ic_php
        resourceFallback[FileType.DEX] = R.drawable.ic_dex
        resourceFallback[FileType.ARSC] = R.drawable.ic_arsc
        resourceFallback[FileType.CLASS] = R.drawable.ic_java
        resourceFallback[FileType.KEYSTORE] = R.drawable.ic_keystore
        resourceFallback[FileType.UNKNOWN] = R.drawable.ic_file
        resourceFallback[FileType.C] = R.drawable.ic_class_c
        resourceFallback[FileType.SHELL] = R.drawable.ic_terminal

        tintFallback[FileType.FOLDER] = R.color.black
        tintFallback[FileType.APK] = R.color.brown_300
        tintFallback[FileType.XAPK] = R.color.orange_400
        tintFallback[FileType.APKS] = R.color.orange_400
        tintFallback[FileType.IMAGE] = R.color.orange_100
        tintFallback[FileType.ARCHIVE] = R.color.brown_400
        tintFallback[FileType.JAR] = R.color.orange_400
        tintFallback[FileType.LUA] = R.color.blue_800
        tintFallback[FileType.TEXT] = R.color.blue_800
        tintFallback[FileType.SCRIPT] = R.color.blue_800
        tintFallback[FileType.JAVA] = R.color.blue_800
        tintFallback[FileType.KOTLIN] = R.color.blue_800
        tintFallback[FileType.PYTHON] = R.color.blue_800
        tintFallback[FileType.HTML] = R.color.cyan_200
        tintFallback[FileType.PHP] = R.color.blue_800
        tintFallback[FileType.DEX] = R.color.green_500
        tintFallback[FileType.ARSC] = R.color.orange_300
        tintFallback[FileType.CLASS] = R.color.orange_100
        tintFallback[FileType.KEYSTORE] = R.color.grey_600
        tintFallback[FileType.UNKNOWN] = R.color.grey_600
        tintFallback[FileType.C] = R.color.blue_800
        tintFallback[FileType.CPP] = R.color.blue_800
        tintFallback[FileType.CSHARP] = R.color.green_500
        tintFallback[FileType.JAVASCRIPT] = R.color.orange_300
        tintFallback[FileType.TYPESCRIPT] = R.color.blue_800
        tintFallback[FileType.CSS] = R.color.cyan_200
        tintFallback[FileType.XML] = R.color.orange_200
        tintFallback[FileType.JSON] = R.color.orange_300
        tintFallback[FileType.YAML] = R.color.cyan_200
        tintFallback[FileType.MARKDOWN] = R.color.blue_800
        tintFallback[FileType.SHELL] = R.color.green_500
        tintFallback[FileType.GO] = R.color.cyan_200
        tintFallback[FileType.RUST] = R.color.brown_400
        tintFallback[FileType.SWIFT] = R.color.orange_300
        tintFallback[FileType.DART] = R.color.cyan_200
        tintFallback[FileType.SCALA] = R.color.brown_300
        tintFallback[FileType.RUBY] = R.color.brown_300
        tintFallback[FileType.PERL] = R.color.blue_800
        tintFallback[FileType.R] = R.color.blue_800
        tintFallback[FileType.JULIA] = R.color.green_500
        tintFallback[FileType.GROOVY] = R.color.orange_200
        tintFallback[FileType.SQL] = R.color.orange_200
        tintFallback[FileType.GRADLE] = R.color.blue_800
        tintFallback[FileType.PROPERTIES] = R.color.orange_300
        tintFallback[FileType.LOG] = R.color.grey_600
        tintFallback[FileType.VIDEO] = R.color.cyan_200
        tintFallback[FileType.AUDIO] = R.color.cyan_200
        tintFallback[FileType.PDF] = R.color.brown_300
        tintFallback[FileType.FONT] = R.color.blue_800
        tintFallback[FileType.DATABASE] = R.color.blue_800
        tintFallback[FileType.CERT] = R.color.orange_200
        tintFallback[FileType.ASSEMBLY] = R.color.grey_600
        tintFallback[FileType.OBJECT] = R.color.grey_600
        tintFallback[FileType.BAT] = R.color.grey_600
        tintFallback[FileType.DIFF] = R.color.grey_600
    }

    @JvmStatic
    fun getIconResource(fileType: FileType?): Int {
        if (fileType == null) return R.drawable.ic_file
        return resourceFallback[fileType] ?: R.drawable.ic_file
    }

    @JvmStatic
    fun getBackgroundResource(): Int {
        return R.drawable.bg_file
    }

    @JvmStatic
    fun getIconTint(fileType: FileType?): Int {
        if (fileType == null) return R.color.grey_300
        return tintFallback[fileType] ?: R.color.grey_300
    }

    @JvmStatic
    fun getIconDrawable(fileType: FileType?, context: Context): Drawable? {
        val ft = fileType ?: FileType.UNKNOWN
        if (!initialized) initialize(context)

        for (plugin in plugins) {
            try {
                val result = plugin.getIcon(ft, context)
                if (result?.drawable != null) return result.drawable
            } catch (_: Exception) {
            }
        }

        val resId = getIconResource(ft)
        return if (resId != 0) ContextCompat.getDrawable(context, resId) else null
    }
}
