package bin.mg.main.utils.icon

import android.content.Context
import androidx.core.content.ContextCompat
import bin.mg.main.R
import bin.mg.main.model.FileType

class DefaultIconPlugin : IconPlugin {

    private val iconMap = HashMap<FileType, Int>()
    private val tintMap = HashMap<FileType, Int>()

    init {
        mapIcon(FileType.FOLDER, R.drawable.ic_folder)
        mapIcon(FileType.APK, R.drawable.ic_android)
        mapIcon(FileType.XAPK, R.drawable.ic_android)
        mapIcon(FileType.APKS, R.drawable.ic_android)
        mapIcon(FileType.IMAGE, R.drawable.ic_image)
        mapIcon(FileType.ARCHIVE, R.drawable.ic_archived)
        mapIcon(FileType.JAR, R.drawable.ic_java)
        mapIcon(FileType.LUA, R.drawable.ic_lua)
        mapIcon(FileType.TEXT, R.drawable.ic_text)
        mapIcon(FileType.SCRIPT, R.drawable.ic_script)
        mapIcon(FileType.JAVA, R.drawable.ic_java)
        mapIcon(FileType.KOTLIN, R.drawable.ic_kotlin)
        mapIcon(FileType.PYTHON, R.drawable.ic_python)
        mapIcon(FileType.HTML, R.drawable.ic_html)
        mapIcon(FileType.PHP, R.drawable.ic_php)
        mapIcon(FileType.DEX, R.drawable.ic_dex)
        mapIcon(FileType.ARSC, R.drawable.ic_arsc)
        mapIcon(FileType.CLASS, R.drawable.ic_java)
        mapIcon(FileType.KEYSTORE, R.drawable.ic_keystore)
        mapIcon(FileType.UNKNOWN, R.drawable.ic_file)

        mapIcon(FileType.C, R.drawable.ic_class_c)
        mapIcon(FileType.SHELL, R.drawable.ic_terminal)

        mapTint(FileType.FOLDER, R.color.black)
        mapTint(FileType.APK, R.color.brown_300)
        mapTint(FileType.XAPK, R.color.orange_400)
        mapTint(FileType.APKS, R.color.orange_400)
        mapTint(FileType.IMAGE, R.color.orange_100)
        mapTint(FileType.ARCHIVE, R.color.brown_400)
        mapTint(FileType.JAR, R.color.orange_400)
        mapTint(FileType.LUA, R.color.blue_800)
        mapTint(FileType.TEXT, R.color.blue_800)
        mapTint(FileType.SCRIPT, R.color.blue_800)
        mapTint(FileType.JAVA, R.color.blue_800)
        mapTint(FileType.KOTLIN, R.color.blue_800)
        mapTint(FileType.PYTHON, R.color.blue_800)
        mapTint(FileType.HTML, R.color.cyan_200)
        mapTint(FileType.PHP, R.color.blue_800)
        mapTint(FileType.DEX, R.color.green_500)
        mapTint(FileType.ARSC, R.color.orange_300)
        mapTint(FileType.CLASS, R.color.orange_100)
        mapTint(FileType.KEYSTORE, R.color.grey_600)
        mapTint(FileType.UNKNOWN, R.color.grey_600)

        mapTint(FileType.C, R.color.blue_800)
        mapTint(FileType.CPP, R.color.blue_800)
        mapTint(FileType.CSHARP, R.color.green_500)
        mapTint(FileType.JAVASCRIPT, R.color.orange_300)
        mapTint(FileType.TYPESCRIPT, R.color.blue_800)
        mapTint(FileType.CSS, R.color.cyan_200)
        mapTint(FileType.XML, R.color.orange_200)
        mapTint(FileType.JSON, R.color.orange_300)
        mapTint(FileType.YAML, R.color.cyan_200)
        mapTint(FileType.MARKDOWN, R.color.blue_800)
        mapTint(FileType.SHELL, R.color.green_500)
        mapTint(FileType.GO, R.color.cyan_200)
        mapTint(FileType.RUST, R.color.brown_400)
        mapTint(FileType.SWIFT, R.color.orange_300)
        mapTint(FileType.DART, R.color.cyan_200)
        mapTint(FileType.SCALA, R.color.brown_300)
        mapTint(FileType.RUBY, R.color.brown_300)
        mapTint(FileType.PERL, R.color.blue_800)
        mapTint(FileType.R, R.color.blue_800)
        mapTint(FileType.JULIA, R.color.green_500)
        mapTint(FileType.GROOVY, R.color.orange_200)
        mapTint(FileType.SQL, R.color.orange_200)
        mapTint(FileType.GRADLE, R.color.blue_800)
        mapTint(FileType.PROPERTIES, R.color.orange_300)
        mapTint(FileType.LOG, R.color.grey_600)
        mapTint(FileType.VIDEO, R.color.cyan_200)
        mapTint(FileType.AUDIO, R.color.cyan_200)
        mapTint(FileType.PDF, R.color.brown_300)
        mapTint(FileType.FONT, R.color.blue_800)
        mapTint(FileType.DATABASE, R.color.blue_800)
        mapTint(FileType.CERT, R.color.orange_200)
        mapTint(FileType.ASSEMBLY, R.color.grey_600)
        mapTint(FileType.OBJECT, R.color.grey_600)
        mapTint(FileType.BAT, R.color.grey_600)
        mapTint(FileType.DIFF, R.color.grey_600)
    }

    private fun mapIcon(type: FileType, iconRes: Int) {
        iconMap[type] = iconRes
    }

    private fun mapTint(type: FileType, tintRes: Int) {
        tintMap[type] = tintRes
    }

    override fun getIcon(fileType: FileType, context: Context): IconResult? {
        val resId = iconMap[fileType] ?: return null
        return IconResult(
            drawable = if (resId != 0) ContextCompat.getDrawable(context, resId) else null,
            tintColor = tintMap[fileType]
        )
    }

    override fun getTint(fileType: FileType, context: Context): Int? {
        val resId = tintMap[fileType] ?: return null
        return ContextCompat.getColor(context, resId)
    }
}
