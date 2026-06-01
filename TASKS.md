# MM Manager - Tasks & Goals

## Source Location
- **App source:** `/storage/emulated/0/CodeOnTheGoProjects/MM Manager`
- **GitHub:** `https://github.com/Magic1-Mods/mm-manager` (private)
- **CI:** GitHub Actions on push to `main` builds via `.github/workflows/android.yml`

## Screenshots (Reference UI)
All in `/storage/emulated/0/Pictures/Screenshot/`:

## Reference App
- **Dex Editor:** `/storage/emulated/0/Download/Dex-Editor-Android-1.3_final.zip`
  - Use its UI structure (toolbar, preferences, editor) as template for the text editor

---

## Completed Tasks

### Java-to-Kotlin Conversion
- All 17 Java source files converted to idiomatic Kotlin
- Kotlin Gradle plugin 1.9.22, core-ktx 1.12.0 added
- All original .java files deleted

### UI Redesign - Drawer
- Added Recycle Bin, Screen Color Picker, Signing Key, Common Passwords tools
- Overflow menu: Theme follows system, Add local storage, Manage tools group, Preferences
- Storage info uses compact format (831M, 57.66G)
- Tools section visible by default
- Drawer header compact (12dp padding)
- Custom path items use 32dp circles with theme-colored backgrounds
- Custom path icons colored with primary theme color

### File List
- Item height reduced from 48dp to 42dp with smaller icons (30dp/16dp)
- File sizes shown next to dates (e.g. "26-05-31 18:42  222.22K")
- Bottom bar icons greyed when back/forward stacks empty
- Root `/` navigation uses `su -c ls /` fallback when File.listFiles() fails

### SAF / Custom Storage
- URL-encoded document IDs decoded properly
- App names extracted from `/data/data/` paths via PackageManager
- Display names stored alongside paths in SharedPreferences
- Tree URIs stored alongside custom paths
- `SafHelper` utility uses ContentResolver for SAF file operations
- `loadPanel()` falls back to SAF listing when File.listFiles() returns empty
- Volume path resolution searches `/storage/`, `/mnt/media_rw/` mount points

### Text Editor (incomplete - needs full rewrite)
- New layout with MT Manager-style toolbar (back, search, undo, redo, save, edit mode, overflow)
- Search bar with prev/next/close
- Undo/redo (200-level stack)
- Preferences fragment overlay (font size, word wrap, line numbers, auto save, auto indent, syntax highlight)
- SAF file read/write support
- Unsaved changes dialog
- Tab insertion
- Replaced deprecated AsyncTask with Executors

---

## Remaining / Broken Tasks

### 1. Text Editor - Full Rewrite to Match MT Manager Exactly
**Current state:** The preferences UI and overall layout don't match MT Manager at all. Need to copy Dex Editor's approach and adapt.
- **Study Dex Editor** at `/storage/emulated/0/Download/Dex-Editor-Android-1.3_final.zip`
- **Adapt the UI** from Dex Editor to match MT Manager's text editor style
- **Preferences** should match the fragment overlay pattern from MT Manager (or Dex Editor)
- **Toolbar icons** and layout must match MT Manager exactly
- **Info bar** (filename + line:col) should match MT Manager styling
- **Symbol bar** styling needs to match MT Manager
- **Dark mode / theming** for editor

### 2. SAF "Cannot Access" for App Private Directories
- App-private directories (e.g. `/data/data/com.itsaky.androidide/files/...`) show "Cannot access" or raw URL-encoded paths
- Need `DocumentsContract` receiver or proper document tree intent handling in the manifest
- Need intent filter for handling/document tree content URIs

### 3. Bottom Bar Icons
- Need to match MT Manager's bottom bar icon set exactly
- Currently using `ic_transfer` (custom) but may not match MT Manager style
- Icons should be 20dp viewport, simple thin stroke style like MT Manager

### 4. Image Thumbnails
- Image thumbnails not loading reliably in file list
- MT Manager shows thumbnails for images (Screenshot_*.jpg visible in file list)
- Check Glide configuration and loadImageThumbnail method

### 5. APK Icon Loading
- Currently uses `setTarget` on `iconContainer.background` — should load into `icon` ImageView instead
- Background approach causes layout issues and doesn't match MT Manager

### 6. Right Panel Filename Truncation
- Long filenames in right panel truncated (e.g. "Screenshot_20260531-18...")
- May need `maxLines="1"` with `ellipsize="middle"` instead of "end"
- Or adjust layout padding/text sizes

### 7. Root `/` Navigation
- `File.listFiles()` returns null on non-rooted devices
- MT Manager clearly browses `/` on same device (shows data, etc, mnt, proc, product, sdcard, storage, system, system_ext, vendor)
- Need to investigate how MT Manager achieves root access browsing
- Current `su -c ls /` fallback may not work without root permission dialog

### 8. Toolbar Overflow Button
- `btn_toolbar_menu` was hidden but still referenced in `initToolbar()`
- Need to clean up: remove the findViewById call, keep layout hidden

### 9. Theme Switching
- `toggleTheme()` cycles palettes but `isDarkMode` calculation doesn't consider `currentPalette == 2`
- Need to verify dark mode works correctly with palette cycling

---

## Build Info
- **Credentials:** GitHub PAT stored in `~/.git-credentials`
- **No local build** — push to GitHub and let Actions build
