package bin.mg.main;

public class MathUtils {
    public static float constrains(float min, float max, float value) {
        return Math.max(min, Math.min(max, value));
    }
}