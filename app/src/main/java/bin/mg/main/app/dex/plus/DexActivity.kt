package bin.mg.main.app.dex.plus

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.viewpager.widget.ViewPager
import bin.mg.main.R
import bin.mg.main.utils.theme.ThemeManager
import com.google.android.material.tabs.TabLayout
import kotlin.concurrent.thread

class DexActivity : AppCompatActivity() {

    private var classes: List<String>? = null
    private var myPager: TabPage? = null
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toolbar: Toolbar
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager
    private lateinit var contentContainer: View
    private lateinit var loadingContainer: View
    private var dexLoaded = false
    private var currentDexName: String? = null

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyActivityTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dex)

        val dexP = intent.getSerializableExtra("dexP") as? HashMap<String, String>

        toolbar = findViewById(R.id.dex_toolbar)
        tabLayout = findViewById(R.id.dex_tab_layout)
        viewPager = findViewById(R.id.dex_view_pager)
        contentContainer = findViewById(R.id.dex_content_container)
        loadingContainer = findViewById(R.id.dex_loading_container)
        drawerLayout = findViewById(R.id.dex_drawer_layout)

        if (dexP != null && dexP.size == 1) {
            currentDexName = dexP.keys.first()
        } else {
            currentDexName = null
        }

        initToolbar()
        applyTheme()
        loadClasses(dexP)
    }

    private fun initToolbar() {
        setSupportActionBar(toolbar)
        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
    }

    private fun applyTheme() {
        val primary = ThemeManager.primary(this)
        val accent = ThemeManager.accent(this)
        val isDark = ThemeManager.isDarkMode(this)

        window.statusBarColor = primary

        toolbar.setBackgroundColor(primary)
        toolbar.navigationIcon?.colorFilter = PorterDuffColorFilter(0xFFFFFFFF.toInt(), PorterDuff.Mode.SRC_IN)
        for (i in 0 until toolbar.menu.size()) {
            toolbar.menu.getItem(i).icon?.colorFilter = PorterDuffColorFilter(0xFFFFFFFF.toInt(), PorterDuff.Mode.SRC_IN)
        }

        tabLayout.setTabTextColors(ThemeManager.textSecondary(this), if (isDark) 0xFFFFFFFF.toInt() else 0xFF000000.toInt())
        tabLayout.setSelectedTabIndicatorColor(accent)

        if (isDark) {
            loadingContainer.setBackgroundColor(0xFF212121.toInt())
            contentContainer.setBackgroundColor(0xFF212121.toInt())
            val rootView = findViewById<View>(android.R.id.content)
            rootView?.setBackgroundColor(0xFF212121.toInt())
        } else {
            loadingContainer.setBackgroundColor(0xFFFAFAFA.toInt())
            contentContainer.setBackgroundColor(0xFFFFFFFF.toInt())
            val rootView = findViewById<View>(android.R.id.content)
            rootView?.setBackgroundColor(0xFFFAFAFA.toInt())
        }

        myPager?.let { pager ->
            pager.notifyDataSetChanged()
        }

        if (dexLoaded) {
            supportActionBar?.apply {
                setTitle(R.string.dex_title)
                subtitle = currentDexName ?: getString(R.string.dex_subtitle)
            }
        }
    }

    private fun loadClasses(dexP: HashMap<String, String>?) {
        if (dexP.isNullOrEmpty()) {
            showDexContent(dexP, null)
            return
        }
        thread {
            val loadedClasses = ClassTreeGet.getAllClasses(dexP)
            runOnUiThread {
                if (!isFinishing && !isDestroyed) {
                    showDexContent(dexP, loadedClasses)
                }
            }
        }
    }

    private fun showDexContent(dexP: HashMap<String, String>?, loadedClasses: List<String>?) {
        classes = loadedClasses
        val tabs = resources.getStringArray(R.array.dex_tab_titles)
        myPager = TabPage(this, tabs, classes)
        viewPager.adapter = myPager
        tabLayout.setupWithViewPager(viewPager)

        contentContainer.visibility = View.VISIBLE
        loadingContainer.visibility = View.GONE
        dexLoaded = true

        applyTheme()
        invalidateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean = true

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.clear()
        if (dexLoaded) {
            menuInflater.inflate(R.menu.menu_dex, menu)
        }
        for (i in 0 until menu.size()) {
            menu.getItem(i).icon?.colorFilter = PorterDuffColorFilter(0xFFFFFFFF.toInt(), PorterDuff.Mode.SRC_IN)
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_build -> {
                Toast.makeText(this, "Build: Coming soon", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_settings -> {
                Toast.makeText(this, "Settings: Coming soon", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_about -> {
                Toast.makeText(this, "Dex Editor plus", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}