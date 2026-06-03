package bin.mg.main.model

import java.util.Locale

enum class FileType {
    FOLDER,
    APK,
    XAPK,
    APKS,
    IMAGE,
    ARCHIVE,
    JAR,
    TEXT,
    SCRIPT,
    JAVA,
    KOTLIN,
    PYTHON,
    HTML,
    PHP,
    DEX,
    ARSC,
    CLASS,
    LUA,
    KEYSTORE,
    UNKNOWN;

    companion object {
        private val EXT_MAP = HashMap<String, FileType>()

        init {
            map(APK, "apk")
            map(XAPK, "xapk")
            map(APKS, "apks")
            map(IMAGE, "png", "jpg", "jpeg", "gif", "bmp", "webp", "svg")
            map(JAR, "jar")
            addCompound("tar.gz", ARCHIVE)
            addCompound("tar.xz", ARCHIVE)
            addCompound("tar.zst", ARCHIVE)
            addCompound("tar.bz2", ARCHIVE)
            addCompound("tar.lz4", ARCHIVE)
            addCompound("tar.lzo", ARCHIVE)
            map(ARCHIVE, "zip", "tar", "7z", "rar", "gz", "xz", "bz2", "lz4", "zst", "zstd", "lzop", "bzip2", "gzip", "lzma")
            map(SCRIPT, "mtsx", "mmsx", "c", "cpp", "xml", "h", "hpp", "cxx", "cs", "a", "s", "ts", "js", "sh", "gradle", "gradle.kts")
            map(TEXT, "txt", "md", "properties", "css", "SF", "MF", "log", "json")
            map(JAVA, "java")
            map(KOTLIN, "kt")
            map(PYTHON, "py")
            map(HTML, "html")
            map(DEX, "dex")
            map(ARSC, "arsc")
            map(CLASS, "class")
            map(LUA, "lua")
            map(KEYSTORE, "keystore")
        }

        private fun map(type: FileType, vararg extensions: String) {
            for (ext in extensions) {
                EXT_MAP[ext] = type
            }
        }

        private fun addCompound(compound: String, type: FileType) {
            EXT_MAP[compound] = type
        }

        @JvmStatic
        fun fromExtension(extension: String?): FileType {
            if (extension == null) return UNKNOWN
            return EXT_MAP[extension.lowercase().trim()] ?: UNKNOWN
        }

        @JvmStatic
        fun fromFileName(fileName: String?): FileType {
            if (fileName == null) return UNKNOWN
            if (fileName == "..") return FOLDER

            val lastDot = fileName.lastIndexOf('.')
            if (lastDot < 0 || lastDot >= fileName.length - 1) {
                return UNKNOWN
            }

            val parts = fileName.split(".")
            if (parts.isEmpty()) return UNKNOWN

            for (i in parts.size - 1 downTo 1) {
                val extBuilder = StringBuilder(parts[i])
                for (j in i + 1 until parts.size) {
                    extBuilder.append(".").append(parts[j])
                }
                val ext = extBuilder.toString().lowercase(Locale.ROOT)
                val type = EXT_MAP[ext]
                if (type != null) {
                    return type
                }
            }

            return UNKNOWN
        }
    }
}
