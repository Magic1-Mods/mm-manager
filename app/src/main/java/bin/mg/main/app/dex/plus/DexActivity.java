package bin.mg.main.app.dex.plus;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.view.GravityCompat;

import bin.mg.main.R;
import com.google.android.material.tabs.TabLayout;
import androidx.drawerlayout.widget.DrawerLayout;

import java.util.HashMap;
import java.util.List;

public class DexActivity extends AppCompatActivity {

    private List<String> classes;
    private TabPage myPager;
    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private View contentContainer;
    private View loadingContainer;
    private boolean dexLoaded;
    private String currentDexName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dex);

        HashMap<String, String> dexP = (HashMap<String, String>) getIntent().getSerializableExtra("dexP");

        toolbar = (Toolbar) findViewById(R.id.dex_toolbar);
        tabLayout = (TabLayout) findViewById(R.id.dex_tab_layout);
        viewPager = (ViewPager) findViewById(R.id.dex_view_pager);
        contentContainer = findViewById(R.id.dex_content_container);
        loadingContainer = findViewById(R.id.dex_loading_container);

        if (dexP != null && dexP.size() == 1) {
            currentDexName = dexP.keySet().iterator().next();
        } else {
            currentDexName = null;
        }

        initToolbar();
        loadClasses(dexP);
    }

    private void initToolbar() {
        setSupportActionBar(toolbar);
        drawerLayout = (DrawerLayout) findViewById(R.id.dex_drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
            getSupportActionBar().setSubtitle("");
        }
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    private void loadClasses(final HashMap<String, String> dexP) {
        if (dexP == null || dexP.isEmpty()) {
            showDexContent(dexP, null);
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<String> loadedClasses = ClassTreeGet.getAllClasses(dexP);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isFinishing() && !isDestroyed()) {
                            showDexContent(dexP, loadedClasses);
                        }
                    }
                });
            }
        }).start();
    }

    private void showDexContent(HashMap<String, String> dexP, List<String> loadedClasses) {
        classes = loadedClasses;

        String[] tabs = getResources().getStringArray(R.array.dex_tab_titles);
        myPager = new TabPage(this, tabs, classes);
        viewPager.setAdapter(myPager);
        tabLayout.setupWithViewPager(viewPager);

        contentContainer.setVisibility(View.VISIBLE);
        loadingContainer.setVisibility(View.GONE);
        dexLoaded = true;

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.dex_title);
            if (currentDexName != null) {
                getSupportActionBar().setSubtitle(currentDexName);
            } else {
                getSupportActionBar().setSubtitle(R.string.dex_subtitle);
            }
        }

        invalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        if (dexLoaded) {
            getMenuInflater().inflate(R.menu.menu_dex, menu);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_build) {
            Toast.makeText(this, "Build: Coming soon", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_settings) {
            Toast.makeText(this, "Settings: Coming soon", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_about) {
            Toast.makeText(this, "Dex Editor plus", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
