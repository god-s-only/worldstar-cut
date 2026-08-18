# ProGuard rules for WorldstarCut
# Keep Room entities
-keep class com.worldstar.cut.features.video_editor.data.local.db.ProjectEntity { *; }
-keep class com.worldstar.cut.features.video_editor.data.local.db.TrackEntity { *; }
-keep class com.worldstar.cut.features.video_editor.data.local.db.ClipEntity { *; }
-keep class com.worldstar.cut.features.export.data.local.db.ExportHistoryEntity { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# FFmpeg
-keep class com.arthenica.ffmpegkit.** { *; }

# Media3
-keep class androidx.media3.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
