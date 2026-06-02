package bin.mg.main.utils.theme

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import bin.mg.main.R

object ThemeManager {

    private const val PREFS_NAME = "MagicManagerPrefs"
    private const val KEY_THEME_PALETTE = "themePalette"
    private const val KEY_FOLLOW_SYSTEM_THEME = "followSystemTheme"

    val PALETTES = arrayOf(
        intArrayOf(0xFF212121.toInt(), 0xFF424242.toInt()),
        intArrayOf(0xFF1565C0.toInt(), 0xFF1E88E5.toInt()),
        intArrayOf(0xFF6A1B9A.toInt(), 0xFF8E24AA.toInt()),
        intArrayOf(0xFF2E7D32.toInt(), 0xFF43A047.toInt()),
        intArrayOf(0xFFE65100.toInt(), 0xFFF57C00.toInt()),
        intArrayOf(0xFFC2185B.toInt(), 0xFFD81B60.toInt()),
        intArrayOf(0xFF00838F.toInt(), 0xFF00ACC1.toInt()),
        intArrayOf(0xFF4527A0.toInt(), 0xFF5E35B1.toInt()),
        intArrayOf(0xFFB71C1C.toInt(), 0xFFE53935.toInt()),
        intArrayOf(0xFF1A237E.toInt(), 0xFF3949AB.toInt()),
        intArrayOf(0xFF00695C.toInt(), 0xFF00897B.toInt()),
        intArrayOf(0xFF0277BD.toInt(), 0xFF00838F.toInt()),
        intArrayOf(0xFF4A148C.toInt(), 0xFF6A1B9A.toInt()),
        intArrayOf(0xFFBF360C.toInt(), 0xFFE64A19.toInt()),
        intArrayOf(0xFF1B5E20.toInt(), 0xFF2E7D32.toInt()),
    )

    val THEME_NAMES = arrayOf(
        "AppTheme",
        "AppTheme.Palette1", "AppTheme.Palette2", "AppTheme.Palette3",
        "AppTheme.Palette4", "AppTheme.Palette5", "AppTheme.Palette6",
        "AppTheme.Palette7", "AppTheme.Palette8", "AppTheme.Palette9",
        "AppTheme.Palette10", "AppTheme.Palette11", "AppTheme.Palette12",
        "AppTheme.Palette13", "AppTheme.Palette14"
    )

    fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun currentPalette(ctx: Context): Int = prefs(ctx).getInt(KEY_THEME_PALETTE, 0)

    fun followSystemTheme(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_FOLLOW_SYSTEM_THEME, false)

    fun isDarkMode(ctx: Context): Boolean {
        val follow = followSystemTheme(ctx)
        if (!follow) return false
        val mode = ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return mode == Configuration.UI_MODE_NIGHT_YES
    }

    fun primary(ctx: Context): Int {
        return if (isDarkMode(ctx)) 0xFF212121.toInt() else PALETTES[currentPalette(ctx)][0]
    }

    fun secondary(ctx: Context): Int {
        return if (isDarkMode(ctx)) 0xFF424242.toInt() else PALETTES[currentPalette(ctx)][1]
    }

    fun accent(ctx: Context): Int = secondary(ctx)

    fun lighten(color: Int, factor: Float): Int {
        val a = (color shr 24) and 0xFF
        var r = (color shr 16) and 0xFF
        var g = (color shr 8) and 0xFF
        var b = color and 0xFF
        r = minOf(255, (r + (255 - r) * factor).toInt())
        g = minOf(255, (g + (255 - g) * factor).toInt())
        b = minOf(255, (b + (255 - b) * factor).toInt())
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    fun darken(color: Int, factor: Float): Int {
        val a = (color shr 24) and 0xFF
        var r = (color shr 16) and 0xFF
        var g = (color shr 8) and 0xFF
        var b = color and 0xFF
        r = maxOf(0, (r * (1 - factor)).toInt())
        g = maxOf(0, (g * (1 - factor)).toInt())
        b = maxOf(0, (b * (1 - factor)).toInt())
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    fun mainBackground(ctx: Context): Int =
        if (isDarkMode(ctx)) 0xFF212121.toInt() else lighten(primary(ctx), 0.92f)

    fun panelBackground(ctx: Context): Int =
        if (isDarkMode(ctx)) 0xFF303030.toInt() else 0xFFFFFFFF.toInt()

    fun toolbarBackground(ctx: Context): Int = primary(ctx)

    fun drawerBackground(ctx: Context): Int =
        if (isDarkMode(ctx)) 0xFF303030.toInt() else lighten(primary(ctx), 0.92f)

    fun textPrimary(ctx: Context): Int =
        if (isDarkMode(ctx)) 0xFFFFFFFF.toInt() else darken(primary(ctx), 0.5f)

    fun textSecondary(ctx: Context): Int =
        if (isDarkMode(ctx)) 0xFFB0B0B0.toInt() else lighten(primary(ctx), 0.35f)

    fun dividerColor(ctx: Context): Int =
        if (isDarkMode(ctx)) 0xFF424242.toInt() else lighten(primary(ctx), 0.82f)

    fun statsText(ctx: Context): Int =
        if (isDarkMode(ctx)) 0xFF9E9E9E.toInt() else lighten(primary(ctx), 0.5f)

    fun buttonTint(ctx: Context): Int =
        if (isDarkMode(ctx)) 0xFFE0E0E0.toInt() else darken(primary(ctx), 0.3f)

    fun iconTint(ctx: Context): Int = 0xFFFFFFFF.toInt()

    fun progressTint(ctx: Context): Int = secondary(ctx)

    fun tabIndicator(ctx: Context): Int = secondary(ctx)

    fun tabText(ctx: Context): Int = 0xFF757575.toInt()

    fun tabTextSelected(ctx: Context): Int = 0xFF000000.toInt()

    fun applyActivityTheme(activity: AppCompatActivity) {
        if (isDarkMode(activity)) {
            activity.setTheme(R.style.AppTheme_Dark)
            return
        }
        val palette = currentPalette(activity).coerceIn(0, THEME_NAMES.size - 1)
        val resId = try {
            R.style::class.java.getField(THEME_NAMES[palette]).getInt(null)
        } catch (e: Exception) {
            R.style.AppTheme
        }
        activity.setTheme(resId)
    }

    fun setPalette(ctx: Context, index: Int) {
        val safe = ((index % PALETTES.size) + PALETTES.size) % PALETTES.size
        prefs(ctx).edit().putInt(KEY_THEME_PALETTE, safe).apply()
    }

    fun setFollowSystem(ctx: Context, follow: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_FOLLOW_SYSTEM_THEME, follow).apply()
        if (follow) {
            prefs(ctx).edit().putInt(KEY_THEME_PALETTE, 0).apply()
        }
    }
}
