package bin.mg.main

object MathUtils {
    @JvmStatic
    fun constrains(min: Float, max: Float, value: Float): Float {
        return value.coerceIn(min, max)
    }
}
