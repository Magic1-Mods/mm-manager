package bin.mg.main.file

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import bin.mg.main.R
import bin.mg.main.utils.theme.ThemeManager
import java.io.File

class OpenFileAdapter(
    private val onFileClicked: (Int) -> Unit
) : RecyclerView.Adapter<OpenFileAdapter.FileViewHolder>() {

    data class OpenFile(val path: String, val name: String)

    private val files = mutableListOf<OpenFile>()
    private var activeIndex = 0

    fun setFiles(paths: List<String>, activeIdx: Int) {
        files.clear()
        paths.forEach { path ->
            files.add(OpenFile(path, File(path).name))
        }
        activeIndex = activeIdx
        notifyDataSetChanged()
    }

    fun setActiveIndex(idx: Int) {
        val old = activeIndex
        activeIndex = idx
        notifyItemChanged(old)
        notifyItemChanged(activeIndex)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.drawer_file_item, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val ctx     = holder.itemView.context
        val file    = files[position]
        val isDark  = ThemeManager.isDarkMode(ctx)
        val primary = ThemeManager.primary(ctx)

        holder.filename.text = file.name
        holder.filepath.text = file.path

        if (position == activeIndex) {
            // Highlighted row — tinted background, accent text & icon
            val activeBg = if (isDark)
                ThemeManager.lighten(primary, 0.12f).let { Color.argb(60,
                    (it shr 16) and 0xFF, (it shr 8) and 0xFF, it and 0xFF) }
            else
                ThemeManager.lighten(primary, 0.88f)

            holder.itemView.setBackgroundColor(activeBg)
            holder.filename.setTextColor(ThemeManager.secondary(ctx))
            holder.filepath.setTextColor(ThemeManager.secondary(ctx))
            holder.icon.setColorFilter(ThemeManager.secondary(ctx))
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
            holder.filename.setTextColor(ThemeManager.textPrimary(ctx))
            holder.filepath.setTextColor(ThemeManager.textSecondary(ctx))
            holder.icon.setColorFilter(ThemeManager.textSecondary(ctx))
        }

        holder.itemView.setOnClickListener { onFileClicked(holder.adapterPosition) }
    }

    override fun getItemCount(): Int = files.size

    class FileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView     = view.findViewById(R.id.icon_drawer_file)
        val filename: TextView  = view.findViewById(R.id.text_drawer_filename)
        val filepath: TextView  = view.findViewById(R.id.text_drawer_filepath)
    }
}
