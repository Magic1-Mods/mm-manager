package bin.mg.main.utils.file

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import bin.mg.main.model.FileItem
import java.io.File
import java.util.Locale
import java.util.concurrent.Executors

class FileSystemHelper private constructor(context: Context) {

    private val appContext: Context = context.applicationContext
    private val executor = Executors.newFixedThreadPool(4)
    private val cache: LruCache<String, List<FileItem>>

    init {
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = maxMemory / 8
        cache = object : LruCache<String, List<FileItem>>(cacheSize) {
            override fun sizeOf(key: String, items: List<FileItem>): Int {
                return items.size
            }
        }
    }

    fun execute(runnable: Runnable) {
        executor.execute(runnable)
    }

    fun listDirectory(path: String): List<FileItem> {
        val cached = cache.get(path)
        if (cached != null) {
            return ArrayList(cached)
        }

        val items = mutableListOf<FileItem>()
        val directory = File(path)

        if (!directory.exists() || !directory.isDirectory) {
            return items
        }

        val files = directory.listFiles() ?: return items

        files.sortWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase(Locale.ROOT) })

        for (file in files) {
            items.add(FileItem.fromFile(file))
        }

        cache.put(path, ArrayList(items))
        return items
    }

    fun invalidateCache(path: String) {
        cache.remove(path)
        val parent = File(path).parentFile
        if (parent != null) {
            cache.remove(parent.absolutePath)
        }
    }

    fun invalidateAllCache() {
        cache.evictAll()
    }

    fun canNavigateTo(path: String): Boolean {
        val file = File(path)
        return file.exists() && file.isDirectory
    }

    fun canRead(path: String): Boolean {
        return File(path).canRead()
    }

    fun getParentPath(path: String): String? {
        val file = File(path)
        val parent = file.parentFile
        return parent?.absolutePath
    }

    fun shouldShowParent(path: String?): Boolean {
        return path != null && path != "/"
    }

    fun getApkIcon(context: Context, apkPath: String): Bitmap? {
        try {
            val pm = context.packageManager
            var flags = PackageManager.GET_META_DATA
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                flags = flags or PackageManager.GET_SIGNING_CERTIFICATES
            }
            val pkgInfo = pm.getPackageArchiveInfo(apkPath, flags)
            if (pkgInfo != null && pkgInfo.applicationInfo != null) {
                pkgInfo.applicationInfo!!.sourceDir = apkPath
                pkgInfo.applicationInfo!!.publicSourceDir = apkPath
                val icon = pkgInfo.applicationInfo!!.loadIcon(pm)
                if (icon != null) {
                    return drawableToBitmap(icon)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap? {
        if (drawable is BitmapDrawable) {
            val bitmap = drawable.bitmap
            if (bitmap != null) return bitmap
        }

        var intrinsicWidth = drawable.intrinsicWidth
        var intrinsicHeight = drawable.intrinsicHeight
        if (intrinsicWidth <= 0) intrinsicWidth = 96
        if (intrinsicHeight <= 0) intrinsicHeight = 96

        val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    fun shutdown() {
        executor.shutdown()
    }

    companion object {
        @Volatile
        private var instance: FileSystemHelper? = null

        @JvmStatic
        fun getInstance(context: Context): FileSystemHelper {
            return instance ?: synchronized(this) {
                instance ?: FileSystemHelper(context).also { instance = it }
            }
        }
    }
}
