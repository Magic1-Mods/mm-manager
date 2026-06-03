# ==========================================
# R8 FULL MODE — Maximum shrinking + renaming
# ==========================================
-allowaccessmodification
-optimizationpasses 5
-optimizations !code/simplification/arithmetic,!code/simplification/cast

-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}

# ==========================================
# OBFUSCATION — max obfuscation
# ==========================================
-repackageclasses ''
-overloadaggressively
-dontusemixedcaseclassnames

# ==========================================
# ANDROID COMPONENTS — keep class name only
# ==========================================
-keep public class * extends android.app.Activity {
    public <init>(...);
}
-keep public class * extends android.app.Application {
    public <init>(...);
}
-keep public class * extends android.app.Service {
    public <init>(...);
}
-keep public class * extends android.content.BroadcastReceiver {
    public <init>(...);
}
-keep public class * extends android.content.ContentProvider {
    public <init>(...);
}

# ==========================================
# APP ENTRY POINTS — keep class + members
# ==========================================
-keep class bin.mg.main.MainActivity { *; }
-keep class bin.mg.main.AppMain { *; }
-keep class bin.mg.main.file.TextEditorActivity { *; }
-keep class bin.mg.main.file.EditorPreferencesFragment { *; }
-keep class bin.mg.main.file.TextEditorPreferencesFragment { *; }
-keep class bin.mg.main.file.SyntaxSelectorFragment { *; }
-keep class bin.mg.main.app.dex.plus.DexActivity { *; }

# ==========================================
# VIEWS inflated from XML (reflection)
# ==========================================
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
-keep class bin.mg.main.PullToRefreshLayout { *; }

# ==========================================
# ENUMS + PARCELABLE + SERIALIZABLE
# ==========================================
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
-keepclassmembers class * implements java.io.Serializable {
    <fields>;
}

# ==========================================
# JNI / NATIVE
# ==========================================
-keepclasseswithmembernames class * {
    native <methods>;
}

# ==========================================
# SORA EDITOR — minimal public API
# ==========================================
-keep public class io.github.rosemoe.sora.widget.CodeEditor { public *; }
-keep public class io.github.rosemoe.sora.widget.SymbolInputView { public *; }
-keep public class io.github.rosemoe.sora.langs.textmate.TextMateLanguage { public *; }
-keep public class io.github.rosemoe.sora.langs.textmate.TextMateColorScheme { public *; }
-keep public class io.github.rosemoe.sora.langs.textmate.registry.** { public *; }
-keep public class io.github.rosemoe.sora.langs.textmate.registry.model.** { public *; }
-keep public class io.github.rosemoe.sora.langs.java.JavaLanguage { public *; }
-keep public class io.github.rosemoe.sora.lang.EmptyLanguage { public *; }
-keep public class io.github.rosemoe.sora.widget.schemes.EditorColorScheme { public *; }
-keep public class io.github.rosemoe.sora.widget.component.EditorTextActionWindow { *; }
-keep public class io.github.rosemoe.sora.event.** { public *; }
-dontwarn io.github.rosemoe.sora.**

# ==========================================
# TM4E — TextMate grammar engine
# ==========================================
-keep class org.eclipse.tm4e.core.registry.** { public *; }
-keep class org.eclipse.tm4e.core.internal.** { *; }
-keep class org.eclipse.tm4e.theme.api.** { public *; }
-dontwarn org.eclipse.tm4e.**

# ==========================================
# DEXLIB2 — vendored source, R8 analyzes it
# Only keep absolute minimum entry points
# ==========================================
-keep class org.jf.dexlib2.DexFileFactory { *; }
-keep class org.jf.dexlib2.writer.pool.DexPool { public *; }
-keep class org.jf.dexlib2.builder.DexBuilder { public *; }
-dontwarn org.jf.dexlib2.**

# ==========================================
# GUAVA — only what dexlib2 actually uses
# ==========================================
-dontwarn com.google.common.**
-dontwarn javax.annotation.**
-dontwarn javax.lang.model.**

# ==========================================
# GSON — annotation-based only
# ==========================================
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-dontwarn com.google.gson.**

# ==========================================
# GLIDE — minimal
# ==========================================
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-dontwarn com.bumptech.glide.**

# ==========================================
# SUPPRESS WARNINGS
# ==========================================
-dontwarn **
-ignorewarnings
