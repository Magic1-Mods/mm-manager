package bin.mg.main.editor.complete;

import java.util.*;

/**
 * Comprehensive XML/HTML tag and attribute completion database.
 * Supports HTML5, Android Layout XML, Android Manifest XML, and general XML.
 */
public class XmlCompletionProvider {

    private final Map<String, List<String>> tagAttributes = new LinkedHashMap<>();
    private final Map<String, List<String>> attrValues = new LinkedHashMap<>();
    private final Set<String> tagNames = new LinkedHashSet<>();
    private final Set<String> allAttributes = new LinkedHashSet<>();
    private final List<String> tagNameList = new ArrayList<>();

    private static volatile XmlCompletionProvider instance;

    public static XmlCompletionProvider getInstance() {
        if (instance == null) {
            synchronized (XmlCompletionProvider.class) {
                if (instance == null) {
                    instance = new XmlCompletionProvider();
                }
            }
        }
        return instance;
    }

    private XmlCompletionProvider() {
        initHtmlTags();
        initAndroidLayoutTags();
        initAndroidManifestTags();
        initCommonAttributes();
        initAttributeValues();
        buildIndex();
    }

    private void addTag(String tag, String... attrs) {
        tagNames.add(tag);
        List<String> list = new ArrayList<>();
        for (String a : attrs) {
            list.add(a);
            allAttributes.add(a);
        }
        tagAttributes.put(tag, list);
    }

    private void addAttrValues(String attr, String... values) {
        List<String> list = new ArrayList<>();
        for (String v : values) {
            list.add(v);
        }
        attrValues.put(attr, list);
    }

    private void initHtmlTags() {
        String[] common = {"id", "class", "style", "title", "lang", "dir", "hidden", "tabindex", "accesskey", "contenteditable", "draggable", "spellcheck", "data-*"};
        String[] events = {"onclick", "ondblclick", "onchange", "oninput", "onkeydown", "onkeyup", "onkeypress", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onfocus", "onblur", "onsubmit", "onreset", "onscroll", "onload", "onerror", "onresize", "ontouchstart", "ontouchend", "ontouchmove"};

        addTag("html", concat(common, "manifest", "xmlns"));
        addTag("head", concat(common, "profile"));
        addTag("body", concat(common, events, "onload", "onunload", "onhashchange"));
        addTag("title", common);
        addTag("base", "href", "target");
        addTag("link", "rel", "href", "type", "media", "sizes", "crossorigin", "integrity", "as", "hreflang");
        addTag("meta", "charset", "name", "content", "http-equiv", "property", "scheme");
        addTag("style", "type", "media", "scoped", "nonce");
        addTag("script", "src", "type", "async", "defer", "crossorigin", "integrity", "nomodule", "nonce", "charset", "language");
        addTag("noscript", common);

        addTag("div", concat(common, "align"));
        addTag("span", common);
        addTag("p", concat(common, "align"));
        addTag("br", common);
        addTag("hr", concat(common, "align", "size", "width", "noshade", "color"));
        addTag("h1", concat(common, "align"));
        addTag("h2", concat(common, "align"));
        addTag("h3", concat(common, "align"));
        addTag("h4", concat(common, "align"));
        addTag("h5", concat(common, "align"));
        addTag("h6", concat(common, "align"));
        addTag("b", common);
        addTag("i", common);
        addTag("u", common);
        addTag("s", common);
        addTag("em", common);
        addTag("strong", common);
        addTag("small", common);
        addTag("sub", common);
        addTag("sup", common);
        addTag("mark", common);
        addTag("del", common);
        addTag("ins", common);
        addTag("pre", common);
        addTag("code", common);
        addTag("kbd", common);
        addTag("samp", common);
        addTag("var", common);
        addTag("cite", common);
        addTag("abbr", common);
        addTag("address", common);
        addTag("blockquote", concat(common, "cite"));
        addTag("q", concat(common, "cite"));
        addTag("dl", common);
        addTag("dt", common);
        addTag("dd", common);
        addTag("ol", concat(common, "type", "start", "reversed"));
        addTag("ul", concat(common, "type"));
        addTag("li", concat(common, "type", "value"));
        addTag("table", concat(common, "border", "cellpadding", "cellspacing", "width", "summary", "rules", "frame"));
        addTag("caption", concat(common, "align"));
        addTag("thead", concat(common, "align", "valign"));
        addTag("tbody", concat(common, "align", "valign"));
        addTag("tfoot", concat(common, "align", "valign"));
        addTag("tr", concat(common, "align", "valign", "bgcolor"));
        addTag("th", concat(common, "align", "valign", "colspan", "rowspan", "scope", "abbr", "width", "height", "bgcolor"));
        addTag("td", concat(common, "align", "valign", "colspan", "rowspan", "headers", "width", "height", "bgcolor"));
        addTag("col", concat(common, "span", "width", "align", "valign"));
        addTag("colgroup", concat(common, "span", "width", "align", "valign"));
        addTag("a", concat(common, events, "href", "target", "rel", "download", "hreflang", "type", "ping", "media", "referrerpolicy"));
        addTag("img", concat(common, events, "src", "alt", "width", "height", "loading", "decoding", "crossorigin", "referrerpolicy", "sizes", "srcset", "ismap", "usemap"));
        addTag("form", concat(common, events, "action", "method", "enctype", "accept-charset", "name", "target", "novalidate", "autocomplete", "rel"));
        addTag("input", concat(common, events, "type", "name", "value", "placeholder", "checked", "disabled", "readonly", "required", "maxlength", "minlength", "pattern", "size", "step", "min", "max", "multiple", "accept", "autocomplete", "autofocus", "form", "formaction", "formenctype", "formmethod", "formnovalidate", "formtarget", "list", "src", "alt", "height", "width", "inputmode", "dirname"));
        addTag("textarea", concat(common, events, "name", "rows", "cols", "placeholder", "disabled", "readonly", "required", "maxlength", "minlength", "wrap", "autocomplete", "autofocus", "form", "spellcheck"));
        addTag("select", concat(common, events, "name", "size", "multiple", "disabled", "required", "autofocus", "form"));
        addTag("option", concat(common, "value", "selected", "disabled", "label"));
        addTag("optgroup", concat(common, "label", "disabled"));
        addTag("button", concat(common, events, "type", "name", "value", "disabled", "autofocus", "form", "formaction", "formenctype", "formmethod", "formnovalidate", "formtarget", "popovertarget"));
        addTag("label", concat(common, events, "for", "form"));
        addTag("fieldset", concat(common, "disabled", "form", "name"));
        addTag("legend", common);
        addTag("datalist", concat(common, "id"));
        addTag("output", concat(common, "for", "form", "name"));
        addTag("progress", concat(common, "value", "max"));
        addTag("meter", concat(common, "value", "min", "max", "low", "high", "optimum"));
        addTag("iframe", concat(common, "src", "srcdoc", "name", "width", "height", "sandbox", "allow", "allowfullscreen", "loading", "referrerpolicy"));
        addTag("canvas", concat(common, "width", "height"));
        addTag("svg", concat(common, "width", "height", "viewBox", "xmlns", "version"));
        addTag("nav", common);
        addTag("header", common);
        addTag("footer", common);
        addTag("main", common);
        addTag("section", common);
        addTag("article", common);
        addTag("aside", common);
        addTag("figure", common);
        addTag("figcaption", common);
        addTag("details", concat(common, "open"));
        addTag("summary", common);
        addTag("dialog", concat(common, "open"));
        addTag("data", concat(common, "value"));
        addTag("time", concat(common, "datetime"));
        addTag("wbr", common);
        addTag("video", concat(common, events, "src", "poster", "width", "height", "controls", "autoplay", "loop", "muted", "preload", "playsinline", "crossorigin"));
        addTag("audio", concat(common, events, "src", "controls", "autoplay", "loop", "muted", "preload", "crossorigin"));
        addTag("source", concat(common, "src", "type", "srcset", "media", "sizes"));
        addTag("track", concat(common, "src", "kind", "srclang", "label", "default"));
        addTag("map", concat(common, "name"));
        addTag("area", concat(common, "shape", "coords", "href", "alt", "target", "rel", "download", "ping", "referrerpolicy"));
        addTag("picture", common);
        addTag("embed", concat(common, "src", "type", "width", "height"));
        addTag("object", concat(common, "data", "type", "width", "height", "name", "form", "usemap"));
        addTag("param", concat(common, "name", "value"));
        addTag("template", common);
        addTag("slot", concat(common, "name"));
        addTag("ruby", common);
        addTag("rt", common);
        addTag("rp", common);
        addTag("bdi", common);
        addTag("bdo", concat(common, "dir"));
    }

    private void initAndroidLayoutTags() {
        addTag("androidx.constraintlayout.widget.ConstraintLayout", "android:layout_width", "android:layout_height", "android:background", "android:padding", "android:paddingStart", "android:paddingEnd", "android:paddingTop", "android:paddingBottom", "android:paddingLeft", "android:paddingRight", "android:clipChildren", "android:clipToPadding", "android:focusable", "android:focusableInTouchMode", "android:visibility", "android:alpha", "android:rotation", "android:scaleX", "android:scaleY", "android:translationX", "android:translationY", "android:elevation", "android:clickable", "android:foreground", "android:foregroundGravity", "android:minHeight", "android:minWidth", "android:tag");
        addTag("LinearLayout", "android:layout_width", "android:layout_height", "android:orientation", "android:gravity", "android:layout_gravity", "android:weightSum", "android:background", "android:padding", "android:baselineAligned", "android:divider", "android:showDividers", "android:measureWithLargestChild", "android:clipToPadding", "android:visibility", "android:alpha", "android:elevation", "android:clickable", "android:foreground", "android:minHeight", "android:minWidth", "android:tag", "android:focusable", "android:id");
        addTag("RelativeLayout", "android:layout_width", "android:layout_height", "android:gravity", "android:background", "android:padding", "android:clipChildren", "android:clipToPadding", "android:visibility", "android:alpha", "android:elevation", "android:clickable", "android:foreground", "android:minHeight", "android:minWidth", "android:tag", "android:focusable", "android:id");
        addTag("FrameLayout", "android:layout_width", "android:layout_height", "android:gravity", "android:background", "android:padding", "android:foreground", "android:foregroundGravity", "android:measureAllChildren", "android:clipToPadding", "android:visibility", "android:alpha", "android:elevation", "android:clickable", "android:minHeight", "android:minWidth", "android:tag", "android:focusable", "android:id");
        addTag("ScrollView", "android:layout_width", "android:layout_height", "android:fillViewport", "android:scrollbars", "android:scrollbarStyle", "android:scrollbarSize", "android:overScrollMode", "android:background", "android:padding", "android:clipToPadding", "android:visibility", "android:alpha", "android:elevation", "android:clickable", "android:minHeight", "android:minWidth", "android:tag", "android:focusable", "android:id");
        addTag("HorizontalScrollView", "android:layout_width", "android:layout_height", "android:fillViewport", "android:scrollbars", "android:scrollbarStyle", "android:overScrollMode", "android:background", "android:padding", "android:visibility", "android:alpha", "android:elevation", "android:clickable", "android:tag", "android:focusable", "android:id");
        addTag("NestedScrollView", "android:layout_width", "android:layout_height", "android:fillViewport", "android:scrollbars", "android:scrollbarStyle", "android:overScrollMode", "android:background", "android:padding", "android:clipToPadding", "android:visibility", "android:alpha", "android:elevation", "android:tag", "android:focusable", "android:id");
        addTag("androidx.coordinatorlayout.widget.CoordinatorLayout", "android:layout_width", "android:layout_height", "android:background", "android:padding", "android:clipChildren", "android:clipToPadding", "android:visibility", "android:alpha", "android:elevation", "android:tag", "android:focusable", "android:id");
        addTag("androidx.drawerlayout.widget.DrawerLayout", "android:layout_width", "android:layout_height", "android:background", "android:padding", "android:clipChildren", "android:clipToPadding", "android:visibility", "android:alpha", "android:elevation", "android:tag", "android:focusable", "android:id");
        addTag("GridView", "android:layout_width", "android:layout_height", "android:numColumns", "android:columnWidth", "android:horizontalSpacing", "android:verticalSpacing", "android:gravity", "android:stretchMode", "android:scrollbars", "android:background", "android:padding", "android:visibility", "android:alpha", "android:elevation", "android:id");
        addTag("ListView", "android:layout_width", "android:layout_height", "android:divider", "android:dividerHeight", "android:entries", "android:footerDividersEnabled", "android:headerDividersEnabled", "android:scrollbars", "android:choiceMode", "android:clickable", "android:background", "android:padding", "android:visibility", "android:alpha", "android:elevation", "android:id");
        addTag("RecyclerView", "android:layout_width", "android:layout_height", "android:scrollbars", "android:overScrollMode", "android:clipToPadding", "android:padding", "android:background", "android:visibility", "android:alpha", "android:elevation", "android:id", "android:tag");
        addTag("ViewPager", "android:layout_width", "android:layout_height", "android:background", "android:padding", "android:visibility", "android:alpha", "android:elevation", "android:id");

        addTag("TextView", "android:layout_width", "android:layout_height", "android:text", "android:textSize", "android:textColor", "android:textStyle", "android:textAlignment", "android:gravity", "android:typeface", "android:fontFamily", "android:lineSpacingExtra", "android:lineSpacingMultiplier", "android:maxLines", "android:minLines", "android:lines", "android:maxLength", "android:ellipsize", "android:singleLine", "android:inputType", "android:drawableLeft", "android:drawableRight", "android:drawableTop", "android:drawableBottom", "android:drawablePadding", "android:padding", "android:background", "android:clickable", "android:focusable", "android:visibility", "android:alpha", "android:elevation", "android:id", "android:tag", "android:autoLink", "android:linksClickable", "android:shadowColor", "android:shadowDx", "android:shadowDy", "android:shadowRadius", "android:letterSpacing", "android:includeFontPadding");
        addTag("EditText", "android:layout_width", "android:layout_height", "android:text", "android:hint", "android:textSize", "android:textColor", "android:textColorHint", "android:textStyle", "android:gravity", "android:typeface", "android:fontFamily", "android:maxLines", "android:minLines", "android:lines", "android:maxLength", "android:ellipsize", "android:singleLine", "android:inputType", "android:imeOptions", "android:digits", "android:phoneNumber", "android:password", "android:autofillHints", "android:background", "android:padding", "android:drawableLeft", "android:drawableRight", "android:drawablePadding", "android:selectAllOnFocus", "android:visibility", "android:alpha", "android:elevation", "android:id", "android:tag", "android:cursorVisible", "android:textCursorDrawable");
        addTag("Button", "android:layout_width", "android:layout_height", "android:text", "android:textSize", "android:textColor", "android:textAllCaps", "android:gravity", "android:background", "android:backgroundTint", "android:elevation", "android:padding", "android:clickable", "android:focusable", "android:visibility", "android:alpha", "android:id", "android:tag", "android:stateListAnimator", "android:shadowColor", "android:shadowDx", "android:shadowDy", "android:shadowRadius");
        addTag("ImageButton", "android:layout_width", "android:layout_height", "android:src", "android:scaleType", "android:padding", "android:background", "android:backgroundTint", "android:clickable", "android:focusable", "android:visibility", "android:alpha", "android:elevation", "android:id", "android:tag", "android:tint", "android:adjustViewBounds", "android:cropToPadding", "android:maxWidth", "android:maxHeight");
        addTag("ImageView", "android:layout_width", "android:layout_height", "android:src", "android:scaleType", "android:adjustViewBounds", "android:maxWidth", "android:maxHeight", "android:tint", "android:tintMode", "android:padding", "android:background", "android:clickable", "android:focusable", "android:visibility", "android:alpha", "android:elevation", "android:id", "android:tag", "android:cropToPadding", "android:baseline", "android:baselineAlignBottom");
        addTag("ProgressBar", "android:layout_width", "android:layout_height", "android:progress", "android:max", "android:indeterminate", "android:indeterminateOnly", "android:indeterminateDrawable", "android:progressDrawable", "android:progressTint", "android:background", "android:visibility", "android:alpha", "android:elevation", "android:id", "android:tag", "android:min", "android:secondaryProgress");
        addTag("SeekBar", "android:layout_width", "android:layout_height", "android:progress", "android:max", "android:thumb", "android:thumbTint", "android:progressDrawable", "android:progressTint", "android:splitTrack", "android:background", "android:visibility", "android:alpha", "android:elevation", "android:id", "android:tag", "android:min");
        addTag("Switch", "android:layout_width", "android:layout_height", "android:text", "android:textOn", "android:textOff", "android:checked", "android:thumb", "android:track", "android:showText", "android:switchMinWidth", "android:switchPadding", "android:background", "android:visibility", "android:alpha", "android:elevation", "android:id", "android:tag", "android:enabled");
        addTag("ToggleButton", "android:layout_width", "android:layout_height", "android:textOn", "android:textOff", "android:checked", "android:disabledAlpha", "android:background", "android:visibility", "android:alpha", "android:elevation", "android:id", "android:tag");
        addTag("CheckBox", "android:layout_width", "android:layout_height", "android:text", "android:checked", "android:button", "android:buttonTint", "android:gravity", "android:background", "android:visibility", "android:alpha", "android:elevation", "android:id", "android:tag", "android:enabled");
        addTag("RadioButton", "android:layout_width", "android:layout_height", "android:text", "android:checked", "android:button", "android:buttonTint", "android:gravity", "android:background", "android:visibility", "android:alpha", "android:elevation", "android:id", "android:tag");
        addTag("RadioGroup", "android:layout_width", "android:layout_height", "android:orientation", "android:checkedButton", "android:gravity", "android:background", "android:padding", "android:visibility", "android:alpha", "android:elevation", "android:id", "android:tag");
        addTag("Spinner", "android:layout_width", "android:layout_height", "android:entries", "android:prompt", "android:gravity", "android:background", "android:visibility", "android:alpha", "android:elevation", "android:id", "android:tag", "android:spinnerMode", "android:dropDownHorizontalOffset", "android:dropDownVerticalOffset", "android:dropDownWidth", "android:popupBackground");
        addTag("WebView", "android:layout_width", "android:layout_height", "android:background", "android:visibility", "android:alpha", "android:elevation", "android:id", "android:tag", "android:scrollbars", "android:scrollbarStyle");

        addTag("CardView", "android:layout_width", "android:layout_height", "app:cardCornerRadius", "app:cardElevation", "app:cardBackgroundColor", "app:cardMaxElevation", "app:cardUseCompatPadding", "app:cardPreventCornerOverlap", "android:contentPadding", "android:background", "android:visibility", "android:alpha", "android:elevation", "android:id", "android:tag", "android:clickable", "android:foreground");
        addTag("com.google.android.material.card.MaterialCardView", "android:layout_width", "android:layout_height", "app:cardCornerRadius", "app:cardElevation", "app:cardBackgroundColor", "app:strokeWidth", "app:strokeColor", "app:cardForegroundColor", "app:checkedIcon", "app:checkedIconTint", "android:clickable", "android:foreground", "android:background", "android:visibility", "android:alpha", "android:elevation", "android:id", "android:tag");
        addTag("com.google.android.material.button.MaterialButton", "android:layout_width", "android:layout_height", "android:text", "app:icon", "app:iconGravity", "app:iconPadding", "app:iconSize", "app:iconTint", "app:iconTintMode", "app:cornerRadius", "app:strokeWidth", "app:strokeColor", "app:backgroundTint", "app:rippleColor", "android:backgroundTint", "android:textColor", "android:textSize", "android:gravity", "android:padding", "android:elevation", "android:clickable", "android:visibility", "android:alpha", "android:id", "android:tag");
        addTag("com.google.android.material.textfield.TextInputLayout", "android:layout_width", "android:layout_height", "android:hint", "app:hintEnabled", "app:hintAnimationEnabled", "app:endIconMode", "app:startIconDrawable", "app:counterEnabled", "app:counterMaxLength", "app:passwordToggleEnabled", "app:boxBackgroundMode", "app:boxCornerRadiusTopStart", "app:boxCornerRadiusTopEnd", "app:boxCornerRadiusBottomStart", "app:boxCornerRadiusBottomEnd", "app:boxStrokeColor", "app:boxStrokeWidth", "app:errorEnabled", "app:helperText", "app:prefixText", "app:suffixText", "android:background", "android:visibility", "android:alpha", "android:id", "android:tag");
        addTag("com.google.android.material.textfield.TextInputEditText", "android:layout_width", "android:layout_height", "android:text", "android:hint", "android:inputType", "android:imeOptions", "android:maxLines", "android:background", "android:padding", "android:visibility", "android:alpha", "android:elevation", "android:id", "android:tag");
        addTag("com.google.android.material.floatingactionbutton.FloatingActionButton", "android:layout_width", "android:layout_height", "android:src", "app:fabSize", "app:backgroundTint", "app:rippleColor", "app:elevation", "app:hoveredFocusedTranslationZ", "app:pressedTranslationZ", "app:borderWidth", "app:maxImageSize", "android:clickable", "android:visibility", "android:alpha", "android:id", "android:tag");
        addTag("com.google.android.material.bottomnavigation.BottomNavigationView", "android:layout_width", "android:layout_height", "app:menu", "app:itemBackground", "app:itemIconTint", "app:itemTextColor", "app:itemIconSize", "app:labelVisibilityMode", "app:itemRippleColor", "android:background", "android:visibility", "android:alpha", "android:elevation", "android:id", "android:tag");
        addTag("com.google.android.material.bottomappbar.BottomAppBar", "android:layout_width", "android:layout_height", "app:fabCradleMargin", "app:fabCradleRoundedCornerRadius", "app:fabCradleVerticalOffset", "app:fabAlignmentMode", "app:fabAnimationMode", "android:menu", "android:background", "android:visibility", "android:alpha", "android:elevation", "android:id", "android:tag");
        addTag("com.google.android.material.appbar.AppBarLayout", "android:layout_width", "android:layout_height", "android:background", "android:elevation", "app:elevation", "app:liftOnScroll", "android:visibility", "android:alpha", "android:id", "android:tag", "android:fitsSystemWindows");
        addTag("com.google.android.material.appbar.MaterialToolbar", "android:layout_width", "android:layout_height", "android:background", "android:backgroundTint", "android:title", "android:subtitle", "android:logo", "android:navigationIcon", "android:menu", "app:titleTextColor", "app:subtitleTextColor", "app:titleCentered", "app:navigationContentDescription", "android:visibility", "android:alpha", "android:elevation", "android:id", "android:tag");
        addTag("Toolbar", "android:layout_width", "android:layout_height", "android:background", "android:backgroundTint", "android:title", "android:subtitle", "android:logo", "android:navigationIcon", "android:popupTheme", "android:menu", "android:visibility", "android:alpha", "android:elevation", "android:id", "android:tag");
        addTag("TabLayout", "android:layout_width", "android:layout_height", "app:tabMode", "app:tabGravity", "app:tabTextColor", "app:tabSelectedTextColor", "app:tabIndicatorColor", "app:tabIndicatorHeight", "app:tabBackground", "app:tabTextAppearance", "app:tabMaxWidth", "app:tabMinWidth", "app:tabPadding", "app:tabPaddingStart", "app:tabPaddingEnd", "app:tabRippleColor", "app:tabIndicator", "android:background", "android:visibility", "android:alpha", "android:elevation", "android:id", "android:tag");
        addTag("ViewPager2", "android:layout_width", "android:layout_height", "android:background", "android:visibility", "android:alpha", "android:id", "android:tag", "android:offscreenPageLimit");
        addTag("View", "android:layout_width", "android:layout_height", "android:background", "android:clickable", "android:focusable", "android:visibility", "android:alpha", "android:elevation", "android:id", "android:tag", "android:minHeight", "android:minWidth", "android:padding", "android:contentDescription", "android:nextFocusDown", "android:nextFocusLeft", "android:nextFocusRight", "android:nextFocusUp", "android:translationX", "android:translationY", "android:scaleX", "android:scaleY", "android:rotation", "android:transformPivotX", "android:transformPivotY");
        addTag("Space", "android:layout_width", "android:layout_height", "android:visibility", "android:id", "android:tag");
        addTag("Include", "layout", "android:layout_width", "android:layout_height", "android:id", "android:visibility", "android:alpha");

        addTag("fragment", "android:name", "android:id", "android:tag", "android:layout_width", "android:layout_height");
    }

    private void initAndroidManifestTags() {
        addTag("manifest", "xmlns:android", "package", "android:versionCode", "android:versionName", "android:installLocation", "android:sharedUserId", "android:sharedUserLabel", "android:compileSdkVersion", "android:compileSdkVersionCodename");
        addTag("application", "android:allowBackup", "android:allowClearUserData", "android:backupAgent", "android:icon", "android:label", "android:roundIcon", "android:logo", "android:name", "android:theme", "android:supportsRtl", "android:debuggable", "android:enabled", "android:exported", "android:hasCode", "android:hardwareAccelerated", "android:killAfterRestore", "android:largeHeap", "android:manageSpaceActivity", "android:multiArch", "android:permission", "android:persistent", "android:process", "android:restoreAnyVersion", "android:requiredAccountType", "android:resizeableActivity", "android:screenOrientation", "android:sizeConfigChanges", "android:taskAffinity", "android:uiOptions", "android:usesCleartextTraffic", "android:vmSafeMode", "android:networkSecurityConfig", "android:appComponentFactory", "android:zygotePreloadName", "android:textClassifier");
        addTag("activity", "android:name", "android:exported", "android:label", "android:theme", "android:launchMode", "android:configChanges", "android:windowSoftInputMode", "android:screenOrientation", "android:parentActivityName", "android:noHistory", "android:excludeFromRecents", "android:taskAffinity", "android:allowTaskReparenting", "android:alwaysRetainTaskState", "android:clearTaskOnLaunch", "android:finishOnTaskLaunch", "android:permission", "android:process", "android:stateNotNeeded", "android:supportsPictureInPicture", "android:resizeableActivity", "android:autoRemoveFromRecents", "android:enableOnBackInvokedCallback", "android:documentLaunchMode", "android:immersive", "android:maxRecents", "android:multiprocess", "android:relinquishTaskIdentity", "android:rotationAnimation", "android:showForAllUsers", "android:showOnLockScreen", "android:showWhenLocked", "android:turnScreenOn", "android:uiOptions", "android:windowSoftInputMode");
        addTag("service", "android:name", "android:exported", "android:enabled", "android:permission", "android:process", "android:foregroundServiceType", "android:stopWithTask", "android:isolatedProcess", "android:description", "android:directBootAware", "android:externalService", "android:grantUriPermissions", "android:exported");
        addTag("receiver", "android:name", "android:exported", "android:enabled", "android:permission", "android:process", "android:directBootAware", "android:description", "android:grantUriPermissions");
        addTag("provider", "android:name", "android:exported", "android:enabled", "android:authorities", "android:permission", "android:process", "android:readPermission", "android:writePermission", "android:grantUriPermissions", "android:multiprocess", "android:syncable", "android:directBootAware", "android:pathPermission", "android:initOrder");
        addTag("uses-permission", "android:name", "android:maxSdkVersion");
        addTag("uses-sdk", "android:minSdkVersion", "android:targetSdkVersion", "android:maxSdkVersion");
        addTag("uses-feature", "android:name", "android:required", "android:glEsVersion");
        addTag("intent-filter", "android:priority", "android:label", "android:icon", "android:autoVerify");
        addTag("action", "android:name");
        addTag("category", "android:name");
        addTag("data", "android:scheme", "android:host", "android:port", "android:path", "android:pathPrefix", "android:pathPattern", "android:mimeType", "android:ssp", "android:sspPrefix", "android:sspPattern");
        addTag("meta-data", "android:name", "android:value", "android:resource");
        addTag("instrumentation", "android:name", "android:targetPackage", "android:label", "android:icon", "android:functionalTest", "android:handleProfiling");
        addTag("permission", "android:name", "android:label", "android:description", "android:icon", "android:permissionGroup", "android:protectionLevel", "android:backgroundPermission", "android:foregroundServiceType");
        addTag("permission-group", "android:name", "android:label", "android:description", "android:icon", "android:priority");
        addTag("permission-tree", "android:name", "android:label", "android:icon");
        addTag("path-permission", "android:path", "android:pathPrefix", "android:pathPattern", "android:permission", "android:readPermission", "android:writePermission");
        addTag("grant-uri-permission", "android:path", "android:pathPrefix", "android:pathPattern");
        addTag("queries", "android:name");
        addTag("package", "android:name");
        addTag("compatible-screens", "android:name");
        addTag("screen", "android:screenSize", "android:screenDensity");
        addTag("supports-gl-texture", "android:name");
        addTag("supports-input", "android:name", "android:requiresNavigation", "android:requiresFiveWayNav", "android:requiresTrackball");
        addTag("supports-screens", "android:smallScreens", "android:normalScreens", "android:largeScreens", "android:xlargeScreens", "android:anyDensity", "android:requiresSmallestWidthDp", "android:compatibleWidthLimitDp", "android:largestWidthLimitDp");
        addTag("protection-level", "android:protectionLevel");
        addTag("feature-group", "android:name");
        addTag("configFor", "android:name");
    }

    private void initCommonAttributes() {
        addTag("resource", "android:name", "android:type", "android:id");
        addTag("color", "android:name");
        addTag("dimen", "android:name");
        addTag("string", "android:name", "android:translatable");
        addTag("string-array", "android:name", "android:translatable");
        addTag("integer", "android:name");
        addTag("integer-array", "android:name");
        addTag("bool", "android:name");
        addTag("drawable", "android:name");
        addTag("style", "android:name", "android:parent");
        addTag("item", "android:name", "android:value", "android:color", "android:dimen", "android:string", "android:drawable", "android:id");
        addTag("declare-styleable", "android:name");
        addTag("attr", "android:name", "android:format", "android:min", "android:max");
        addTag("enum", "android:name", "android:value");
        addTag("flag", "android:name", "android:value");
        addTag("selector", "xmlns:android");
        addTag("shape", "xmlns:android", "android:shape");
        addTag("gradient", "android:startColor", "android:endColor", "android:centerColor", "android:angle", "android:type", "android:centerX", "android:centerY", "android:gradientRadius", "android:useLevel");
        addTag("solid", "android:color");
        addTag("stroke", "android:width", "android:color", "android:dashWidth", "android:dashGap");
        addTag("corners", "android:radius", "android:topLeftRadius", "android:topRightRadius", "android:bottomLeftRadius", "android:bottomRightRadius");
        addTag("padding", "android:left", "android:top", "android:right", "android:bottom");
        addTag("size", "android:width", "android:height");
        addTag("layer-list", "xmlns:android");
        addTag("ripple", "xmlns:android", "android:color");
        addTag("animated-selector", "xmlns:android");
        addTag("transition", "android:fromId", "android:toId", "android:drawable");
        addTag("animated-vector", "xmlns:android", "android:drawable");
        addTag("target", "android:name", "android:animation");
        addTag("vector", "xmlns:android", "android:width", "android:height", "android:viewportWidth", "android:viewportHeight", "android:alpha", "android:tint");
        addTag("path", "android:name", "android:pathData", "android:fillColor", "android:strokeColor", "android:strokeWidth", "android:strokeAlpha", "android:fillAlpha", "android:strokeLineCap", "android:strokeLineJoin", "android:strokeMiterLimit", "android:trimPathStart", "android:trimPathEnd", "android:trimPathOffset");
        addTag("clip-path", "android:name", "android:pathData");
        addTag("group", "android:name", "android:translateX", "android:translateY", "android:scaleX", "android:scaleY", "android:rotation", "android:pivotX", "android:pivotY");
    }

    private void initAttributeValues() {
        addAttrValues("android:orientation", "vertical", "horizontal");
        addAttrValues("android:gravity", "top", "bottom", "left", "right", "center", "center_vertical", "center_horizontal", "start", "end", "fill", "fill_vertical", "fill_horizontal", "clip_vertical", "clip_horizontal");
        addAttrValues("android:layout_gravity", "top", "bottom", "left", "right", "center", "center_vertical", "center_horizontal", "start", "end", "fill", "fill_vertical", "fill_horizontal", "clip_vertical", "clip_horizontal");
        addAttrValues("android:visibility", "visible", "invisible", "gone");
        addAttrValues("android:scaleType", "fitXY", "fitStart", "fitCenter", "fitEnd", "center", "centerCrop", "centerInside", "matrix");
        addAttrValues("android:inputType", "text", "textCapCharacters", "textCapWords", "textCapSentences", "textAutoCorrect", "textAutoComplete", "textMultiLine", "textImeMultiLine", "textNoSuggestions", "textUri", "textEmailAddress", "textEmailSubject", "textShortMessage", "textLongMessage", "textPersonName", "textPostalAddress", "textPassword", "textVisiblePassword", "textWebEditText", "textFilter", "textPhonetic", "textWebEmailAddress", "textWebPassword", "number", "numberSigned", "numberDecimal", "numberPassword", "phone", "datetime", "date", "time");
        addAttrValues("android:imeOptions", "actionUnspecified", "actionNone", "actionGo", "actionSearch", "actionSend", "actionNext", "actionDone", "actionPrevious", "flagNoFullscreen", "flagNoExtractUi", "flagNoAccessoryAction", "flagNoPersonalizedLearning", "flagForceAscii");
        addAttrValues("android:ellipsize", "none", "start", "middle", "end", "marquee");
        addAttrValues("android:textStyle", "normal", "bold", "italic");
        addAttrValues("android:typeface", "normal", "sans", "serif", "monospace");
        addAttrValues("android:scrollbars", "none", "horizontal", "vertical");
        addAttrValues("android:choiceMode", "none", "singleChoice", "multipleChoice", "multipleChoiceModal");
        addAttrValues("android:stretchMode", "none", "spacingWidth", "columnWidth", "spacingWidthUniform");
        addAttrValues("android:shape", "rectangle", "oval", "line", "ring");
        addAttrValues("android:angle", "0", "90", "180", "270");
        addAttrValues("android:gradientType", "linear", "radial", "sweep");
        addAttrValues("android:strokeLineCap", "butt", "round", "square");
        addAttrValues("android:strokeLineJoin", "miter", "round", "bevel");
        addAttrValues("android:fillType", "nonZero", "evenOdd");
        addAttrValues("android:tintMode", "src_over", "src_in", "src_atop", "multiply", "screen", "add", "src", "add_overlay");
        addAttrValues("android:adjustViewBounds", "true", "false");
        addAttrValues("android:clickable", "true", "false");
        addAttrValues("android:focusable", "true", "false");
        addAttrValues("android:focusableInTouchMode", "true", "false");
        addAttrValues("android:enabled", "true", "false");
        addAttrValues("android:checked", "true", "false");
        addAttrValues("android:selected", "true", "false");
        addAttrValues("android:indeterminate", "true", "false");
        addAttrValues("android:singleLine", "true", "false");
        addAttrValues("android:selectAllOnFocus", "true", "false");
        addAttrValues("android:lines", "1", "2", "3", "4", "5");
        addAttrValues("android:maxLines", "1", "2", "3", "4", "5", "10");
        addAttrValues("android:exported", "true", "false");
        addAttrValues("android:enabled", "true", "false");
        addAttrValues("android:supportsRtl", "true", "false");
        addAttrValues("android:allowBackup", "true", "false");
        addAttrValues("android:debuggable", "true", "false");
        addAttrValues("android:launchMode", "standard", "singleTop", "singleTask", "singleInstance", "singleInstancePerTask");
        addAttrValues("android:screenOrientation", "portrait", "landscape", "unspecified", "behind", "sensor", "nosensor", "user", "fullSensor", "userLandscape", "userPortrait", "locked", "sensorLandscape", "sensorPortrait", "reverseLandscape", "reversePortrait", "fullUser");
        addAttrValues("android:windowSoftInputMode", "stateUnspecified", "stateUnchanged", "stateHidden", "stateAlwaysHidden", "stateVisible", "stateAlwaysVisible", "adjustUnspecified", "adjustResize", "adjustPan", "adjustNothing");
        addAttrValues("android:configChanges", "orientation", "keyboardHidden", "screenSize", "smallestScreenSize", "screenLayout", "fontScale", "locale", "layoutDirection", "mcc", "mnc", "navigation", "keyboard", "touchscreen", "uiMode", "colorMode", "density");
        addAttrValues("android:protectionLevel", "normal", "dangerous", "signature", "signatureOrSystem", "internal", "development", "instant", "privileged", "appop", "pre23", "installer", "verifier", "preinstalled", "setup", "instant", "runtime", "retailDemo", "textClassifier");
        addAttrValues("android:foregroundServiceType", "connectedDevice", "dataSync", "location", "mediaPlayback", "mediaProjection", "phoneCall", "camera", "microphone", "health", "remoteMessaging", "systemExempted", "shortService", "specialUse");
        addAttrValues("app:fabSize", "auto", "normal", "mini");
        addAttrValues("app:tabMode", "fixed", "scrollable", "auto");
        addAttrValues("app:tabGravity", "fill", "center", "start");
        addAttrValues("app:labelVisibilityMode", "auto", "labeled", "unlabeled", "selected", "unlabeled");
        addAttrValues("app:boxBackgroundMode", "none", "filled", "outline");
        addAttrValues("app:endIconMode", "none", "password_toggle", "clear_text", "dropdown_menu", "custom");
        addAttrValues("app:cardUseCompatPadding", "true", "false");
        addAttrValues("app:cardPreventCornerOverlap", "true", "false");
    }

    private void buildIndex() {
        tagNameList.addAll(tagNames);
    }

    public List<String> getTagNames() {
        return tagNameList;
    }

    public List<String> getTagCompletions(String prefix) {
        return filterByPrefix(tagNames, prefix);
    }

    public List<String> getAttributeCompletions(String tagName, String prefix) {
        List<String> attrs = tagAttributes.get(tagName);
        if (attrs != null) {
            return filterByPrefix(new LinkedHashSet<>(attrs), prefix);
        }
        return filterByPrefix(allAttributes, prefix);
    }

    public List<String> getAttributeValueCompletions(String attrName, String prefix) {
        List<String> values = attrValues.get(attrName);
        if (values != null) {
            return filterByPrefix(new LinkedHashSet<>(values), prefix);
        }
        return new ArrayList<>();
    }

    private List<String> filterByPrefix(Set<String> items, String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return new ArrayList<>(items);
        }
        String lowerPrefix = prefix.toLowerCase();
        List<String> result = new ArrayList<>();
        for (String item : items) {
            if (item.toLowerCase().startsWith(lowerPrefix) || item.toLowerCase().contains(lowerPrefix)) {
                result.add(item);
            }
        }
        return result;
    }

    private static String[] concat(String[] a, String... b) {
        String[] result = Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    private static String[] concat(String[] a, String[] b, String... c) {
        String[] result = Arrays.copyOf(a, a.length + b.length + c.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        System.arraycopy(c, 0, result, a.length + b.length, c.length);
        return result;
    }
}
