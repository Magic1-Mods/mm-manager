# ==========================================
# SOURCE LINE NUMBERS (for stack traces)
# ==========================================

-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# ==========================================
# REMOVE LOG CALLS
# ==========================================

-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
}

# ==========================================
# OBFUSCATION DICTIONARY
# ==========================================

-obfuscationdictionary       proguard-dictionary.txt
-classobfuscationdictionary  proguard-dictionary.txt
-packageobfuscationdictionary proguard-dictionary.txt

-dontusemixedcaseclassnames

# ==========================================
# AGGRESSIVE OBFUSCATION
# ==========================================

-useuniqueclassmembernames
-overloadaggressively
-repackageclasses ''
-flattenpackagehierarchy ''

# ==========================================
# OPTIMIZATION
# ==========================================

-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification

# ==========================================
# KOTLIN METADATA + COROUTINES
# ==========================================

-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { <fields>; }
-keep class kotlin.coroutines.Continuation
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**
-dontwarn kotlin.reflect.jvm.internal.**

# ==========================================
# ANDROID ENTRY POINTS (keep constructors + class)
# ==========================================

-keep public class * extends android.app.Application
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends androidx.appcompat.app.AppCompatActivity
-keep public class * extends androidx.fragment.app.Fragment
-keep public class * extends android.preference.Preference

# Our app's main activities/services stay at their FQCN
-keep class bin.mg.main.MainActivity { *; }
-keep class bin.mg.main.AppMain { *; }
-keep class bin.mg.main.app.dex.plus.DexActivity { *; }
-keep class bin.mg.main.ui.editor.FileEditorActivity { *; }
-keep class bin.mg.main.ui.editor.EditorPreferencesFragment { *; }
-keep class bin.mg.main.ui.editor.TextEditorPreferencesFragment { *; }
-keep class bin.mg.main.ui.editor.SyntaxSelectorFragment { *; }

# ==========================================
# VIEWS REFERENCED FROM XML
# (Activity#setContentView inflates by reflection)
# ==========================================

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
-keepclasseswithmembers class * extends android.view.View {
    public <init>(android.content.Context);
}

# Custom views in our project
-keep class bin.mg.main.PullToRefreshLayout { *; }
-keep class bin.mg.main.ui.view.ViewHolder { *; }
-keep class bin.mg.main.app.dex.plus.clickeffect.T_a { *; }

# ==========================================
# ENUMS (values()/valueOf used by reflection)
# ==========================================

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ==========================================
# PARCELABLE
# ==========================================

-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ==========================================
# SERIALIZABLE (Intent extras, prefs)
# ==========================================

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    <fields>;
}

# ==========================================
# JNI / NATIVE METHODS
# Keep class + native method names AND keep
# the class in its original package path
# (no repackaging of native holders)
# ==========================================

-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# Keep the original FQCN of any class that has native methods.
# -keep on the class itself prevents -repackageclasses ''
# from moving it, which would break JNI symbol resolution.
-keep class * {
    native <methods>;
}

# ==========================================
# ANNOTATIONS + SIGNED CODE
# ==========================================

-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes Exceptions
-keepattributes Deprecated

# Keep classes referenced by annotations
-keep,allowobfuscation @interface * { *; }

# ==========================================
# ANDROIDX / MATERIAL
# ==========================================

-keep class androidx.lifecycle.** { *; }
-keep class androidx.core.** { *; }
-keep class androidx.appcompat.** { *; }
-keep class com.google.android.material.** { *; }
-keep class androidx.recyclerview.widget.** { *; }
-keep class androidx.viewpager.widget.** { *; }
-keep class androidx.drawerlayout.widget.** { *; }
-keep class androidx.cardview.widget.** { *; }
-keep class androidx.preference.** { *; }
-keep class androidx.documentfile.** { *; }
-keep class androidx.swiperefreshlayout.** { *; }
-keep class androidx.constraintlayout.** { *; }
-keep class androidx.interpolator.** { *; }
-keep class androidx.startup.** { *; }
-dontwarn androidx.**

# ==========================================
# SORA EDITOR (text editor library)
# ==========================================

-keep class io.github.rosemoe.sora.** { *; }
-keep class org.eclipse.tm4e.** { *; }
-dontwarn io.github.rosemoe.sora.**
-dontwarn org.eclipse.tm4e.**

# ==========================================
# DEX LIBRARY (org.jf.dexlib2 — heavy reflection)
# ==========================================

-keep class org.jf.dexlib2.** { *; }
-keep class org.jf.util.** { *; }
-keep class com.google.common.** { *; }
-keep class javax.annotation.** { *; }
-keep class com.google.code.findbugs.** { *; }
-dontwarn org.jf.dexlib2.**
-dontwarn org.jf.util.**
-dontwarn com.google.common.**
-dontwarn javax.annotation.**

# ==========================================
# GLIDE (image loader used elsewhere)
# ==========================================

-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep class com.bumptech.glide.GeneratedAppGlideModuleImpl
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** { **[] $VALUES; public *; }
-dontwarn com.bumptech.glide.**

# ==========================================
# FAST SCROLLER (custom RecyclerView)
# ==========================================

-keep class com.fastrecyclerview.** { *; }
-dontwarn com.fastrecyclerview.**

# ==========================================
# GSON (used by theme/serialization)
# ==========================================

-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-dontwarn com.google.gson.**

# ==========================================
# THIRD-PARTY
# ==========================================

-keep class com.github.angads25.filepicker.** { *; }
-dontwarn com.github.angads25.filepicker.**

# ==========================================
# SUPPRESS WARNINGS
# ==========================================

-dontwarn **
-ignorewarnings
