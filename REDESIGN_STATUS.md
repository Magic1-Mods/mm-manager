# MM Manager - Editor Redesign Status

## Completed Work

### 1. Vector Drawable Icons Created (20 new icons)
All edit menu and overflow menu items now use proper Android vector drawables instead of text/unicode:
- `ic_copy_line.xml`, `ic_cut_line.xml`, `ic_delete_line.xml`, `ic_empty_line.xml`
- `ic_replace_line.xml`, `ic_duplicate_line.xml`
- `ic_uppercase.xml`, `ic_lowercase.xml`
- `ic_indent_increase.xml`, `ic_indent_decrease.xml`
- `ic_toggle_comment.xml`, `ic_reformat_code.xml`
- `ic_syntax.xml`, `ic_navigate_before.xml`, `ic_navigate_next.xml`
- `ic_jump_to_line.xml`, `ic_soft_wrap.xml`, `ic_read_only.xml`
- `ic_close_file.xml`, `bg_symbol_handle.xml`

### 2. Symbol Panel Rebuilt as Custom View
- **New file:** `SymbolPanel.java` - Custom `LinearLayout` with drag-to-expand/collapse
- 3 rows of symbols: `→ / + - * = <` / `> " ' ; | \ -` / `() [] {} ...`
- `→` inserts tab character
- Drag up to expand, drag down to collapse
- Smooth animations, state preserved
- `OnSymbolClickListener` interface for the activity
- Replaces the old inline programmatic symbol bar code in TextEditorActivity

### 3. Layout Updated
- `activity_file_editor.xml` - Removed old `symbol_bar` + `symbol_input` LinearLayouts, replaced with `<bin.mg.main.file.SymbolPanel>` custom view at bottom
- `layout_symbol_panel.xml` - New layout for the symbol panel component

### 4. TextEditorActivity.kt Major Rewrite
- Removed old `setupSymbolBar()`, `createSymbolRow()`, `setupKeyboardListener()`, `updateSymbolBarForKeyboard()`, `expandSymbolBar()`, `collapseSymbolBar()` (~200 lines removed)
- Uses `SymbolPanel` custom view instead
- **Edit menu** now uses `addMenuItem()` helper with proper drawable icons for all 14 items including "Reformat code"
- **Overflow menu** rewritten with:
  - "File" submenu with "Save"
  - Proper icons for Search, Syntax, Previous/Next position, Jump to line
  - Checkable toggle items for Soft wrap, Read-only, Smooth mode, Code completion
  - Preferences and Close file with icons
- Removed `EditorPreferencesFragment` import/implementation, now uses `PreferencesActivity`
- Added `onResume()` to reload editor settings when returning from preferences
- Removed unused imports (`Menu`, `MenuItem`, `ColorDrawable`, `Drawable`, `MenuBuilder`, `MenuItemImpl`, `GapBuffer`)

### 5. PreferencesActivity Created (New Full-Screen Activity)
- **New file:** `PreferencesActivity.kt` - Programmatic ScrollView with sections matching MT Manager screenshots
- **Appearance section:** Font, Font size, Tab size, Show indent guides, Show ASCII/Unicode control chars, Show soft wrap arrows, Show line numbers, Fixed line numbers, Show blank symbol, Hide single space
- **Highlight section:** Syntax highlight toggle, Syntax files manager
- **Completion section:** Apply completion on Enter, Max height of completion window
- **Customize section:** Edit function bar, Edit floating menus, Edit tool menus (TODO stubs)
- **Function section:** Function bar, Enable magnifier, Pinch to zoom, Auto indent, Use tabs, Keep word when soft wrapping, Threshold for smooth mode
- **Other section:** Preference for keeping files, Check if modified, Check if deleted, Double confirm before exit
- Registered in `AndroidManifest.xml`

### 6. Dead Code & Unused Resources Removed
- Deleted `EditorPreferencesFragment.kt`
- Deleted `TextEditorPreferencesFragment.kt`
- Deleted `dialog_editor_preferences.xml`
- Deleted `fragment_editor_preferences.xml`
- Deleted `file_editor_menu.xml`
- Removed `EditorPreferencesDialog` style from `themes.xml`
- Updated `proguard-rules.pro` to reference new classes

### 7. EditView.java Bug Fixes & Optimizations
- **Null-safe `onTextChanged()`** - Added null check for `mTextListener`
- **IME composing region** - Added `mComposingStart`/`mComposingEnd` fields; `setComposingText()` now properly tracks composing region instead of double-inserting text via `insert()`; `commitText()` clears composing region before inserting; `finishComposingText()` dismisses composition
- **`getCursorCapsMode()` optimized** - No longer converts entire buffer to string; reads only last 500 chars before cursor
- **`deleteSurroundingText()` optimized** - Now uses batch edits for multi-character deletes
- **Removed excessive logging** - Removed ~15 `Log.d()` calls from TextInputConnection, magnifier, autocomplete that hurt performance on large files

### 8. Large File Performance Optimizations (up to 3 MB)
- **Max file size increased** from 2 MB to 3 MB in `TextEditorActivity`
- **`updateWordSet()` rewritten** - For files > 200KB, scans only a 100KB window around cursor instead of full buffer; capped at 20,000 unique words
- **`filterAndShowSuggestions()` optimized** - Pre-computes lowercase prefix outside loop
- **`find()` optimized** - Added try/catch for invalid regex; capped at 5,000 matches
- **`setCursorPosition(float, float)` optimized** - Reuses `mTempWidths` float array instead of allocating new `float[]` per touch event
- **Cached Color.parseColor constants** - `COLOR_LINE_NUMBER_BG`, `COLOR_SEPARATOR`, `COLOR_LINE_NUMBER_TEXT`, `COLOR_CURRENT_LINE_BG`, `COLOR_SELECTION_BG`, `COLOR_LINE_SELECT_BG`, `COLOR_MATCH_HIGHLIGHT`, `COLOR_MATCH_CURRENT` - avoids parsing in every draw frame

## What Needs To Be Done

### High Priority
1. **Test compilation** - Run `./gradlew assembleDebug` on CI to verify everything compiles
2. **PreferencesActivity dark mode** - Currently hardcoded to white background; should respect `ThemeManager.isDarkMode()` like the rest of the app
3. **Preferences persistence** - Many new preference keys (`show_indent_guides`, `show_ascii_control`, etc.) are saved but not read/applied anywhere in EditView.java or TextEditorActivity
4. **"Edit function bar", "Edit floating menus", "Edit tool menus"** - Currently show TODO toasts; these need actual implementations
5. **"Syntax files manager"** - Currently shows TODO toast
6. **"Reformat code"** - Menu item exists but shows TODO toast; needs implementation

### Medium Priority
7. **Symbol panel keyboard behavior** - The symbol panel should ideally stay pinned above the keyboard when it opens. Currently it's a fixed LinearLayout at the bottom; may need `WindowInsetsListener` or `ViewTreeObserver` to resize when keyboard appears
8. **Symbol panel drag gestures** - The drag-to-expand uses raw touch on the entire panel container, not just the handle. Should refine to only respond to handle area drags
9. **Overflow menu checkmark refresh** - The checkmarks on toggle items (Soft wrap, Read-only, etc.) use PopupMenu's built-in checkable but the state doesn't visually update on re-open since we're not using XML menu
10. **Position history** - `pushCurrentPosition()` is called on Jump to line but not on other navigation (word click, search result click, etc.)
11. **`drawEditorContent()` is dead code** - This method is never called from outside; `onDraw()` handles everything. Should be removed.

### Low Priority
12. **`GapBufferPro.java`** - Marked "Not ready yet", experimental Editable/Spannable implementation. Could be removed to reduce codebase
13. **`WordWrapLayout.java`** - May be unused dead code from the original editor
14. **`toolbar_overflow.xml`** - Only used by `MainActivity.kt`, not by the editor. Verify it's still needed
15. **`ic_file.xml`** was deleted - Was a duplicate of an existing icon. Verify no references remain.
16. **Proguard** - `shrinkResources true` and `minifyEnabled true` on debug builds may slow builds; consider disabling for debug

## Key Files Modified
| File | Changes |
|------|---------|
| `TextEditorActivity.kt` | Major rewrite: symbol panel, menus, preferences integration |
| `EditView.java` | Bug fixes (IME composing, null safety), performance optimizations |
| `SymbolPanel.java` | **NEW** - Custom draggable symbol panel |
| `PreferencesActivity.kt` | **NEW** - Full-screen preferences matching MT Manager |
| `activity_file_editor.xml` | Replaced symbol_bar with SymbolPanel custom view |
| `layout_symbol_panel.xml` | **NEW** - Symbol panel layout |
| `AndroidManifest.xml` | Added PreferencesActivity |
| `proguard-rules.pro` | Updated keep rules for new classes |
| `themes.xml` | Removed unused EditorPreferencesDialog style |
| 20 new drawable XMLs | Vector icons for menus |
| 4 files deleted | Old preference fragments and layouts |

## Key Files NOT Modified (but relevant)
| File | Notes |
|------|-------|
| `GapBuffer.java` | Core data structure, no changes needed |
| `MHSyntaxHighlightEngine.java` | Syntax highlighting, no changes needed |
| `ClipboardPanel.java` | Selection clipboard panel, no changes needed |
| `SelectionMenuPopup.java` | Text selection popup, no changes needed |
| `build.gradle` | No dependency changes needed |
