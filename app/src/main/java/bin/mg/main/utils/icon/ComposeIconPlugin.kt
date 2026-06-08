package bin.mg.main.utils.icon

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.View
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import bin.mg.main.model.FileType

class ComposeIconPlugin(context: Context) : IconPlugin {

    private val appContext = context.applicationContext
    private val composeView = ComposeView(appContext)
    private val iconCache = HashMap<FileType, Drawable>()
    private val tintCache = HashMap<FileType, Int>()

    private var currentVector by mutableStateOf<ImageVector?>(null)
    private var currentColor by mutableStateOf(android.graphics.Color.WHITE)
    private var currentSizeDp = 24
    private var initialized = false
    private var renderFailed = false

    private val iconMapping = HashMap<FileType, ImageVector>()
    private val tintMapping = HashMap<FileType, Int>()

    init {
        iconMapping[FileType.FOLDER] = Icons.Default.Folder
        iconMapping[FileType.APK] = Icons.Default.Android
        iconMapping[FileType.XAPK] = Icons.Default.Android
        iconMapping[FileType.APKS] = Icons.Default.Android
        iconMapping[FileType.IMAGE] = Icons.Default.Image
        iconMapping[FileType.ARCHIVE] = Icons.Default.Archive
        iconMapping[FileType.JAR] = Icons.Default.Archive
        iconMapping[FileType.TEXT] = Icons.Default.Description
        iconMapping[FileType.SCRIPT] = Icons.Default.Code
        iconMapping[FileType.JAVA] = Icons.Default.Code
        iconMapping[FileType.KOTLIN] = Icons.Default.Code
        iconMapping[FileType.PYTHON] = Icons.Default.Code
        iconMapping[FileType.HTML] = Icons.Default.Code
        iconMapping[FileType.PHP] = Icons.Default.Code
        iconMapping[FileType.C] = Icons.Default.Code
        iconMapping[FileType.CPP] = Icons.Default.Code
        iconMapping[FileType.CSHARP] = Icons.Default.Code
        iconMapping[FileType.JAVASCRIPT] = Icons.Default.Code
        iconMapping[FileType.TYPESCRIPT] = Icons.Default.Code
        iconMapping[FileType.CSS] = Icons.Default.Code
        iconMapping[FileType.XML] = Icons.Default.Code
        iconMapping[FileType.JSON] = Icons.Default.DataObject
        iconMapping[FileType.YAML] = Icons.Default.Description
        iconMapping[FileType.MARKDOWN] = Icons.Default.Description
        iconMapping[FileType.SHELL] = Icons.Default.Terminal
        iconMapping[FileType.GO] = Icons.Default.Code
        iconMapping[FileType.RUST] = Icons.Default.Code
        iconMapping[FileType.SWIFT] = Icons.Default.Code
        iconMapping[FileType.DART] = Icons.Default.Code
        iconMapping[FileType.SCALA] = Icons.Default.Code
        iconMapping[FileType.RUBY] = Icons.Default.Code
        iconMapping[FileType.PERL] = Icons.Default.Code
        iconMapping[FileType.R] = Icons.Default.Code
        iconMapping[FileType.JULIA] = Icons.Default.Code
        iconMapping[FileType.GROOVY] = Icons.Default.Code
        iconMapping[FileType.SQL] = Icons.Default.Storage
        iconMapping[FileType.GRADLE] = Icons.Default.Settings
        iconMapping[FileType.PROPERTIES] = Icons.Default.Tune
        iconMapping[FileType.LOG] = Icons.Default.Article
        iconMapping[FileType.VIDEO] = Icons.Default.Movie
        iconMapping[FileType.AUDIO] = Icons.Default.MusicNote
        iconMapping[FileType.PDF] = Icons.Default.PictureAsPdf
        iconMapping[FileType.FONT] = Icons.Default.FontDownload
        iconMapping[FileType.DATABASE] = Icons.Default.Storage
        iconMapping[FileType.CERT] = Icons.Default.Lock
        iconMapping[FileType.ASSEMBLY] = Icons.Default.Code
        iconMapping[FileType.OBJECT] = Icons.Default.Storage
        iconMapping[FileType.BAT] = Icons.Default.Terminal
        iconMapping[FileType.DIFF] = Icons.Default.Compare
        iconMapping[FileType.DEX] = Icons.Default.Android
        iconMapping[FileType.ARSC] = Icons.Default.DataObject
        iconMapping[FileType.CLASS] = Icons.Default.Code
        iconMapping[FileType.LUA] = Icons.Default.Code
        iconMapping[FileType.KEYSTORE] = Icons.Default.Lock
        iconMapping[FileType.UNKNOWN] = Icons.Default.InsertDriveFile

        tintMapping[FileType.APK] = 0xFF7CB342.toInt()
        tintMapping[FileType.XAPK] = 0xFFFF9800.toInt()
        tintMapping[FileType.APKS] = 0xFFFF9800.toInt()
        tintMapping[FileType.IMAGE] = 0xFFFFCC80.toInt()
        tintMapping[FileType.ARCHIVE] = 0xFFA1887F.toInt()
        tintMapping[FileType.JAR] = 0xFFFF9800.toInt()
        tintMapping[FileType.TEXT] = 0xFF1565C0.toInt()
        tintMapping[FileType.SCRIPT] = 0xFF1565C0.toInt()
        tintMapping[FileType.JAVA] = 0xFF1565C0.toInt()
        tintMapping[FileType.KOTLIN] = 0xFF1565C0.toInt()
        tintMapping[FileType.PYTHON] = 0xFF1565C0.toInt()
        tintMapping[FileType.HTML] = 0xFF00BCD4.toInt()
        tintMapping[FileType.PHP] = 0xFF1565C0.toInt()
        tintMapping[FileType.DEX] = 0xFF4CAF50.toInt()
        tintMapping[FileType.ARSC] = 0xFFFFB74D.toInt()
        tintMapping[FileType.CLASS] = 0xFFFFCC80.toInt()
        tintMapping[FileType.KEYSTORE] = 0xFF9E9E9E.toInt()
        tintMapping[FileType.UNKNOWN] = 0xFF9E9E9E.toInt()
        tintMapping[FileType.C] = 0xFF1565C0.toInt()
        tintMapping[FileType.CPP] = 0xFF1565C0.toInt()
        tintMapping[FileType.CSHARP] = 0xFF4CAF50.toInt()
        tintMapping[FileType.JAVASCRIPT] = 0xFFFFB74D.toInt()
        tintMapping[FileType.TYPESCRIPT] = 0xFF1565C0.toInt()
        tintMapping[FileType.CSS] = 0xFF00BCD4.toInt()
        tintMapping[FileType.XML] = 0xFFFFB74D.toInt()
        tintMapping[FileType.JSON] = 0xFFFFB74D.toInt()
        tintMapping[FileType.YAML] = 0xFF00BCD4.toInt()
        tintMapping[FileType.MARKDOWN] = 0xFF1565C0.toInt()
        tintMapping[FileType.SHELL] = 0xFF4CAF50.toInt()
        tintMapping[FileType.GO] = 0xFF00BCD4.toInt()
        tintMapping[FileType.RUST] = 0xFFA1887F.toInt()
        tintMapping[FileType.SWIFT] = 0xFFFFB74D.toInt()
        tintMapping[FileType.DART] = 0xFF00BCD4.toInt()
        tintMapping[FileType.SCALA] = 0xFFA1887F.toInt()
        tintMapping[FileType.RUBY] = 0xFFA1887F.toInt()
        tintMapping[FileType.PERL] = 0xFF1565C0.toInt()
        tintMapping[FileType.R] = 0xFF1565C0.toInt()
        tintMapping[FileType.JULIA] = 0xFF4CAF50.toInt()
        tintMapping[FileType.GROOVY] = 0xFFFFB74D.toInt()
        tintMapping[FileType.SQL] = 0xFFFFB74D.toInt()
        tintMapping[FileType.GRADLE] = 0xFF1565C0.toInt()
        tintMapping[FileType.PROPERTIES] = 0xFFFFB74D.toInt()
        tintMapping[FileType.LOG] = 0xFF9E9E9E.toInt()
        tintMapping[FileType.VIDEO] = 0xFF00BCD4.toInt()
        tintMapping[FileType.AUDIO] = 0xFF00BCD4.toInt()
        tintMapping[FileType.PDF] = 0xFFA1887F.toInt()
        tintMapping[FileType.FONT] = 0xFF1565C0.toInt()
        tintMapping[FileType.DATABASE] = 0xFF1565C0.toInt()
        tintMapping[FileType.CERT] = 0xFFFFB74D.toInt()
        tintMapping[FileType.ASSEMBLY] = 0xFF9E9E9E.toInt()
        tintMapping[FileType.OBJECT] = 0xFF9E9E9E.toInt()
        tintMapping[FileType.BAT] = 0xFF9E9E9E.toInt()
        tintMapping[FileType.DIFF] = 0xFF9E9E9E.toInt()

        composeView.setContent {
            val vector = currentVector
            if (vector != null) {
                Icon(
                    imageVector = vector,
                    contentDescription = null,
                    modifier = Modifier.size(currentSizeDp.dp),
                    tint = Color(currentColor)
                )
            }
        }
        initialized = true
    }

    private fun renderIconToDrawable(vector: ImageVector, sizeDp: Int, colorInt: Int): Drawable? {
        if (!initialized || renderFailed) return null
        try {
            currentVector = vector
            currentColor = colorInt
            currentSizeDp = sizeDp

            val density = appContext.resources.displayMetrics.density
            val sizePx = (sizeDp * density).toInt()

            composeView.measure(
                View.MeasureSpec.makeMeasureSpec(sizePx, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(sizePx, View.MeasureSpec.EXACTLY)
            )
            composeView.layout(0, 0, sizePx, sizePx)

            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            composeView.draw(canvas)

            return BitmapDrawable(appContext.resources, bitmap)
        } catch (e: Exception) {
            renderFailed = true
            return null
        }
    }

    override fun getIcon(fileType: FileType, context: Context): IconResult? {
        if (renderFailed) return null

        iconCache[fileType]?.let { return IconResult(it, tintCache[fileType]) }

        val vector = iconMapping[fileType] ?: Icons.Default.InsertDriveFile
        val colorInt = tintMapping[fileType] ?: android.graphics.Color.WHITE
        val drawable = renderIconToDrawable(vector, 24, colorInt)
        if (drawable != null) {
            iconCache[fileType] = drawable
            tintCache[fileType] = colorInt
            return IconResult(drawable, colorInt)
        }
        return null
    }

    override fun getTint(fileType: FileType, context: Context): Int? {
        return tintMapping[fileType]
    }
}
