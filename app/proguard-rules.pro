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
# ANDROID COMPONENTS
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
# CUSTOM EDITOR MODULES
# ==========================================
-keep class bin.mg.editor.core.** { *; }
-keep class bin.mg.editor.rendering.** { *; }
-keep class bin.mg.editor.syntax.** { *; }

# ==========================================
# TM4E — TextMate grammar engine
# ==========================================
-keep public class org.eclipse.tm4e.core.registry.IGrammarSource { public *; }
-keep public class org.eclipse.tm4e.core.registry.IThemeSource { public *; }
-keep public class org.eclipse.tm4e.core.registry.Registry { public *; }
-dontwarn org.eclipse.tm4e.**

# ==========================================
# DEXLIB2 — vendored source, minimal entry points
# ==========================================
-keep public class org.jf.dexlib2.DexFileFactory { public *; }
-keep public class org.jf.dexlib2.writer.pool.DexPool { public *; }
-keep public class org.jf.dexlib2.builder.DexBuilder { public *; }
-dontwarn org.jf.dexlib2.**
-dontwarn com.google.common.**
-dontwarn javax.annotation.**
-dontwarn javax.lang.model.**
