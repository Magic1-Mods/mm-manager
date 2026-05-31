package bin.mg.main.handlers

import android.content.Context
import android.view.View
import bin.mg.main.model.FileItem

interface FileClickHandler {
    fun onClick(context: Context, item: FileItem, anchor: View)
    fun onLongClick(context: Context, item: FileItem, anchor: View)
}
