package bin.mg.main.ui.view

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import bin.mg.main.R
import bin.mg.main.model.FileItem
import bin.mg.main.model.FileType
import bin.mg.main.utils.file.FileSystemHelper
import bin.mg.main.utils.icon.IconManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import java.io.File
import java.sql.Date
import java.text.SimpleDateFormat
import java.util.Locale

class ViewHolder(
    itemView: View,
    private val context: Context,
    private val fileHelper: FileSystemHelper,
    private val mainHandler: Handler,
    private val dateFormat: SimpleDateFormat,
    private val isImageThumbnailsEnabled: Boolean,
    private var themeColor: Int,
    private var isDarkMode: Boolean
) : RecyclerView.ViewHolder(itemView) {

    val iconContainer: FrameLayout = itemView.findViewById(R.id.file_icon_container)
    val icon: ImageView = itemView.findViewById(R.id.file_icon)
    val name: TextView = itemView.findViewById(R.id.file_name)
    val date: TextView = itemView.findViewById(R.id.file_date)

    fun setThemeColor(color: Int) {
        themeColor = color
    }

    fun bind(item: FileItem, isDarkMode: Boolean) {
        this.isDarkMode = isDarkMode
        iconContainer.background = null
        icon.visibility = View.VISIBLE
        icon.colorFilter = null
        name.text = item.name

        val textPrimary = if (isDarkMode) 0xFFFFFFFF.toInt() else 0xFF212121.toInt()
        val textSecondary = if (isDarkMode) 0xFFB0B0B0.toInt() else 0xFF757575.toInt()
        name.setTextColor(textPrimary)
        date.setTextColor(textSecondary)

        val fileType = item.fileType

        if (item.isParentDirectory()) {
            setFolderUI(fileType, true)
            date.text = ""
        } else if (item.isDirectory) {
            setFolderUI(FileType.FOLDER, false)
            date.text = formatDate(item.lastModified)
        } else {
            bindFile(item, fileType)
        }
    }

    private fun setFolderUI(type: FileType, isParent: Boolean) {
        val bgColor = if (themeColor != 0) themeColor else 0xFF1976D2.toInt()

        val bg = ContextCompat.getDrawable(context, R.drawable.bg_file)!!.mutate() as GradientDrawable
        bg.setColor(bgColor)

        iconContainer.background = bg
        icon.setImageResource(R.drawable.ic_folder)

        if (isParent) date.text = ""
    }

    private fun bindFile(item: FileItem, fileType: FileType) {
        val bg = ContextCompat.getDrawable(context, R.drawable.bg_file)!!.mutate() as GradientDrawable
        bg.setColor(ContextCompat.getColor(context, IconManager.getIconTint(fileType)))
        iconContainer.background = bg

        if (fileType == FileType.IMAGE && isImageThumbnailsEnabled) {
            icon.visibility = View.VISIBLE
            icon.scaleType = ImageView.ScaleType.CENTER_CROP
            icon.colorFilter = null
        } else if (fileType == FileType.APK) {
            icon.visibility = View.VISIBLE
            icon.scaleType = ImageView.ScaleType.FIT_CENTER
            icon.colorFilter = null
            icon.setImageResource(IconManager.getIconResource(fileType))
        } else {
            icon.visibility = View.VISIBLE
            icon.scaleType = ImageView.ScaleType.CENTER_INSIDE
            icon.setImageResource(IconManager.getIconResource(fileType))
        }

        val dateStr = formatDate(item.lastModified)
        val sizeStr = formatSize(item.size)
        date.text = if (dateStr.isNotEmpty()) "$dateStr  $sizeStr" else sizeStr

        loadThumbnail(item, fileType)
    }

    private fun loadThumbnail(item: FileItem, type: FileType) {
        when (type) {
            FileType.APK -> loadApkIcon(item.path)
            FileType.IMAGE -> if (isImageThumbnailsEnabled) loadImageThumbnail(item.path)
            else -> {}
        }
    }

    private fun loadApkIcon(path: String) {
        fileHelper.execute {
            val bitmap = fileHelper.getApkIcon(context, path)

            if (bitmap != null) {
                mainHandler.post {
                    Glide.with(context)
                        .load(bitmap)
                        .override(96, 96)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(icon)
                }
            }
        }
    }

    private fun loadImageThumbnail(path: String) {
        val file = File(path)
        if (!file.exists()) return
        Glide.with(context)
            .load(file)
            .override(96, 96)
            .centerCrop()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.ic_file)
            .error(R.drawable.ic_file)
            .into(icon)
    }

    private fun formatSize(size: Long): String {
        if (size < 1024) return "$size B"
        if (size < 1024 * 1024) return String.format(Locale.getDefault(), "%.1f KB", size / 1024.0)
        if (size < 1024 * 1024 * 1024) return String.format(Locale.getDefault(), "%.1f MB", size / (1024.0 * 1024))
        return String.format(Locale.getDefault(), "%.1f GB", size / (1024.0 * 1024 * 1024))
    }

    private fun formatDate(ts: Long): String {
        return if (ts <= 0) "" else dateFormat.format(Date(ts))
    }

    companion object {
        private const val DEFAULT_FOLDER_COLOR = 0xFF1976D2.toInt()
    }
}
