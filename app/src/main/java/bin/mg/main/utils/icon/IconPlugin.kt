package bin.mg.main.utils.icon

import android.content.Context
import android.graphics.drawable.Drawable
import bin.mg.main.model.FileType

data class IconResult(
    val drawable: Drawable?,
    val tintColor: Int? = null
)

interface IconPlugin {
    fun getIcon(fileType: FileType, context: Context): IconResult?
    fun getTint(fileType: FileType, context: Context): Int?
}
