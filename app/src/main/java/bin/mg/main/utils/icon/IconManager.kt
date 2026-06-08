package bin.mg.main.utils.icon

import bin.mg.main.R
import bin.mg.main.model.FileType

object IconManager {

    private val ICON_MAP = HashMap<FileType, Int>()
    private val TINT_MAP = HashMap<FileType, Int>()

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
        mapIcon(FileType.UNKNOWN, R.drawable.ic_file)
        mapIcon(FileType.KEYSTORE, R.drawable.ic_keystore)
        mapIcon(FileType.C, R.drawable.ic_class_c)
        mapIcon(FileType.CPP, R.drawable.ic_class_c)
        mapIcon(FileType.CSHARP, R.drawable.ic_script)
        mapIcon(FileType.JAVASCRIPT, R.drawable.ic_script)
        mapIcon(FileType.TYPESCRIPT, R.drawable.ic_script)
        mapIcon(FileType.CSS, R.drawable.ic_code)
        mapIcon(FileType.XML, R.drawable.ic_code)
        mapIcon(FileType.JSON, R.drawable.ic_code)
        mapIcon(FileType.YAML, R.drawable.ic_code)
        mapIcon(FileType.MARKDOWN, R.drawable.ic_text)
        mapIcon(FileType.SHELL, R.drawable.ic_terminal)
        mapIcon(FileType.GO, R.drawable.ic_script)
        mapIcon(FileType.RUST, R.drawable.ic_script)
        mapIcon(FileType.SWIFT, R.drawable.ic_script)
        mapIcon(FileType.DART, R.drawable.ic_script)
        mapIcon(FileType.SCALA, R.drawable.ic_script)
        mapIcon(FileType.RUBY, R.drawable.ic_script)
        mapIcon(FileType.PERL, R.drawable.ic_script)
        mapIcon(FileType.R, R.drawable.ic_script)
        mapIcon(FileType.JULIA, R.drawable.ic_script)
        mapIcon(FileType.GROOVY, R.drawable.ic_script)
        mapIcon(FileType.SQL, R.drawable.ic_code)
        mapIcon(FileType.GRADLE, R.drawable.ic_script)
        mapIcon(FileType.PROPERTIES, R.drawable.ic_text)
        mapIcon(FileType.LOG, R.drawable.ic_text)
        mapIcon(FileType.VIDEO, R.drawable.ic_image)
        mapIcon(FileType.AUDIO, R.drawable.ic_image)
        mapIcon(FileType.PDF, R.drawable.ic_file)
        mapIcon(FileType.FONT, R.drawable.ic_file)
        mapIcon(FileType.DATABASE, R.drawable.ic_file)
        mapIcon(FileType.CERT, R.drawable.ic_keystore)
        mapIcon(FileType.ASSEMBLY, R.drawable.ic_class_c)
        mapIcon(FileType.OBJECT, R.drawable.ic_file)
        mapIcon(FileType.BAT, R.drawable.ic_terminal)
        mapIcon(FileType.DIFF, R.drawable.ic_text)

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
        ICON_MAP[type] = iconRes
    }

    private fun mapTint(type: FileType, tintRes: Int) {
        TINT_MAP[type] = tintRes
    }

    @JvmStatic
    fun getIconResource(fileType: FileType?): Int {
        if (fileType == null) return R.drawable.ic_file
        return ICON_MAP[fileType] ?: R.drawable.ic_file
    }

    @JvmStatic
    fun getBackgroundResource(): Int {
        return R.drawable.bg_file
    }

    @JvmStatic
    fun getIconTint(fileType: FileType?): Int {
        if (fileType == null) return R.color.grey_300
        return TINT_MAP[fileType] ?: R.color.grey_300
    }
}
