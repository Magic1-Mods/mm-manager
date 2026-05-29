package bin.mg.main;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import java.io.File;

public class CrashHandler extends AppCompatActivity {
    private String crashLog;
    private TextView textLog;
    private Button btnCopy, btnShare, btnRestart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.crash_handler);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        crashLog = getIntent().getStringExtra("crash_log");
        if (crashLog == null) {
            crashLog = "No crash log available";
        }

        textLog = findViewById(R.id.crash_log);
        textLog.setText(crashLog);

        btnCopy = findViewById(R.id.btn_copy);
        btnShare = findViewById(R.id.btn_share);
        btnRestart = findViewById(R.id.btn_restart);

        btnCopy.setOnClickListener(v -> copyToClipboard());
        btnShare.setOnClickListener(v -> shareCrashLog());
        btnRestart.setOnClickListener(v -> restartApp());
    }

    private void copyToClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("crash_log", crashLog);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Crash log copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    private void shareCrashLog() {
        try {
            File logDir = new File(getExternalFilesDir(null), "crash_logs");
            File[] files = logDir.listFiles();
            if (files != null && files.length > 0) {
                File latestLog = files[files.length - 1];
                Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", latestLog);
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Crash Log");
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(shareIntent, "Share crash log"));
            } else {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, crashLog);
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Crash Log");
                startActivity(Intent.createChooser(shareIntent, "Share crash log"));
            }
        } catch (Exception e) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, crashLog);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Crash Log");
            startActivity(Intent.createChooser(shareIntent, "Share crash log"));
        }
    }

    private void restartApp() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}