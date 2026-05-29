package bin.mg.main.model;

import java.util.*;

public enum FileType {
    FOLDER,
    APK,
    XAPK,
    APKS,
    IMAGE,
    ARCHIVE,
    JAR,
    TEXT,
    SCRIPT,
    JAVA,
    KOTLIN,
    PYTHON,
    HTML,
    PHP,
    DEX,
    ARSC,
    CLASS,
    LUA,
    KEYSTORE,
    UNKNOWN;

    private static final Map<String, FileType> EXT_MAP = new HashMap<>();

    static {
        map(APK, "apk");
        map(XAPK, "xapk");
        map(APKS, "apks");
        map(IMAGE, "png", "jpg", "jpeg", "gif", "bmp", "webp", "svg");
        map(JAR, "jar");
        addCompound("tar.gz", ARCHIVE);
        addCompound("tar.xz", ARCHIVE);
        addCompound("tar.zst", ARCHIVE);
        addCompound("tar.bz2", ARCHIVE);
        addCompound("tar.lz4", ARCHIVE);
        addCompound("tar.lzo", ARCHIVE);
        map(ARCHIVE, "zip", "tar", "7z", "rar", "gz", "xz", "bz2", "lz4", "zst", "zstd", "lzop", "bzip2", "gzip", "lzma");
        map(SCRIPT, "c", "cpp", "xml", "h", "hpp", "cxx", "cs", "a", "s", "ts", "js", "sh", "gradle", "gradle.kts");
        map(TEXT, "txt", "md", "properties", "css", "SF", "MF", "log", "json");
        map(JAVA, "java");
        map(KOTLIN, "kt");
        map(PYTHON, "py");
        map(HTML, "html");
        map(DEX, "dex");
        map(ARSC, "arsc");
        map(CLASS, "class");
        map(LUA, "lua");
        map(KEYSTORE, "keystore");
    }

    private static void map(FileType type, String... extensions) {
        for (String ext : extensions) {
            EXT_MAP.put(ext, type);
        }
    }

    private static void addCompound(String compound, FileType type) {
        EXT_MAP.put(compound, type);
    }

    public static FileType fromExtension(String extension) {
        if (extension == null) return UNKNOWN;
        extension = extension.toLowerCase().trim();
        return EXT_MAP.getOrDefault(extension, UNKNOWN);
    }

    public static FileType fromFileName(String fileName) {
        if (fileName == null) return UNKNOWN;
        if (fileName.equals("..")) return FOLDER;

        int lastDot = fileName.lastIndexOf('.');
        if (lastDot < 0 || lastDot >= fileName.length() - 1) {
            return UNKNOWN;
        }

        String[] parts = fileName.split("\\.");
        if (parts.length == 0) return UNKNOWN;

        for (int i = parts.length - 1; i >= 1; i--) {
            StringBuilder extBuilder = new StringBuilder(parts[i]);
            for (int j = i + 1; j < parts.length; j++) {
                extBuilder.append(".").append(parts[j]);
            }
            String ext = extBuilder.toString().toLowerCase(Locale.ROOT);
            FileType type = EXT_MAP.get(ext);
            if (type != null) {
                return type;
            }
        }

        return UNKNOWN;
    }
}