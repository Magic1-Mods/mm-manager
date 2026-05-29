package bin.mg.main;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.provider.Settings;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import bin.mg.main.utils.file.FileSystemHelper;
import bin.mg.main.model.FileItem;
import bin.mg.main.ui.adapter.FileAdapter;
import bin.mg.main.BuildConfig;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class MainActivity extends AppCompatActivity {
    
    private static final int REQUEST_CODE_PERMISSION = 100;
    private static final int REQUEST_CODE_SAF = 101;
    private static final String DEFAULT_PATH = "/storage/emulated/0/";
    private static final String PREFS_NAME = "MagicManagerPrefs";
    private static final String KEY_PATH_LEFT = "pathLeft";
    private static final String KEY_PATH_RIGHT = "pathRight";
    private static final String KEY_SCROLL_LEFT = "scrollLeft";
    private static final String KEY_SCROLL_RIGHT = "scrollRight";
    private static final String KEY_THEME_PALETTE = "themePalette";
    private static final String KEY_FOLLOW_SYSTEM_THEME = "followSystemTheme";
    private static final String KEY_CUSTOM_PATHS = "customPaths";
    
    private TextView toolbarPath, topStats, diskInfo;
    private RecyclerView recyclerLeft, recyclerRight;
    private View panelLeft, panelRight;
    private ImageButton btnBack, btnForward, btnNew, btnSwap, btnParent;
    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private ActionBarDrawerToggle drawerToggle;
    
    private LinearLayout sectionLocalContent, sectionHomeContent, sectionToolsContent;
    private ImageView sectionLocalArrow, sectionHomeArrow, sectionToolsArrow;
    private ProgressBar rootProgress, storageProgress;
    private TextView rootStorageText, storageText;
    private LinearLayout dynamicStorageContainer;
    private ImageButton btnThemeToggle, btnDrawerMenu;
    
    private FileAdapter adapterLeft, adapterRight;
    private String currentPathLeft = DEFAULT_PATH;
    private String currentPathRight = DEFAULT_PATH;
    private int activePanel = 0;
    
    private final Stack<String> backStackLeft = new Stack<>();
    private final Stack<String> forwardStackLeft = new Stack<>();
    private final Stack<String> backStackRight = new Stack<>();
    private final Stack<String> forwardStackRight = new Stack<>();
    
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicLong lastClickTime = new AtomicLong(0);
    private static final long DEBOUNCE_DELAY = 200;
    
    private volatile String loadingPathLeft = null;
    private volatile String loadingPathRight = null;
    
    private int currentPalette = 0;
    private boolean followSystemTheme = false;
    private ArrayList<String> customPaths = new ArrayList<>();
    
    private static final int[][] PALETTES = {
        {0xFF212121, 0xFF424242},
        {0xFF1565C0, 0xFF1E88E5},
        {0xFF6A1B9A, 0xFF8E24AA},
        {0xFF2E7D32, 0xFF43A047},
        {0xFFE65100, 0xFFF57C00},
        {0xFFC2185B, 0xFFD81B60},
        {0xFF00838F, 0xFF00ACC1},
        {0xFF4527A0, 0xFF5E35B1},
        {0xFFB71C1C, 0xFFE53935},
        {0xFF1A237E, 0xFF3949AB},
        {0xFF00695C, 0xFF00897B},
        {0xFF0277BD, 0xFF00838F},
        {0xFF4A148C, 0xFF6A1B9A},
        {0xFFBF360C, 0xFFE64A19},
        {0xFF1B5E20, 0xFF2E7D32},
    };
    
    private int getPrimaryColor() {
        return PALETTES[currentPalette][0];
    }
    
    private int getSecondaryColor() {
        return PALETTES[currentPalette][1];
    }
    
    private int lightenColor(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        r = Math.min(255, (int)(r + (255 - r) * factor));
        g = Math.min(255, (int)(g + (255 - g) * factor));
        b = Math.min(255, (int)(b + (255 - b) * factor));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    
    private int darkenColor(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        r = Math.max(0, (int)(r * (1 - factor)));
        g = Math.max(0, (int)(g * (1 - factor)));
        b = Math.max(0, (int)(b * (1 - factor)));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    
    private int getThemeColor(String attrName, int defaultColor) {
        int primary = getPrimaryColor();
        int secondary = getSecondaryColor();
        
        switch (attrName) {
            case "drawerBackground": return lightenColor(primary, 0.92f);
            case "headerBackground": return primary;
            case "iconTint": return 0xFFFFFFFF;
            case "iconTintSecondary": return lightenColor(primary, 0.6f);
            case "textPrimary": return darkenColor(primary, 0.5f);
            case "textSecondary": return lightenColor(primary, 0.35f);
            case "sectionText": return secondary;
            case "dividerColor": return lightenColor(primary, 0.82f);
            case "progressTint": return secondary;
            case "mainBackground": return lightenColor(primary, 0.92f);
            case "toolbarBackground": return primary;
            case "panelBg": return 0xFFFFFFFF;
            case "bottomBarBackground": return 0xFFFFFFFF;
            case "statsTextColor": return lightenColor(primary, 0.5f);
            case "buttonTint": return darkenColor(primary, 0.3f);
            default: return defaultColor;
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        loadThemePalette();
        applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initToolbar();
        initViews();
        initDrawerSections();
        registerFileHandlers();
        setupAdapters();
        setupListeners();
        loadCustomPaths();
        loadState();
        applyThemeColors();
        
        if (!hasPermissions()) {
            requestPermissions();
        } else {
            loadBothPanels();
            updateStorageInfo();
        }
        
        setActivePanel(0);
    }
    
    private void loadThemePalette() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        currentPalette = prefs.getInt(KEY_THEME_PALETTE, 0);
        followSystemTheme = prefs.getBoolean(KEY_FOLLOW_SYSTEM_THEME, false);
    }
    
    private void applyTheme() {
        if (followSystemTheme) {
            int currentNightMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            if (currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                setTheme(R.style.AppTheme_Dark);
            } else {
                String[] themes = {
                    "AppTheme",
                    "AppTheme.Palette1",
                    "AppTheme.Palette2",
                    "AppTheme.Palette3",
                    "AppTheme.Palette4",
                    "AppTheme.Palette5",
                    "AppTheme.Palette6",
                    "AppTheme.Palette7",
                    "AppTheme.Palette8",
                    "AppTheme.Palette9",
                    "AppTheme.Palette10"
                };
                try {
                    int themeRes = R.style.class.getField(themes[currentPalette]).getInt(null);
                    setTheme(themeRes);
                } catch (Exception e) {
                    setTheme(R.style.AppTheme);
                }
            }
        } else {
            String[] themes = {
                "AppTheme",
                "AppTheme.Palette1",
                "AppTheme.Palette2",
                "AppTheme.Palette3",
                "AppTheme.Palette4",
                "AppTheme.Palette5",
                "AppTheme.Palette6",
                "AppTheme.Palette7",
                "AppTheme.Palette8",
                "AppTheme.Palette9",
                "AppTheme.Palette10"
            };
            
            try {
                int themeRes = R.style.class.getField(themes[currentPalette]).getInt(null);
                setTheme(themeRes);
            } catch (Exception e) {
                setTheme(R.style.AppTheme);
            }
        }
    }
    
    private void loadCustomPaths() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String pathsJson = prefs.getString(KEY_CUSTOM_PATHS, "");
        if (!pathsJson.isEmpty()) {
            String[] paths = pathsJson.split("|||");
            for (String path : paths) {
                if (!path.isEmpty()) {
                    customPaths.add(path);
                }
            }
        }
    }
    
    private void saveCustomPaths() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < customPaths.size(); i++) {
            if (i > 0) sb.append("|||");
            sb.append(customPaths.get(i));
        }
        prefs.edit().putString(KEY_CUSTOM_PATHS, sb.toString()).apply();
    }
    
    private void toggleTheme() {
        currentPalette = (currentPalette + 1) % PALETTES.length;
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putInt(KEY_THEME_PALETTE, currentPalette).apply();
        applyThemeColors();
    }
    
    private void applyThemeColors() {
        boolean isDarkMode = followSystemTheme && 
            (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) 
            == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        
        int primaryColor, mainBg, panelBg, bottomBarBg, textPrimary, textSecondary, statsText, buttonTint, dividerColor;
        
        if (isDarkMode) {
            primaryColor = 0xFF212121;
            mainBg = 0xFF212121;
            panelBg = 0xFF303030;
            bottomBarBg = 0xFF303030;
            textPrimary = 0xFFFFFFFF;
            textSecondary = 0xFFB0B0B0;
            statsText = 0xFF9E9E9E;
            buttonTint = 0xFFE0E0E0;
            dividerColor = 0xFF424242;
        } else {
            primaryColor = getPrimaryColor();
            mainBg = 0xFFF5F5F5;
            panelBg = 0xFFFFFFFF;
            bottomBarBg = 0xFFFFFFFF;
            textPrimary = 0xFF212121;
            textSecondary = 0xFF757575;
            statsText = 0xFF9E9E9E;
            buttonTint = 0xFF424242;
            dividerColor = 0xFFE0E0E0;
        }
        
        Window window = getWindow();
        window.setStatusBarColor(primaryColor);
        
        View toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setBackgroundColor(primaryColor);
        }
        
        View statsContainer = findViewById(R.id.stats_container);
        if (statsContainer != null) {
            statsContainer.setBackgroundColor(primaryColor);
        }
        
        LinearLayout mainContainer = findViewById(R.id.main_container);
        if (mainContainer != null) {
            mainContainer.setBackgroundColor(mainBg);
        }
        
        FrameLayout panelLeft = findViewById(R.id.panel_left);
        FrameLayout panelRight = findViewById(R.id.panel_right);
        if (panelLeft != null) panelLeft.setBackgroundColor(panelBg);
        if (panelRight != null) panelRight.setBackgroundColor(panelBg);
        
        View divider = findViewById(R.id.divider);
        if (divider != null) divider.setBackgroundColor(dividerColor);
        
        LinearLayout bottomBar = findViewById(R.id.bottom_bar);
        if (bottomBar != null) {
            bottomBar.setBackgroundColor(bottomBarBg);
        }
        
        TextView toolbarPath = findViewById(R.id.toolbar_path);
        if (toolbarPath != null) toolbarPath.setTextColor(0xFFFFFFFF);
        
        TextView statsLeft = findViewById(R.id.stats_left);
        TextView diskInfo = findViewById(R.id.disk_info);
        if (statsLeft != null) statsLeft.setTextColor(statsText);
        if (diskInfo != null) diskInfo.setTextColor(statsText);
        
        ImageButton btnBack = findViewById(R.id.btn_back);
        ImageButton btnForward = findViewById(R.id.btn_forward);
        ImageButton btnNew = findViewById(R.id.btn_new);
        ImageButton btnSwap = findViewById(R.id.btn_swap);
        ImageButton btnParent = findViewById(R.id.btn_parent);
        if (btnBack != null) btnBack.setColorFilter(buttonTint);
        if (btnForward != null) btnForward.setColorFilter(buttonTint);
        if (btnNew != null) btnNew.setColorFilter(buttonTint);
        if (btnSwap != null) btnSwap.setColorFilter(buttonTint);
        if (btnParent != null) btnParent.setColorFilter(buttonTint);
        if (adapterLeft != null) adapterLeft.setThemeColor(primaryColor, isDarkMode);
        if (adapterRight != null) adapterRight.setThemeColor(primaryColor, isDarkMode);
        updateDrawerColors(primaryColor, isDarkMode);
    }
    
    private void updateDrawerColors(int primaryColor, boolean isDarkMode) {
        View drawerView = findViewById(R.id.nav_drawer);
        if (drawerView == null) return;
        
        int textPrimary, textSecondary;
        if (isDarkMode) {
            textPrimary = 0xFFFFFFFF;
            textSecondary = 0xFFB0B0B0;
        } else {
            textPrimary = 0xFF212121;
            textSecondary = 0xFF757575;
        }
        
        View headerBg = findViewById(R.id.header_background);
        if (headerBg != null) {
            headerBg.setBackgroundColor(primaryColor);
        }
        
        TextView appName = findViewById(R.id.header_app_name);
        if (appName != null) {
            appName.setTextColor(0xFFFFFFFF);
            String appNameStr = getString(R.string.app_name);
            String versionStr = BuildConfig.VERSION_NAME;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                appName.setText(Html.fromHtml(appNameStr + " <font color='#99FFFFFF'><small>v" + versionStr + "</small></font>", Html.FROM_HTML_MODE_COMPACT));
            } else {
                appName.setText(Html.fromHtml(appNameStr + " <font color='#99FFFFFF'><small>v" + versionStr + "</small></font>"));
            }
        }
        
        ImageButton themeToggle = findViewById(R.id.btn_theme_toggle);
        ImageButton drawerMenu = findViewById(R.id.btn_drawer_menu);
        int iconTint = 0xFFFFFFFF;
        if (themeToggle != null) themeToggle.setColorFilter(iconTint);
        if (drawerMenu != null) drawerMenu.setColorFilter(iconTint);
        
        TextView rootStorageText = findViewById(R.id.root_storage_text);
        TextView storageText = findViewById(R.id.storage_text);
        if (rootStorageText != null) rootStorageText.setTextColor(textSecondary);
        if (storageText != null) storageText.setTextColor(textSecondary);
        
        updateDrawerIconColors(primaryColor);
    }
    
    private void updateDrawerIconColors(int primaryColor) {
        int[] bgIds = {
            R.id.icon_root_bg,
            R.id.icon_storage_bg,
            R.id.icon_plugins_bg,
            R.id.icon_text_editor_bg,
            R.id.icon_terminal_bg,
            R.id.icon_settings_bg,
            R.id.icon_about_bg
        };

        for (int id : bgIds) {
            View v = findViewById(id);
            if (v != null) {
                Drawable bg = v.getBackground();
                if (bg instanceof GradientDrawable) {
                    GradientDrawable shape = (GradientDrawable) bg.mutate();
                    shape.setColor(primaryColor);
                }
            }
        }
    }
    
    private void loadState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedLeft = prefs.getString(KEY_PATH_LEFT, DEFAULT_PATH);
        String savedRight = prefs.getString(KEY_PATH_RIGHT, DEFAULT_PATH);
        File leftDir = new File(savedLeft);
        File rightDir = new File(savedRight);
        if (leftDir.isDirectory() && leftDir.canRead()) {
            currentPathLeft = savedLeft;
        }
        if (rightDir.isDirectory() && rightDir.canRead()) {
            currentPathRight = savedRight;
        }
    }
    
    private void saveState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit()
            .putString(KEY_PATH_LEFT, currentPathLeft)
            .putString(KEY_PATH_RIGHT, currentPathRight)
            .putInt(KEY_SCROLL_LEFT, getCurrentScrollPosition(true))
            .putInt(KEY_SCROLL_RIGHT, getCurrentScrollPosition(false))
            .apply();
    }
    
    private int getCurrentScrollPosition(boolean isLeft) {
        RecyclerView recycler = isLeft ? recyclerLeft : recyclerRight;
        if (recycler != null && recycler.getLayoutManager() != null) {
            LinearLayoutManager lm = (LinearLayoutManager) recycler.getLayoutManager();
            return lm.findFirstVisibleItemPosition();
        }
        return 0;
    }
    
    private void initToolbar() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        drawerLayout = findViewById(R.id.drawer_layout);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
            R.string.app_name, R.string.app_name);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
    }
    
    private void initViews() {
        toolbarPath = findViewById(R.id.toolbar_path);
        topStats = findViewById(R.id.stats_left);
        diskInfo = findViewById(R.id.disk_info);
        recyclerLeft = findViewById(R.id.list_left);
        recyclerRight = findViewById(R.id.list_right);
        panelLeft = findViewById(R.id.panel_left);
        panelRight = findViewById(R.id.panel_right);
        
        btnBack = findViewById(R.id.btn_back);
        btnForward = findViewById(R.id.btn_forward);
        btnNew = findViewById(R.id.btn_new);
        btnSwap = findViewById(R.id.btn_swap);
        btnParent = findViewById(R.id.btn_parent);
        
        rootProgress = findViewById(R.id.root_progress);
        storageProgress = findViewById(R.id.storage_progress);
        rootStorageText = findViewById(R.id.root_storage_text);
        storageText = findViewById(R.id.storage_text);
        dynamicStorageContainer = findViewById(R.id.dynamic_storage_container);
        
        btnThemeToggle = findViewById(R.id.btn_theme_toggle);
        btnDrawerMenu = findViewById(R.id.btn_drawer_menu);
    }
    
    private void initDrawerSections() {
        LinearLayout sectionLocalHeader = findViewById(R.id.section_local_header);
        sectionLocalContent = findViewById(R.id.section_local_content);
        sectionLocalArrow = findViewById(R.id.section_local_arrow);
        
        LinearLayout sectionToolsHeader = findViewById(R.id.section_tools_header);
        sectionToolsContent = findViewById(R.id.section_tools_content);
        sectionToolsArrow = findViewById(R.id.section_tools_arrow);
        
        sectionLocalHeader.setOnClickListener(v -> toggleSection(sectionLocalContent, sectionLocalArrow));
        sectionToolsHeader.setOnClickListener(v -> toggleSection(sectionToolsContent, sectionToolsArrow));
        
        findViewById(R.id.nav_root_directory).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            navigateToPath("/", activePanel);
        });
        
        findViewById(R.id.nav_storage_directory).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            navigateToPath(DEFAULT_PATH, activePanel);
        });
        
        
        findViewById(R.id.nav_settings).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            Toast.makeText(this, "Settings not implemented", Toast.LENGTH_SHORT).show();
        });
        
        findViewById(R.id.nav_about).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            showAboutDialog();
        });
        
        findViewById(R.id.nav_plugins).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            Toast.makeText(this, "Plugin Manager not implemented", Toast.LENGTH_SHORT).show();
        });
        
        findViewById(R.id.nav_text_editor).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            Toast.makeText(this, "Text Editor not implemented", Toast.LENGTH_SHORT).show();
        });
        
        findViewById(R.id.nav_terminal).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            Toast.makeText(this, "Terminal not implemented", Toast.LENGTH_SHORT).show();
        });
        
        btnThemeToggle.setOnClickListener(v -> toggleTheme());
        
        btnDrawerMenu.setOnClickListener(this::showDrawerMenu);
        
        populateDynamicStorage();
    }
    
    private void populateDynamicStorage() {
        dynamicStorageContainer.removeAllViews();
        
        for (int i = 0; i < customPaths.size(); i++) {
            String path = customPaths.get(i);
            String name = new File(path).getName();
            if (name.isEmpty()) name = path;
            
            View itemView = LayoutInflater.from(this).inflate(R.layout.item_storage_path, dynamicStorageContainer, false);
            
            TextView nameText = itemView.findViewById(R.id.storage_name);
            ProgressBar progressBar = itemView.findViewById(R.id.storage_progress);
            TextView storageText = itemView.findViewById(R.id.storage_text);
            
            nameText.setText(name);
            progressBar.setVisibility(View.VISIBLE);
            storageText.setVisibility(View.VISIBLE);
            
            itemView.setTag(path);
            itemView.setOnClickListener(v -> {
                String selectedPath = (String) v.getTag();
                drawerLayout.closeDrawer(GravityCompat.START);
                navigateToPath(selectedPath, activePanel);
            });
            
            dynamicStorageContainer.addView(itemView);
            
            updateDynamicStorageInfo(path, progressBar, storageText);
        }
    }
    
    private void updateDynamicStorageInfo(String path, ProgressBar progressBar, TextView storageText) {
        executor.execute(() -> {
            try {
                File storageDir = new File(path);
                if (!storageDir.exists() || !storageDir.canRead()) {
                    mainHandler.post(() -> {
                        storageText.setText("Cannot access");
                    });
                    return;
                }
                
                StatFs stat = new StatFs(path);
                long total = stat.getBlockCountLong() * stat.getBlockSizeLong();
                long available = stat.getAvailableBlocksLong() * stat.getBlockSizeLong();
                long used = total - available;
                int percent = (int) ((used * 100) / total);
                
                String usedStr = formatSize(used);
                String availStr = formatSize(available);
                
                mainHandler.post(() -> {
                    progressBar.setProgress(percent);
                    storageText.setText(usedStr + " used, " + availStr + " available");
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    storageText.setText("Error reading");
                });
            }
        });
    }
    
    private void showDrawerMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, "Follow System Theme").setCheckable(true).setChecked(followSystemTheme);
        popup.getMenu().add(0, 2, 1, "Add Local Storage").setIcon(R.drawable.ic_local_storage);
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                followSystemTheme = !followSystemTheme;
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                prefs.edit().putBoolean(KEY_FOLLOW_SYSTEM_THEME, followSystemTheme).apply();
                if (followSystemTheme) {
                    currentPalette = 0;
                    prefs.edit().putInt(KEY_THEME_PALETTE, currentPalette).apply();
                }
                recreate();
                return true;
            } else if (item.getItemId() == 2) {
                openSafPicker();
            }
            return true;
        });
        popup.show();
    }
    
    private void openSafPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, REQUEST_CODE_SAF);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    loadBothPanels();
                    updateStorageInfo();
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == REQUEST_CODE_SAF) {
            if (resultCode == RESULT_OK && data != null) {
                Uri treeUri = data.getData();
                
                try {
                    getContentResolver().takePersistableUriPermission(treeUri, 
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                } catch (Exception ignored) {}
                
                String path = getPathFromTreeUri(treeUri);
                if (path != null && !customPaths.contains(path)) {
                    customPaths.add(path);
                    saveCustomPaths();
                    populateDynamicStorage();
                    Toast.makeText(this, "Storage added: " + path, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Could not access this location", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    
    private String getPathFromTreeUri(Uri treeUri) {
        if (treeUri == null) return null;
        
        String docId = treeUri.getLastPathSegment();
        if (docId == null) return null;
        
        if (docId.startsWith("primary:")) {
            return "/storage/" + docId.substring(8);
        }
        
        return treeUri.toString();
    }
    
    private void toggleSection(LinearLayout content, ImageView arrow) {
        if (content.getVisibility() == View.VISIBLE) {
            content.setVisibility(View.GONE);
            arrow.setRotation(0);
        } else {
            content.setVisibility(View.VISIBLE);
            arrow.setRotation(180);
        }
    }
    
    private void registerFileHandlers() {
        // Click handlers can be registered here
    }
    
    private void setupAdapters() {
        adapterLeft = new FileAdapter(this);
        adapterRight = new FileAdapter(this);
        
        LinearLayoutManager layoutLeft = new LinearLayoutManager(this);
        recyclerLeft.setLayoutManager(layoutLeft);
        recyclerLeft.setHasFixedSize(true);
        recyclerLeft.setItemAnimator(null);
        
        LinearLayoutManager layoutRight = new LinearLayoutManager(this);
        recyclerRight.setLayoutManager(layoutRight);
        recyclerRight.setHasFixedSize(true);
        recyclerRight.setItemAnimator(null);
        
        recyclerLeft.setAdapter(adapterLeft);
        recyclerRight.setAdapter(adapterRight);
        
        setupAdapterCallbacks(adapterLeft, true);
        setupAdapterCallbacks(adapterRight, false);
    }
    
    private void setupAdapterCallbacks(FileAdapter adapter, boolean isLeft) {
        adapter.setOnItemClickListener((view, position, item) -> {
            setActivePanel(isLeft ? 0 : 1);
            
            if (item.isParentDirectory()) {
                goUp(isLeft);
            } else if (item.isDirectory()) {
                navigateToPath(item.getPath(), isLeft ? 0 : 1);
            } else {
                openFile(item);
            }
        });
    }
    
    private boolean debounceClick() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastClickTime.get() < DEBOUNCE_DELAY) {
            return true;
        }
        lastClickTime.set(currentTime);
        return false;
    }
    
    private void setupListeners() {
        panelLeft.setOnClickListener(v -> setActivePanel(0));
        panelRight.setOnClickListener(v -> setActivePanel(1));
        
        recyclerLeft.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING || newState == RecyclerView.SCROLL_STATE_SETTLING) {
                    setActivePanel(0);
                }
            }
        });
        
        recyclerRight.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING || newState == RecyclerView.SCROLL_STATE_SETTLING) {
                    setActivePanel(1);
                }
            }
        });
        
        btnBack.setOnClickListener(v -> goBack());
        btnForward.setOnClickListener(v -> goForward());
        btnNew.setOnClickListener(v -> showNewDialog());
        btnSwap.setOnClickListener(v -> {
            if (activePanel == 0) {
                navigateToPath(currentPathLeft, 1);
            } else {
                navigateToPath(currentPathRight, 0);
            }
        });
        btnParent.setOnClickListener(v -> goUp(activePanel == 0));
    }
    
    private void navigateToPath(String path, int panelIndex) {
        if (debounceClick()) return;
        
        boolean isLeft = (panelIndex == 0);
        String currentPath = isLeft ? currentPathLeft : currentPathRight;
        
        if (path.equals(currentPath)) {
            loadPanel(isLeft);
            return;
        }
        
        if (isLeft) {
            backStackLeft.push(currentPathLeft);
            forwardStackLeft.clear();
            currentPathLeft = path;
        } else {
            backStackRight.push(currentPathRight);
            forwardStackRight.clear();
            currentPathRight = path;
        }
        
        loadPanel(isLeft);
        updateTopBar();
        saveState();
    }
    
    private void loadBothPanels() {
        loadPanel(true);
        loadPanel(false);
    }
    
    private void loadPanel(boolean isLeft) {
        String path = isLeft ? currentPathLeft : currentPathRight;
        
        if (isLeft) {
            if (loadingPathLeft != null) return;
            loadingPathLeft = path;
        } else {
            if (loadingPathRight != null) return;
            loadingPathRight = path;
        }
        
        final String targetPath = path;
        
        executor.execute(() -> {
            List<FileItem> items = bin.mg.main.FileUtils.listFiles(targetPath);
            
            String parentPath = bin.mg.main.FileUtils.getParentPath(targetPath);
            if (parentPath != null) {
                items.add(0, FileItem.createParentItem(parentPath));
            }
            
            final List<FileItem> finalItems = new ArrayList<>(items);
            final String finalPath = targetPath;
            
            mainHandler.post(() -> {
                if (isLeft) {
                    if (!finalPath.equals(currentPathLeft)) {
                        loadingPathLeft = null;
                        return;
                    }
                    loadingPathLeft = null;
                    adapterLeft.updateData(finalItems);
                } else {
                    if (!finalPath.equals(currentPathRight)) {
                        loadingPathRight = null;
                        return;
                    }
                    loadingPathRight = null;
                    adapterRight.updateData(finalItems);
                }
                updateTopBar();
                saveState();
            });
        });
    }
    
    private void goUp(boolean isLeft) {
        String current = isLeft ? currentPathLeft : currentPathRight;
        String parent = bin.mg.main.FileUtils.getParentPath(current);
        if (parent != null) {
            navigateToPath(parent, isLeft ? 0 : 1);
        }
    }
    
    private void goBack() {
        Stack<String> backStack = activePanel == 0 ? backStackLeft : backStackRight;
        Stack<String> forwardStack = activePanel == 0 ? forwardStackLeft : forwardStackRight;
        
        if (!backStack.isEmpty()) {
            forwardStack.push(activePanel == 0 ? currentPathLeft : currentPathRight);
            String previous = backStack.pop();
            boolean isLeft = (activePanel == 0);
            if (isLeft) {
                currentPathLeft = previous;
            } else {
                currentPathRight = previous;
            }
            loadPanel(isLeft);
            updateTopBar();
            saveState();
        }
    }
    
    private void goForward() {
        Stack<String> forwardStack = activePanel == 0 ? forwardStackLeft : forwardStackRight;
        
        if (!forwardStack.isEmpty()) {
            if (activePanel == 0) {
                backStackLeft.push(currentPathLeft);
            } else {
                backStackRight.push(currentPathRight);
            }
            String next = forwardStack.pop();
            boolean isLeft = (activePanel == 0);
            if (isLeft) {
                currentPathLeft = next;
            } else {
                currentPathRight = next;
            }
            loadPanel(isLeft);
            updateTopBar();
            saveState();
        }
    }
    
    private void setActivePanel(int panel) {
        activePanel = panel;
        updateSwapIcon();
        updateTopBar();
    }
    
    private void updateSwapIcon() {
        btnSwap.setImageResource(activePanel == 0 ? R.drawable.ic_swap_left_active : R.drawable.ic_swap_right_active);
    }
    
    private void updateTopBar() {
        String path = activePanel == 0 ? currentPathLeft : currentPathRight;
        toolbarPath.setText(path);
        
        FileAdapter adapter = activePanel == 0 ? adapterLeft : adapterRight;
        List<FileItem> items = adapter.getItems();
        
        int folderCount = 0;
        int fileCount = 0;
        for (FileItem item : items) {
            if (!item.isParentDirectory()) {
                if (item.isDirectory()) folderCount++;
                else fileCount++;
            }
        }
        
        String finalStats = "Folders: " + folderCount + "  Files: " + fileCount;
        topStats.setText(finalStats);
        
        executor.execute(() -> {
            long available = 0, total = 0;
            try {
                File currentDir = new File(path);
                String statPath = path;
                while (!new File(statPath).canRead() && statPath.lastIndexOf('/') > 0) {
                    statPath = statPath.substring(0, statPath.lastIndexOf('/'));
                }
                if (statPath.isEmpty()) statPath = "/";
                StatFs stat = new StatFs(statPath);
                available = stat.getAvailableBlocksLong() * stat.getBlockSizeLong();
                total = stat.getBlockCountLong() * stat.getBlockSizeLong();
            } catch (Exception ignored) {}
            
            String finalDisk = String.format("Disk: %.1fGB/%.1fGB", 
                available / (1024.0 * 1024 * 1024), 
                total / (1024.0 * 1024 * 1024));
            
            mainHandler.post(() -> {
                diskInfo.setText(finalDisk);
            });
        });
    }
    
    private void updateStorageInfo() {
        executor.execute(() -> {
            try {
                StatFs rootStat = new StatFs("/");
                long rootTotal = rootStat.getBlockCountLong() * rootStat.getBlockSizeLong();
                long rootAvail = rootStat.getAvailableBlocksLong() * rootStat.getBlockSizeLong();
                long rootUsed = rootTotal - rootAvail;
                int rootPercent = (int) ((rootUsed * 100) / rootTotal);
                
                File storageDir = Environment.getExternalStorageDirectory();
                StatFs storageStat = new StatFs(storageDir.getPath());
                long storageTotal = storageStat.getBlockCountLong() * storageStat.getBlockSizeLong();
                long storageAvail = storageStat.getAvailableBlocksLong() * storageStat.getBlockSizeLong();
                long storageUsed = storageTotal - storageAvail;
                int storagePercent = (int) ((storageUsed * 100) / storageTotal);
                
                String rootUsedStr = formatSize(rootUsed);
                String rootAvailStr = formatSize(rootAvail);
                String storageUsedStr = formatSize(storageUsed);
                String storageAvailStr = formatSize(storageAvail);
                
                mainHandler.post(() -> {
                    rootProgress.setProgress(rootPercent);
                    rootStorageText.setText(rootUsedStr + " used, " + rootAvailStr + " available");
                    
                    storageProgress.setProgress(storagePercent);
                    storageText.setText(storageUsedStr + " used, " + storageAvailStr + " available");
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    rootStorageText.setText("Unable to calculate");
                    storageText.setText("Unable to calculate");
                });
            }
        });
    }
    
    private String formatSize(long size) {
        if (size < 0) size = 0;
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format(Locale.getDefault(), "%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format(Locale.getDefault(), "%.1f MB", size / (1024.0 * 1024));
        return String.format(Locale.getDefault(), "%.1f GB", size / (1024.0 * 1024 * 1024));
    }
    
    private void showNewDialog() {
        EditText input = new EditText(this);
        input.setHint("Name");
        
        new AlertDialog.Builder(this)
            .setTitle("Create")
            .setView(input)
            .setNegativeButton("File", (dialog, which) -> {
                String name = input.getText().toString().trim();
                if (!name.isEmpty()) createFile(name);
            })
            .setPositiveButton("Folder", (dialog, which) -> {
                String name = input.getText().toString().trim();
                if (!name.isEmpty()) createFolder(name);
            })
            .setNeutralButton("Cancel", null)
            .show();
    }
    
    private void createFolder(String name) {
        String path = activePanel == 0 ? currentPathLeft : currentPathRight;
        executor.execute(() -> {
            boolean success = bin.mg.main.FileUtils.createFolder(path, name);
            mainHandler.post(() -> {
                if (success) {
                    loadPanel(activePanel == 0);
                } else {
                    Toast.makeText(this, "Failed to create folder", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
    
    private void createFile(String name) {
        String path = activePanel == 0 ? currentPathLeft : currentPathRight;
        executor.execute(() -> {
            try {
                File file = new File(path, name);
                boolean created = file.createNewFile();
                mainHandler.post(() -> {
                    if (created) {
                        loadPanel(activePanel == 0);
                    } else {
                        Toast.makeText(this, "File already exists", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> Toast.makeText(this, "Failed to create file", Toast.LENGTH_SHORT).show());
            }
        });
    }
    
    private void openFile(FileItem file) {
        String path = file.getPath();
        if (bin.mg.main.FileUtils.isTextFile(path)) {
            bin.mg.main.ui.editor.FileEditorActivity.start(this, path);
        } else if (bin.mg.main.FileUtils.isApkFile(path)) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                uri = androidx.core.content.FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", new File(path));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                uri = Uri.fromFile(new File(path));
            }
            intent.setDataAndType(uri, bin.mg.main.FileUtils.getMimeType(path));
            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "No app found to install APK", Toast.LENGTH_SHORT).show();
            }
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                uri = androidx.core.content.FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", new File(path));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                uri = Uri.fromFile(new File(path));
            }
            intent.setDataAndType(uri, bin.mg.main.FileUtils.getMimeType(path));
            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "No app found", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void showAboutDialog() {
        new AlertDialog.Builder(this)
            .setTitle("MM Manager")
            .setMessage("MM Manager v1.0\n\nDual Panel File Manager")
            .setPositiveButton("OK", null)
            .show();
    }
    
    private boolean hasPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                   checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }
    
    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_CODE_PERMISSION);
            } catch (Exception e) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, REQUEST_CODE_PERMISSION);
            }
        } else {
            requestPermissions(new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, REQUEST_CODE_PERMISSION);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                loadBothPanels();
                updateStorageInfo();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        applyThemeColors();
        
        if (hasPermissions()) {
            if (adapterLeft.getItemCount() == 0 && adapterRight.getItemCount() == 0) {
                loadBothPanels();
                updateStorageInfo();
            } else {
                loadPanel(activePanel == 0);
                loadPanel(activePanel == 1);
                updateTopBar();
            }
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        saveState();
    }
    
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return;
        }
        
        Stack<String> backStack = activePanel == 0 ? backStackLeft : backStackRight;
        if (!backStack.isEmpty()) {
            goBack();
        } else {
            super.onBackPressed();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveState();
        executor.shutdown();
    }
}