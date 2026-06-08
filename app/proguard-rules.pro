# =============================================
# Google Play Licensing
# =============================================
-keep public class com.google.android.vending.licensing.ILicensingService { *; }

# =============================================
# Keep Annotations
# =============================================
-keepattributes *Annotation*
-keep class android.support.annotation.Keep
-keep @interface androidx.annotation.Keep { *; }
-keep @androidx.annotation.Keep class * { *; }
-keep @interface com.google.android.gms.common.annotation.KeepName { *; }
-keep @com.google.android.gms.common.annotation.KeepName class * { *; }
-keep @interface com.google.android.gms.common.util.DynamiteApi { *; }

# =============================================
# YOUR APP MODELS (Critical for Firestore)
# =============================================
-keep class com.phoenixcorp.founderfinder.domain.model.** { *; }
-keepclassmembers class com.phoenixcorp.founderfinder.domain.model.** {
    <fields>;
    <init>(...);
    public <init>(...);
}

# Keep Firestore annotations and property names
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.DocumentId <fields>;
}

# =============================================
# Firebase / Firestore
# =============================================
-keep class * implements com.google.firebase.components.ComponentRegistrar { *; }
-keep,allowshrinking interface com.google.firebase.components.ComponentRegistrar { *; }
-keep class com.google.firebase.firestore.** { *; }
-dontwarn com.google.firebase.**

# =============================================
# Hilt / Dagger
# =============================================
-keep,allowobfuscation,allowshrinking @dagger.hilt.EntryPoint class * { *; }
-keep,allowobfuscation,allowshrinking @dagger.hilt.android.EarlyEntryPoint class * { *; }
-keep,allowobfuscation,allowshrinking @dagger.hilt.internal.ComponentEntryPoint class * { *; }
-keep,allowobfuscation,allowshrinking @dagger.hilt.internal.GeneratedEntryPoint class * { *; }
-keep class com.phoenixcorp.founderfinder.**_Hilt* { *; }

# =============================================
# Navigation
# =============================================
-keep,allowobfuscation,allowshrinking class * extends androidx.navigation.Navigator { *; }

# =============================================
# Jetpack Compose
# =============================================
-keep,allowshrinking class * extends androidx.compose.ui.node.ModifierNodeElement { *; }

# =============================================
# Coil (Image Loading)
# =============================================
-keep class coil.** { *; }
-dontwarn coil.*

# =============================================
# Google Play Services & Parcelables
# =============================================
-keep class com.google.android.gms.common.internal.ReflectedParcelable { *; }
-keep,allowshrinking class * implements com.google.android.gms.common.internal.ReflectedParcelable { *; }
-keep,allowshrinking class * implements androidx.versionedparcelable.VersionedParcelable { *; }
-keep public class androidx.versionedparcelable.ParcelImpl { *; }
-keep,allowshrinking class * extends androidx.startup.Initializer { *; }

# =============================================
# Kotlin Serialization / Coroutines
# =============================================
-keepattributes Annotation, InnerClasses
-keep class kotlin.Metadata { *; }
-dontnote kotlinx.serialization.SerializationStrategy
-dontnote kotlinx.serialization.DeserializationStrategy

# =============================================
# General
# =============================================
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Optional: Enable this temporarily to debug what's being removed
#-printmapping mapping.txt