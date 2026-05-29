package bin.mg.main.utils.file;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Environment;
import android.util.LruCache;
import bin.mg.main.model.FileItem;
import bin.mg.main.model.FileType;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileSystemHelper {
    
    private static FileSystemHelper instance;
    private final ExecutorService executor;
    private final LruCache<String, List<FileItem>> cache;
    private final Context context;
    
    private FileSystemHelper(Context context) {
        this.context = context.getApplicationContext();
        this.executor = Executors.newFixedThreadPool(4);
        
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 8;
        this.cache = new LruCache<String, List<FileItem>>(cacheSize) {
            @Override
            protected int sizeOf(String key, List<FileItem> items) {
                return items.size();
            }
        };
    }
    
    public static synchronized FileSystemHelper getInstance(Context context) {
        if (instance == null) {
            instance = new FileSystemHelper(context);
        }
        return instance;
    }
    
    public void execute(Runnable runnable) {
        executor.execute(runnable);
    }
    
    public List<FileItem> listDirectory(String path) {
        List<FileItem> cached = cache.get(path);
        if (cached != null) {
            return new ArrayList<>(cached);
        }
        
        List<FileItem> items = new ArrayList<>();
        File directory = new File(path);
        
        if (!directory.exists() || !directory.isDirectory()) {
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
        
        cache.put(path, new ArrayList<>(items));
        return items;
    }
    
    public void invalidateCache(String path) {
        cache.remove(path);
        File parent = new File(path).getParentFile();
        if (parent != null) {
            cache.remove(parent.getAbsolutePath());
        }
    }
    
    public void invalidateAllCache() {
        cache.evictAll();
    }
    
    public boolean canNavigateTo(String path) {
        File file = new File(path);
        return file.exists() && file.isDirectory();
    }
    
    public boolean canRead(String path) {
        File file = new File(path);
        return file.canRead();
    }
    
    public String getParentPath(String path) {
        File file = new File(path);
        File parent = file.getParentFile();
        return parent != null ? parent.getAbsolutePath() : null;
    }
    
    public boolean shouldShowParent(String path) {
        return path != null && !path.equals("/");
    }
    
    public Bitmap getApkIcon(Context context, String apkPath) {
        try {
            PackageManager pm = context.getPackageManager();
            int flags = PackageManager.GET_META_DATA;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                flags |= PackageManager.GET_SIGNING_CERTIFICATES;
            }
            android.content.pm.PackageInfo pkgInfo = pm.getPackageArchiveInfo(apkPath, flags);
            if (pkgInfo != null && pkgInfo.applicationInfo != null) {
                pkgInfo.applicationInfo.sourceDir = apkPath;
                pkgInfo.applicationInfo.publicSourceDir = apkPath;
                Drawable icon = pkgInfo.applicationInfo.loadIcon(pm);
                if (icon != null) {
                    return drawableToBitmap(icon);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            if (bitmap != null) return bitmap;
        }
        
        int intrinsicWidth = drawable.getIntrinsicWidth();
        int intrinsicHeight = drawable.getIntrinsicHeight();
        if (intrinsicWidth <= 0) intrinsicWidth = 96;
        if (intrinsicHeight <= 0) intrinsicHeight = 96;
        
        Bitmap bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
    
    public void shutdown() {
        executor.shutdown();
    }
}
