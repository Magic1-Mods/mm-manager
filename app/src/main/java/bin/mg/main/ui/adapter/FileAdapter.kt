package bin.mg.main.ui.adapter

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import bin.mg.main.R
import bin.mg.main.model.FileItem
import bin.mg.main.ui.view.ViewHolder
import bin.mg.main.utils.file.FileSystemHelper
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Locale

class FileAdapter(private val context: Context) : RecyclerView.Adapter<ViewHolder>() {

    private val items = mutableListOf<FileItem>()
    private val dateFormat = SimpleDateFormat("yy-MM-dd HH:mm", Locale.getDefault())
    private val mainHandler = Handler(Looper.getMainLooper())
    private val fileHelper = FileSystemHelper.getInstance(context)
    private var themeColor = 0
    private var isDarkMode = false
    private var clickListener: OnItemClickListener? = null
    var isImageThumbnailsEnabled = true

    interface OnItemClickListener {
        fun onItemClick(view: View, position: Int, item: FileItem)
    }

    fun setOnItemClickListener(listener: OnItemClickListener?) {
        clickListener = listener
    }

    fun setThemeColor(color: Int, isDarkMode: Boolean) {
        themeColor = color
        this.isDarkMode = isDarkMode
        notifyDataSetChanged()
    }

    fun getThemeColor(): Int = themeColor

    fun isDarkMode(): Boolean = isDarkMode

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item, parent, false)

        return ViewHolder(
            view, context, fileHelper, mainHandler, dateFormat,
            isImageThumbnailsEnabled, themeColor, isDarkMode
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.setThemeColor(themeColor)
        val item = items[position]
        holder.bind(item, isDarkMode)

        holder.itemView.setOnClickListener { v ->
            clickListener?.onItemClick(v, position, item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<FileItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        try {
            Glide.with(holder.itemView.context).clear(holder.icon)
            holder.icon.setImageDrawable(null)
            holder.iconContainer.background = null
        } catch (ignored: Exception) {
        }
    }

    fun getItems(): List<FileItem> = ArrayList(items)
}
