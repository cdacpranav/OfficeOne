# ProGuard rules for Office One

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Apache POI
-dontwarn org.apache.poi.**
-dontwarn org.apache.commons.collections4.**
-dontwarn org.apache.commons.compress.**
-dontwarn org.apache.commons.math3.**
-dontwarn javax.xml.stream.**
-keep class org.apache.poi.** { *; }
-keep interface org.apache.poi.** { *; }

# PDFBox Android (Tom Roush)
-dontwarn com.tom_roush.pdfbox.**
-keep class com.tom_roush.pdfbox.** { *; }
-keep interface com.tom_roush.pdfbox.** { *; }
-dontwarn org.bouncycastle.**
-keep class org.bouncycastle.** { *; }

# Coil
-dontwarn coil.**
-keep class coil.** { *; }

# Compose and Material 3
-keep class androidx.compose.material3.** { *; }

# Retain generic signatures and annotations for reflection
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Suppress harmless warnings from java.* packages accessed via POI/PDFBox
-dontwarn java.awt.**
-dontwarn javax.imageio.**
-dontwarn javax.naming.**
-dontwarn java.beans.**
