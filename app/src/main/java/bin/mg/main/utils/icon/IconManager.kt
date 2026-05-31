package bin.mg.main.utils.icon

import bin.mg.main.R
import bin.mg.main.model.FileType

object IconManager {

    private val ICON_MAP = HashMap<FileType, Int>()
    private val TINT_MAP = HashMap<FileType, Int>()

    init {
        // Map icons
        mapIcon(FileType.FOLDER, R.drawable.ic_folder)
        mapIcon(FileType.APK, R.drawable.ic_android)
        mapIcon(FileType.XAPK, R.drawable.ic_android)
        mapIcon(FileType.APKS, R.drawable.ic_android)
        mapIcon(FileType.IMAGE, R.drawable.ic_image)
        mapIcon(FileType.ARCHIVE, R.drawable.ic_archived)
        mapIcon(FileType.JAR, R.drawable.ic_java)
        mapIcon(FileType.LUA, R.drawable.ic_lua)
        mapIcon(FileType.TEXT, R.drawable.ic_code)
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

        mapTint(FileType.FOLDER, R.color.black)
        mapTint(FileType.APK, R.color.brown_300)
        mapTint(FileType.XAPK, R.color.orange_400)
        mapTint(FileType.APKS, R.color.orange_400)
        mapTint(FileType.IMAGE, R.color.orange_100)
        mapTint(FileType.ARCHIVE, R.color.brown_400)
        mapTint(FileType.JAR, R.color.orange_400)
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
        mapTint(FileType.UNKNOWN, R.color.grey_600)
        mapTint(FileType.KEYSTORE, R.color.grey_600)
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
