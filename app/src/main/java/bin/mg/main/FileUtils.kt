package bin.mg.main

import android.os.Environment
import bin.mg.main.model.FileItem
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale

object FileUtils {

    const val ROOT_PATH = "/"
    const val STORAGE_PATH = "/storage"
    const val EMULATED_PATH = "/storage/emulated"

    @JvmStatic
    fun listFiles(directoryPath: String): MutableList<FileItem> {
        val items = mutableListOf<FileItem>()
        val directory = File(directoryPath)

        if (!directory.exists()) return items
        if (!directory.isDirectory) return items

        val files = directory.listFiles() ?: return items

        files.sortWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase(Locale.ROOT) })

        for (file in files) {
            items.add(FileItem.fromFile(file))
        }

        return items
    }

    @JvmStatic
    fun listRootDirectories(): MutableList<FileItem> {
        val items = mutableListOf<FileItem>()
        val root = File(ROOT_PATH)
        val roots = root.listFiles()

        if (roots != null) {
            roots.sortBy { it.name.lowercase(Locale.ROOT) }
            for (file in roots) {
                items.add(FileItem.fromFile(file))
            }
        }

        return items
    }

    @JvmStatic
    fun listStorageDirectories(): MutableList<FileItem> {
        val items = mutableListOf<FileItem>()
        val storage = File(STORAGE_PATH)
        val storages = storage.listFiles()

        if (storages != null) {
            storages.sortBy { it.name.lowercase(Locale.ROOT) }
            for (file in storages) {
                items.add(FileItem.fromFile(file))
            }
        }

        val emulated = File(EMULATED_PATH)
        val emulateds = emulated.listFiles()

        if (emulateds != null) {
            for (file in emulateds) {
                items.add(FileItem.fromFile(file))
            }
        }

        return items
    }

    @JvmStatic
    fun hasParent(path: String?): Boolean {
        if (path.isNullOrEmpty()) return false
        if (path == ROOT_PATH) return false
        return true
    }

    @JvmStatic
    fun getParentPath(path: String?): String? {
        if (path.isNullOrEmpty()) return null
        val file = File(path)
        val parent = file.parentFile
        return parent?.absolutePath
    }

    @JvmStatic
    fun copy(src: File, dest: File): Boolean {
        return if (src.isDirectory) {
            copyDirectory(src, dest)
        } else {
            copyFile(src, dest)
        }
    }

    private fun copyFile(src: File, dest: File): Boolean {
        try {
            val input = FileInputStream(src)
            val output = FileOutputStream(dest)
            val buffer = ByteArray(8192)
            var length: Int
            while (input.read(buffer).also { length = it } > 0) {
                output.write(buffer, 0, length)
            }
            input.close()
            output.close()
            dest.setLastModified(src.lastModified())
            return true
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
    }

    private fun copyDirectory(src: File, dest: File): Boolean {
        if (!dest.exists()) {
            dest.mkdirs()
        }
        val children = src.listFiles()
        if (children != null) {
            for (child in children) {
                val newDest = File(dest, child.name)
                if (!copy(child, newDest)) {
                    return false
                }
            }
        }
        return true
    }

    @JvmStatic
    fun move(src: File, dest: File): Boolean {
        return if (src.renameTo(dest)) {
            true
        } else {
            if (copy(src, dest)) {
                delete(src)
            } else {
                false
            }
        }
    }

    @JvmStatic
    fun delete(file: File): Boolean {
        if (file.isDirectory) {
            val children = file.listFiles()
            if (children != null) {
                for (child in children) {
                    if (!delete(child)) {
                        return false
                    }
                }
            }
        }
        return file.delete()
    }

    @JvmStatic
    fun rename(oldFile: File, newName: String): Boolean {
        val parent = oldFile.parentFile
        val newFile = File(parent, newName)
        return oldFile.renameTo(newFile)
    }

    @JvmStatic
    fun createFolder(parentPath: String, folderName: String): Boolean {
        val parent = File(parentPath)
        val newFolder = File(parent, folderName)
        return newFolder.mkdirs()
    }

    @JvmStatic
    fun getMimeType(filePath: String?): String {
        if (filePath == null) return "*/*"

        var extension = ""
        val i = filePath.lastIndexOf('.')
        if (i > 0) {
            extension = filePath.substring(i + 1).lowercase(Locale.ROOT)
        }

        return when (extension) {
            "txt", "log", "md", "json", "xml" -> "text/plain"
            "pdf" -> "application/pdf"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "apk" -> "application/vnd.android.package-archive"
            "mp4", "mkv", "avi" -> "video/*"
            "mp3", "wav", "flac" -> "audio/*"
            "zip", "rar", "7z" -> "application/zip"
            else -> "*/*"
        }
    }

    @JvmStatic
    fun isTextFile(filePath: String?): Boolean {
        if (filePath == null) return false
        val lower = filePath.lowercase(Locale.ROOT)
        return lower.endsWith(".txt") || lower.endsWith(".log") ||
                lower.endsWith(".md") || lower.endsWith(".json") ||
                lower.endsWith(".xml") || lower.endsWith(".properties") ||
                lower.endsWith(".smali") || lower.endsWith(".java") ||
                lower.endsWith(".kt") || lower.endsWith(".gradle") ||
                lower.endsWith(".html") || lower.endsWith(".css") ||
                lower.endsWith(".js") || lower.endsWith(".py")
    }

    @JvmStatic
    fun isXmlFile(filePath: String?): Boolean {
        if (filePath == null) return false
        return filePath.lowercase(Locale.ROOT).endsWith(".xml")
    }

    @JvmStatic
    fun isApkFile(filePath: String?): Boolean {
        if (filePath == null) return false
        val lower = filePath.lowercase(Locale.ROOT)
        return lower.endsWith(".apk") || lower.endsWith(".xapk") || lower.endsWith(".apks")
    }
}
