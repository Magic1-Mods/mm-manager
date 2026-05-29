package bin.mg.main;

import android.os.Environment;
import bin.mg.main.model.FileItem;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FileUtils {
    
    public static final String ROOT_PATH = "/";
    public static final String STORAGE_PATH = "/storage";
    public static final String EMULATED_PATH = "/storage/emulated";
    
    public static List<FileItem> listFiles(String directoryPath) {
        List<FileItem> items = new ArrayList<>();
        File directory = new File(directoryPath);
        
        if (!directory.exists()) {
            return items;
        }
        
        if (!directory.isDirectory()) {
            return items;
        }
        
        File[] files = directory.listFiles();
        if (files == null) {
            return items;
        }
        
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                if (f1.isDirectory() && !f2.isDirectory()) return -1;
                if (!f1.isDirectory() && f2.isDirectory()) return 1;
                return f1.getName().compareToIgnoreCase(f2.getName());
            }
        });
        
        for (File file : files) {
            items.add(FileItem.fromFile(file));
        }
        
        return items;
    }
    
    public static List<FileItem> listRootDirectories() {
        List<FileItem> items = new ArrayList<>();
        
        File root = new File(ROOT_PATH);
        File[] roots = root.listFiles();
        
        if (roots != null) {
            Arrays.sort(roots, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
            for (File file : roots) {
                items.add(FileItem.fromFile(file));
            }
        }
        
        return items;
    }
    
    public static List<FileItem> listStorageDirectories() {
        List<FileItem> items = new ArrayList<>();
        
        File storage = new File(STORAGE_PATH);
        File[] storages = storage.listFiles();
        
        if (storages != null) {
            Arrays.sort(storages, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
            for (File file : storages) {
                items.add(FileItem.fromFile(file));
            }
        }
        
        File emulated = new File(EMULATED_PATH);
        File[] emulateds = emulated.listFiles();
        
        if (emulateds != null) {
            for (File file : emulateds) {
                items.add(FileItem.fromFile(file));
            }
        }
        
        return items;
    }
    
    public static boolean hasParent(String path) {
        if (path == null || path.isEmpty()) return false;
        if (path.equals(ROOT_PATH)) return false;
        return true;
    }
    
    public static String getParentPath(String path) {
        if (path == null || path.isEmpty()) return null;
        File file = new File(path);
        File parent = file.getParentFile();
        return parent != null ? parent.getAbsolutePath() : null;
    }
    
    public static boolean copy(File src, File dest) {
        if (src.isDirectory()) {
            return copyDirectory(src, dest);
        } else {
            return copyFile(src, dest);
        }
    }
    
    private static boolean copyFile(File src, File dest) {
        try {
            FileInputStream in = new FileInputStream(src);
            FileOutputStream out = new FileOutputStream(dest);
            byte[] buffer = new byte[8192];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            in.close();
            out.close();
            dest.setLastModified(src.lastModified());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    private static boolean copyDirectory(File src, File dest) {
        if (!dest.exists()) {
            dest.mkdirs();
        }
        File[] children = src.listFiles();
        if (children != null) {
            for (File child : children) {
                File newDest = new File(dest, child.getName());
                if (!copy(child, newDest)) {
                    return false;
                }
            }
        }
        return true;
    }
    
    public static boolean move(File src, File dest) {
        if (src.renameTo(dest)) {
            return true;
        } else {
            if (copy(src, dest)) {
                return delete(src);
            }
            return false;
        }
    }
    
    public static boolean delete(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (!delete(child)) {
                        return false;
                    }
                }
            }
        }
        return file.delete();
    }
    
    public static boolean rename(File oldFile, String newName) {
        File parent = oldFile.getParentFile();
        File newFile = new File(parent, newName);
        return oldFile.renameTo(newFile);
    }
    
    public static boolean createFolder(String parentPath, String folderName) {
        File parent = new File(parentPath);
        File newFolder = new File(parent, folderName);
        return newFolder.mkdirs();
    }
    
    public static String getMimeType(String filePath) {
        if (filePath == null) return "*/*";
        
        String extension = "";
        int i = filePath.lastIndexOf('.');
        if (i > 0) {
            extension = filePath.substring(i + 1).toLowerCase();
        }
        
        switch (extension) {
            case "txt":
            case "log":
            case "md":
            case "json":
            case "xml":
                return "text/plain";
            case "pdf":
                return "application/pdf";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "apk":
                return "application/vnd.android.package-archive";
            case "mp4":
            case "mkv":
            case "avi":
                return "video/*";
            case "mp3":
            case "wav":
            case "flac":
                return "audio/*";
            case "zip":
            case "rar":
            case "7z":
                return "application/zip";
            default:
                return "*/*";
        }
    }
    
    public static boolean isTextFile(String filePath) {
        if (filePath == null) return false;
        String lower = filePath.toLowerCase();
        return lower.endsWith(".txt") || lower.endsWith(".log") || 
               lower.endsWith(".md") || lower.endsWith(".json") || 
               lower.endsWith(".xml") || lower.endsWith(".properties") ||
               lower.endsWith(".smali") || lower.endsWith(".java") ||
               lower.endsWith(".kt") || lower.endsWith(".gradle") ||
               lower.endsWith(".html") || lower.endsWith(".css") ||
               lower.endsWith(".js") || lower.endsWith(".py");
    }
    
    public static boolean isXmlFile(String filePath) {
        if (filePath == null) return false;
        return filePath.toLowerCase().endsWith(".xml");
    }
    
    public static boolean isApkFile(String filePath) {
        if (filePath == null) return false;
        String lower = filePath.toLowerCase();
        return lower.endsWith(".apk") || lower.endsWith(".xapk") || lower.endsWith(".apks");
    }
}
