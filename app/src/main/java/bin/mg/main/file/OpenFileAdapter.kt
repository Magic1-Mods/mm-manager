package bin.mg.main.file

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import bin.mg.main.R
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
            val file = File(path)
            files.add(OpenFile(path, file.name))
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
        val view = LayoutInflater.from(parent.context).inflate(R.layout.drawer_file_item, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val file = files[position]
        holder.filename.text = file.name
        holder.filepath.text = file.path

        if (position == activeIndex) {
            holder.itemView.setBackgroundColor(Color.parseColor("#E3F2FD"))
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
        }

        holder.itemView.setOnClickListener {
            onFileClicked(holder.adapterPosition)
        }
    }

    override fun getItemCount(): Int = files.size

    class FileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val filename: TextView = view.findViewById(R.id.text_drawer_filename)
        val filepath: TextView = view.findViewById(R.id.text_drawer_filepath)
    }
}
