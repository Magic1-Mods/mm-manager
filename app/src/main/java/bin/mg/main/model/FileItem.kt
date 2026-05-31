package bin.mg.main.model

import java.io.File

class FileItem(
    var name: String,
    var path: String,
    var size: Long,
    var isDirectory: Boolean,
    var lastModified: Long
) {
    var fileType: FileType = if (isDirectory) FileType.FOLDER else FileType.fromFileName(name)
    var isVirtual: Boolean = false
        private set

    fun isParentDirectory(): Boolean = name == PARENT_DIRECTORY

    fun getExtension(): String {
        if (name.isEmpty()) return ""
        val lastDot = name.lastIndexOf('.')
        if (lastDot < 0 || lastDot >= name.length - 1) return ""
        return name.substring(lastDot + 1)
    }

    fun getExtensionWithCompound(): String {
        if (name.isEmpty()) return ""
        val lastDot = name.lastIndexOf('.')
        if (lastDot < 0 || lastDot >= name.length - 1) return ""

        val parts = name.split(".")
        if (parts.size <= 1) return ""

        val ext = StringBuilder(parts[parts.size - 1])
        for (i in parts.size - 2 downTo 1) {
            val compound = parts[i] + "." + ext.toString()
            if (FileType.fromExtension(compound) != FileType.UNKNOWN) {
                return compound
            }
            ext.insert(0, ".").insert(0, parts[i])
        }

        return parts[parts.size - 1]
    }

    companion object {
        const val PARENT_DIRECTORY = "..."

        @JvmStatic
        fun fromFile(file: File): FileItem {
            return FileItem(
                file.name,
                file.absolutePath,
                file.length(),
                file.isDirectory,
                file.lastModified()
            )
        }

        @JvmStatic
        fun createParentItem(parentPath: String): FileItem {
            return FileItem(PARENT_DIRECTORY, parentPath, 0, true, 0)
        }

        @JvmStatic
        fun createVirtualParent(): FileItem {
            val item = FileItem(PARENT_DIRECTORY, "", 0, true, 0)
            item.isVirtual = true
            return item
        }
    }
}
