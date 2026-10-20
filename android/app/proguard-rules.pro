# Dependencies supply their own consumer rules, including Filament JNI.
# Add narrowly scoped rules here only for verified reflection entry points
# and AndroidX / image-loading symbols checked by Play Console static analysis.
-keep class androidx.activity.EdgeToEdge** { *; }
-keep class androidx.activity.SystemBarStyle** { *; }
-keepnames class androidx.core.** { *; }
-keepnames class androidx.media3.** { *; }
-keepnames class coil.** { *; }
-keepnames class okhttp3.** { *; }
-keepnames class okio.** { *; }
