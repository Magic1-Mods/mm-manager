package bin.mg.main.ui.editor;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import bin.mg.main.R;

public class FileEditorActivity extends AppCompatActivity {

    public static void start(Activity context, String filePath) {
        android.content.Intent intent = new android.content.Intent(context, FileEditorActivity.class);
        intent.putExtra("file_path", filePath);
        context.startActivity(intent);
    }

    private DrawerLayout drawerLayout;
    private LinedEditText editText;
    private TextView filenameText;
    private TextView lineNoEncodingText;
    private LinearLayout symbolInput;
    
    // Drawer Views
    private TextView drawerCurrentFilename;
    private TextView drawerCurrentPath;

    private String currentFilePath = null;
    private boolean isModified = false;
    private boolean justSaved = false;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            updateLineNumber();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_editor);

        initViews();
        setupSymbolBar();
        setupClickListeners();

        currentFilePath = getIntent().getStringExtra("file_path");

        if (currentFilePath != null && new File(currentFilePath).exists()) {
            loadFile();
        } else {
            // Demo view for testing without an intent
            currentFilePath = "/storage/emulated/0/convert.py";
            updateInfoBar();
        }
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        editText = findViewById(R.id.smali_editor);
        filenameText = findViewById(R.id.textview_filename);
        lineNoEncodingText = findViewById(R.id.textview_lineno_encoding);
        symbolInput = findViewById(R.id.symbol_input);
        
        drawerCurrentFilename = findViewById(R.id.drawer_current_filename);
        drawerCurrentPath = findViewById(R.id.drawer_current_path);

        if (editText != null) {
            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    if (!justSaved) {
                        setModified(true);
                    }
                    justSaved = false;
                    mHandler.sendEmptyMessage(0);
                }
            });
        }
    }

    private void setupClickListeners() {
        findViewById(R.id.btn_menu).setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        findViewById(R.id.btn_minimize).setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));
        
        findViewById(R.id.btn_save).setOnClickListener(v -> saveFile());
        
        // Undo / Redo logic stubs
        findViewById(R.id.btn_undo).setOnClickListener(v -> Toast.makeText(this, "Undo clicked", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btn_redo).setOnClickListener(v -> Toast.makeText(this, "Redo clicked", Toast.LENGTH_SHORT).show());
    }

    private void setupSymbolBar() {
        // Exact symbols from the bottom bar screenshot
        String[] symbols = {"→", "/", "+", "-", "*", "=", "<", ">", "(", ")", "[", "]", "{", "}", ":", "\"", "'", ";", "_"};

        for (String symbol : symbols) {
            TextView tv = new TextView(this);
            // Translate the display arrow back to standard tab/indent logic if needed
            String insertText = symbol.equals("→") ? "\t" : symbol;
            
            tv.setText(symbol);
            tv.setTextSize(18);
            tv.setTextColor(0xFF212121);
            tv.setPadding(32, 16, 32, 16);
            tv.setOnClickListener(v -> {
                int start = Math.max(editText.getSelectionStart(), 0);
                int end = Math.max(editText.getSelectionEnd(), 0);
                editText.getText().replace(Math.min(start, end), Math.max(start, end), insertText);
            });
            symbolInput.addView(tv);
        }
    }

    private void updateInfoBar() {
        if (currentFilePath == null) return;
        File file = new File(currentFilePath);
        
        String prefix = isModified ? "* " : "";
        filenameText.setText(prefix + file.getName());
        
        drawerCurrentFilename.setText(file.getName());
        drawerCurrentPath.setText(currentFilePath);
    }

    private void updateLineNumber() {
        if (editText == null || lineNoEncodingText == null) return;

        int line = 1;
        int col = 1;
        int pos = editText.getSelectionStart();
        String text = editText.getText().toString();
        
        if (pos > 0 && pos <= text.length()) {
            String subText = text.substring(0, pos);
            String[] lines = subText.split("\n", -1);
            line = lines.length;
            col = lines[lines.length - 1].length() + 1;
        }
        
        lineNoEncodingText.setText(line + ":" + col + "   UTF-8");
    }

    private void setModified(boolean modified) {
        isModified = modified;
        updateInfoBar();
    }

    private void loadFile() {
        if (currentFilePath == null) return;
        new LoadFileTask().execute(currentFilePath);
    }

    private void saveFile() {
        if (currentFilePath == null) return;
        new SaveFileTask().execute(currentFilePath);
    }

    class LoadFileTask extends AsyncTask<String, Void, String> {
        ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            dialog = ProgressDialog.show(FileEditorActivity.this, "Loading", "Reading file...", true);
        }

        @Override
        protected String doInBackground(String... params) {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new FileReader(params[0]))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                return sb.toString();
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (dialog != null && dialog.isShowing()) dialog.dismiss();
            
            if (result != null) {
                editText.setText(result);
                setModified(false);
                updateLineNumber();
            } else {
                Toast.makeText(FileEditorActivity.this, "Failed to load file", Toast.LENGTH_SHORT).show();
            }
        }
    }

    class SaveFileTask extends AsyncTask<String, Void, Boolean> {
        ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            dialog = ProgressDialog.show(FileEditorActivity.this, "Saving", "Writing file...", true);
        }

        @Override
        protected Boolean doInBackground(String... params) {
            try {
                String content = editText.getText().toString();
                Files.write(Paths.get(params[0]), content.getBytes(StandardCharsets.UTF_8));
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (dialog != null && dialog.isShowing()) dialog.dismiss();

            if (result) {
                justSaved = true;
                setModified(false);
                Toast.makeText(FileEditorActivity.this, "Saved", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(FileEditorActivity.this, "Save failed", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
