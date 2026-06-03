# ==========================================
# R8 FULL MODE — Aggressive shrinking
# ==========================================
-allowaccessmodification
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
-flattenpackagehierarchy ''
-overloadaggressively
-useuniqueclassmembernames

# ==========================================
# ANDROID COMPONENTS (keep minimal)
# ==========================================
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# ==========================================
# APP-SPECIFIC — only what反射 needs
# ==========================================
-keep class bin.mg.main.MainActivity { *; }
-keep class bin.mg.main.AppMain { *; }
-keep class bin.mg.main.file.TextEditorActivity { *; }
-keep class bin.mg.main.file.EditorPreferencesFragment { *; }
-keep class bin.mg.main.file.TextEditorPreferencesFragment { *; }
-keep class bin.mg.main.file.SyntaxSelectorFragment { *; }
-keep class bin.mg.main.app.dex.plus.DexActivity { *; }

# ==========================================
# VIEWS inflated from XML
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
# SORA EDITOR — keep only public API
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
# TM4E (TextMate grammar engine)
# ==========================================
-keep class org.eclipse.tm4e.core.registry.** { public *; }
-keep class org.eclipse.tm4e.core.internal.** { *; }
-keep class org.eclipse.tm4e.theme.api.** { public *; }
-dontwarn org.eclipse.tm4e.**

# ==========================================
# DEXLIB2 — keep only what JNI/reflection needs
# ==========================================
-keep class org.jf.dexlib2.** { *; }
-keep class org.jf.util.** { *; }
-dontwarn org.jf.dexlib2.**
-dontwarn org.jf.util.**

# ==========================================
# GUAVA — strip unused aggressively
# ==========================================
-keep class com.google.common.base.** { *; }
-keep class com.google.common.collect.** { *; }
-dontwarn com.google.common.**
-dontwarn javax.annotation.**
-dontwarn javax.lang.model.**

# ==========================================
# GSON — keep only what serialization needs
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
# ANDROIDX — let consumer rules do their job
# Don't keep entire packages, only suppress warnings
# ==========================================
-dontwarn androidx.**
-dontwarn com.google.android.material.**
