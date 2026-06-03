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
# OBFUSCATION
# ==========================================
-repackageclasses ''
-overloadaggressively
-dontusemixedcaseclassnames

# ==========================================
# ANDROID COMPONENTS — class + ctor only
# ==========================================
-keep public class * extends android.app.Activity { public <init>(...); }
-keep public class * extends android.app.Application { public <init>(...); }
-keep public class * extends android.app.Service { public <init>(...); }
-keep public class * extends android.content.BroadcastReceiver { public <init>(...); }
-keep public class * extends android.content.ContentProvider { public <init>(...); }

# ==========================================
# APP ENTRY POINTS
# ==========================================
-keep class bin.mg.main.MainActivity { *; }
-keep class bin.mg.main.AppMain { *; }
-keep class bin.mg.main.file.TextEditorActivity { *; }
-keep class bin.mg.main.file.EditorPreferencesFragment { *; }
-keep class bin.mg.main.file.TextEditorPreferencesFragment { *; }
-keep class bin.mg.main.file.SyntaxSelectorFragment { *; }
-keep class bin.mg.main.app.dex.plus.DexActivity { *; }

# ==========================================
# VIEWS from XML
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
# JNI
# ==========================================
-keepclasseswithmembernames class * {
    native <methods>;
}

# ==========================================
# SORA EDITOR — class names only, let members rename
# ==========================================
-keep public class io.github.rosemoe.sora.widget.CodeEditor { public *; }
-keep public class io.github.rosemoe.sora.widget.SymbolInputView { public *; }
-keep class io.github.rosemoe.sora.langs.textmate.** { *; }
-keep class io.github.rosemoe.sora.langs.java.** { *; }
-keep class io.github.rosemoe.sora.lang.EmptyLanguage { *; }
-keep class io.github.rosemoe.sora.widget.schemes.EditorColorScheme { *; }
-keep class io.github.rosemoe.sora.widget.component.EditorTextActionWindow { *; }
-keep class io.github.rosemoe.sora.event.** { *; }
-dontwarn io.github.rosemoe.sora.**

# ==========================================
# TM4E — TextMate engine internals
# ==========================================
-keep class org.eclipse.tm4e.core.** { *; }
-keep class org.eclipse.tm4e.theme.** { *; }
-keep class org.eclipse.tm4e.registry.** { *; }
-dontwarn org.eclipse.tm4e.**

# ==========================================
# DEXLIB2 — only keep public API
# ==========================================
-keep public class org.jf.dexlib2.DexFileFactory { public *; }
-keep public class org.jf.dexlib2.writer.pool.DexPool { public *; }
-keep public class org.jf.dexlib2.builder.DexBuilder { public *; }
-dontwarn org.jf.dexlib2.**

# ==========================================
# SUPPRESS WARNINGS
# ==========================================
-dontwarn **
-ignorewarnings
