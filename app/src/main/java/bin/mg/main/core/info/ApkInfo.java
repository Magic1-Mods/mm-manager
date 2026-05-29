package bin.mg.main.core.info;

import android.graphics.drawable.Drawable;
import bin.mg.main.model.FileItem;

public class ApkInfo {
    
    String packageName;
    String versionName;
     long versionCode;
    long fileSize;
    boolean isInstalled;
    boolean isSystemApp;
    boolean signatureV1;
    boolean signatureV2;
    boolean signatureV3;
    int targetSdk;
    int minSdk;
    String installedVersion;
    String dataDir;
    String sourceDir;
    long firstInstallTime;
    long lastUpdateTime;
    int uid;
    Drawable appIcon;
    String appName;
    
    public void getPackageName(FileItem item) {
        
    }
}
