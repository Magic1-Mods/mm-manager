package bin.mg.main;

import android.app.Application;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AppMain extends Application {
    private static final String TAG = "AppMain";
    private static AppMain instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                handleCrash(t, e);
            }
        });
    }

    public static AppMain getInstance() {
        return instance;
    }

    private void handleCrash(Thread thread, Throwable throwable) {
        Log.e(TAG, "CRASH: " + throwable.getMessage(), throwable);

        StringBuilder crashLog = new StringBuilder();
        crashLog.append("=== CRASH LOG ===\n");
        crashLog.append("Time: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n");
        crashLog.append("App: Magic Manager\n");
        crashLog.append("Version: ").append(BuildConfig.VERSION_NAME).append("\n");
        crashLog.append("Android: ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
        crashLog.append("Device: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\n\n");
        crashLog.append("Thread: ").append(thread.getName()).append("\n");
        crashLog.append("Exception: ").append(throwable.getClass().getName()).append("\n");
        crashLog.append("Message: ").append(throwable.getMessage()).append("\n\n");
        
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        crashLog.append("=== STACK TRACE ===\n");
        crashLog.append(sw.toString());
        
        crashLog.append("\n=== CAUSE ===\n");
        Throwable cause = throwable.getCause();
        while (cause != null) {
            crashLog.append("Cause: ").append(cause.getClass().getName()).append("\n");
            crashLog.append("Message: ").append(cause.getMessage()).append("\n\n");
            cause = cause.getCause();
        }

        try {
            File logDir = new File(getExternalFilesDir(null), "crash_logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            String fileName = "crash_" + System.currentTimeMillis() + ".txt";
            File logFile = new File(logDir, fileName);
            FileWriter writer = new FileWriter(logFile);
            writer.write(crashLog.toString());
            writer.close();
            Log.d(TAG, "Crash log saved: " + logFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Failed to save crash log", e);
        }

        Intent intent = new Intent(this, CrashHandler.class);
        intent.putExtra("crash_log", crashLog.toString());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(1);
    }
}