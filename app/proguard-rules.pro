# Google Tink / ErrorProne compile-only annotations
-dontwarn com.google.errorprone.annotations.**

# Osmdroid
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# Room
-keep class androidx.room.** { *; }
-keep class com.moovar.android.core.database.entity.** { *; }
-dontwarn androidx.room.**

# Retrofit
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Kotlinx Serialization
-keepattributes *Annotation*, Signature, InnerClasses
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# SOFSE API Interfaces
-keep interface com.moovar.android.core.network.sofse.api.** { *; }
