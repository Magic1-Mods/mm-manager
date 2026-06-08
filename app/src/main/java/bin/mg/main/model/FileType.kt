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
    C,
    CPP,
    CSHARP,
    JAVASCRIPT,
    TYPESCRIPT,
    CSS,
    XML,
    JSON,
    YAML,
    MARKDOWN,
    SHELL,
    GO,
    RUST,
    SWIFT,
    DART,
    SCALA,
    RUBY,
    PERL,
    R,
    JULIA,
    GROOVY,
    SQL,
    GRADLE,
    PROPERTIES,
    LOG,
    VIDEO,
    AUDIO,
    PDF,
    FONT,
    DATABASE,
    CERT,
    ASSEMBLY,
    OBJECT,
    BAT,
    DIFF,
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
            map(IMAGE, "png", "jpg", "jpeg", "gif", "bmp", "webp", "svg", "ico", "raw", "tiff", "tif", "heic", "heif", "avif")
            map(JAR, "jar", "war", "ear")
            addCompound("tar.gz", ARCHIVE)
            addCompound("tar.xz", ARCHIVE)
            addCompound("tar.zst", ARCHIVE)
            addCompound("tar.bz2", ARCHIVE)
            addCompound("tar.lz4", ARCHIVE)
            addCompound("tar.lzo", ARCHIVE)
            map(ARCHIVE, "zip", "tar", "7z", "rar", "gz", "xz", "bz2", "lz4", "zst", "zstd", "lzop", "bzip2", "gzip", "lzma", "cab", "iso")
            addCompound("gradle.kts", GRADLE)
            map(GRADLE, "gradle")
            map(SCRIPT, "mtsx", "mmsx")
            map(TEXT, "txt", "SF", "MF")
            map(JAVA, "java")
            map(KOTLIN, "kt")
            map(PYTHON, "py", "pyw", "pyx", "ipynb")
            map(HTML, "html", "htm", "xhtml")
            map(PHP, "php", "phtml")
            map(C, "c", "h")
            map(CPP, "cpp", "hpp", "cxx", "hxx", "cc", "hh")
            map(CSHARP, "cs")
            map(JAVASCRIPT, "js", "mjs", "cjs", "jsx")
            map(TYPESCRIPT, "ts", "tsx")
            map(CSS, "css", "scss", "sass", "less", "styl")
            map(XML, "xml", "xsd", "xsl", "xslt", "dtd", "plist")
            map(JSON, "json")
            map(YAML, "yaml", "yml")
            map(MARKDOWN, "md", "mdown", "markdown")
            map(SHELL, "sh", "bash", "zsh", "fish", "ksh")
            map(GO, "go")
            map(RUST, "rs")
            map(SWIFT, "swift")
            map(DART, "dart")
            map(SCALA, "scala")
            map(RUBY, "rb", "rbx", "gem")
            map(PERL, "pl", "pm")
            map(R, "r", "rmd")
            map(JULIA, "jl")
            map(GROOVY, "groovy", "gvy", "gsh")
            map(SQL, "sql")
            map(PROPERTIES, "properties", "prop", "ini", "cfg", "conf", "env", "editorconfig")
            map(LOG, "log")
            map(VIDEO, "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "3gp", "mpg", "mpeg")
            map(AUDIO, "mp3", "wav", "ogg", "flac", "aac", "wma", "m4a", "opus", "aiff", "alac")
            map(PDF, "pdf")
            map(FONT, "ttf", "otf", "woff", "woff2", "eot")
            map(DATABASE, "db", "sqlite", "sqlite3", "db3")
            map(CERT, "pem", "crt", "cer", "p12", "pfx", "der", "key", "csr")
            map(ASSEMBLY, "asm", "s", "inc")
            map(OBJECT, "o", "obj", "lib", "a", "so", "dylib", "dll")
            map(BAT, "bat", "cmd", "ps1")
            map(DIFF, "diff", "patch")
            map(DEX, "dex")
            map(ARSC, "arsc")
            map(CLASS, "class")
            map(LUA, "lua")
            map(KEYSTORE, "keystore", "jks", "bks")
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
            if (parts.size < 2) return UNKNOWN

            for (i in 1 until parts.size) {
                val extBuilder = StringBuilder()
                for (j in i until parts.size) {
                    if (j > i) extBuilder.append(".")
                    extBuilder.append(parts[j])
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
