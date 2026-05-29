package bin.mg.main.utils.icon;

import androidx.core.content.ContextCompat;
import bin.mg.main.R;
import bin.mg.main.model.FileType;
import java.util.HashMap;
import java.util.Map;

public class IconManager {

    private static final Map<FileType, Integer> ICON_MAP = new HashMap<>();
    private static final Map<FileType, Integer> TINT_MAP = new HashMap<>();

    static {
        // Map icons
        mapIcon(FileType.FOLDER, R.drawable.ic_folder);
        mapIcon(FileType.APK, R.drawable.ic_android);
        mapIcon(FileType.XAPK, R.drawable.ic_android);
        mapIcon(FileType.APKS, R.drawable.ic_android);
        mapIcon(FileType.IMAGE, R.drawable.ic_image);
        mapIcon(FileType.ARCHIVE, R.drawable.ic_archived);
        mapIcon(FileType.JAR, R.drawable.ic_java);
        mapIcon(FileType.LUA, R.drawable.ic_lua);
        mapIcon(FileType.TEXT, R.drawable.ic_code);
        mapIcon(FileType.SCRIPT, R.drawable.ic_script);
        mapIcon(FileType.JAVA, R.drawable.ic_java);
        mapIcon(FileType.KOTLIN, R.drawable.ic_kotlin);
        mapIcon(FileType.PYTHON, R.drawable.ic_python);
        mapIcon(FileType.HTML, R.drawable.ic_html);
        mapIcon(FileType.PHP, R.drawable.ic_php);
        mapIcon(FileType.DEX, R.drawable.ic_dex);
        mapIcon(FileType.ARSC, R.drawable.ic_arsc);
        mapIcon(FileType.CLASS, R.drawable.ic_java);
        mapIcon(FileType.UNKNOWN, R.drawable.ic_file);
        mapIcon(FileType.KEYSTORE, R.drawable.ic_keystore);
        
        mapTint(FileType.FOLDER, R.color.black);
        mapTint(FileType.APK, R.color.brown_300);
        mapTint(FileType.XAPK, R.color.orange_400);
        mapTint(FileType.APKS, R.color.orange_400);
        mapTint(FileType.IMAGE, R.color.orange_100);
        mapTint(FileType.ARCHIVE, R.color.brown_400);
        mapTint(FileType.JAR, R.color.orange_400);
        mapTint(FileType.TEXT, R.color.blue_800);
        mapTint(FileType.SCRIPT, R.color.blue_800);
        mapTint(FileType.JAVA, R.color.blue_800);
        mapTint(FileType.KOTLIN, R.color.blue_800);
        mapTint(FileType.PYTHON, R.color.blue_800);
        mapTint(FileType.HTML, R.color.cyan_200);
        mapTint(FileType.PHP, R.color.blue_800);
        mapTint(FileType.DEX, R.color.green_500);
        mapTint(FileType.ARSC, R.color.orange_300);
        mapTint(FileType.CLASS, R.color.orange_100);
        mapTint(FileType.UNKNOWN, R.color.grey_600);
        mapTint(FileType.KEYSTORE, R.color.grey_600);
    }

    private static void mapIcon(FileType type, int iconRes) {
        ICON_MAP.put(type, iconRes);
    }

    private static void mapTint(FileType type, int tintRes) {
        TINT_MAP.put(type, tintRes);
    }

    public static int getIconResource(FileType fileType) {
        if (fileType == null) return R.drawable.ic_file;
        return ICON_MAP.getOrDefault(fileType, R.drawable.ic_file);
    }

    public static int getBackgroundResource() {
        return R.drawable.bg_file;
    }

    public static int getIconTint(FileType fileType) {
        if (fileType == null) return R.color.grey_300;
        return TINT_MAP.getOrDefault(fileType, R.color.grey_300);
    }
}