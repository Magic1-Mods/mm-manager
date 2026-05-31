# ==========================================
# 🔥 GLOBAL SETTINGS
# ==========================================

-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# Remove logs
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# ==========================================
# 🔥 DICTIONARY (UNICODE OBFUSCATION)
# ==========================================

-obfuscationdictionary proguard-dictionary.txt
-classobfuscationdictionary proguard-dictionary.txt
-packageobfuscationdictionary proguard-dictionary.txt

# Force usage of dictionary
-dontusemixedcaseclassnames

# ==========================================
# 🔥 AGGRESSIVE OBFUSCATION
# ==========================================

-useuniqueclassmembernames
-overloadaggressively
-repackageclasses ''
-flattenpackagehierarchy ''

# ==========================================
# 🔥 OPTIMIZATION
# ==========================================

-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification

# ==========================================
# 🔥 KEEP ONLY REQUIRED ENTRY POINTS
# ==========================================

# MainActivity (keep but allow obfuscation inside)
-keep class com.android.support.MainActivity {
    <init>();
}

# Android components (keep constructors only)
-keep class * extends android.app.Activity {
    <init>();
}
-keep class * extends android.app.Service {
    <init>();
}
-keep class * extends android.content.BroadcastReceiver {
    <init>();
}
-keep class * extends android.content.ContentProvider {
    <init>();
}

# ==========================================
# 🔥 REQUIRED ANDROID / UI CLASSES
# ==========================================

-keep class android.os.Handler { *; }

# RecyclerView essentials
-keep class androidx.recyclerview.widget.RecyclerView { *; }
-keep class androidx.recyclerview.widget.LinearLayoutManager { *; }

# Your adapter
-keep class com.android.support.SoAdapter { *; }

# AppCompat + Material (needed)
-keep class androidx.appcompat.** { *; }
-keep class com.google.android.material.** { *; }

# Core Android usage
-keep class android.view.** { *; }
-keep class android.widget.** { *; }
-keep class android.content.Context { *; }

# File handling
-keep class java.io.** { *; }

# ==========================================
# 🔥 STRINGFOG SUPPORT
# ==========================================

-keep class com.github.megatronking.stringfog.** { *; }
-keep class * implements com.github.megatronking.stringfog.IStringFog { *; }
-keep class com.github.megatronking.stringfog.xor.StringFogImpl { *; }

# ==========================================
# 🔥 JNI / SERIALIZATION SAFETY
# ==========================================

# Native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ==========================================
# 🔥 CLEANUP / AGGRESSIVE SHRINKING
# ==========================================

-dontwarn **
-ignorewarnings