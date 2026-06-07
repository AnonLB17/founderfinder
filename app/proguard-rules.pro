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
# Navigation
# =============================================
-keep,allowobfuscation,allowshrinking class * extends androidx.navigation.Navigator { *; }

# =============================================
# Hilt / Dagger
# =============================================
-keep,allowobfuscation,allowshrinking @dagger.hilt.EntryPoint class * { *; }
-keep,allowobfuscation,allowshrinking @dagger.hilt.android.EarlyEntryPoint class * { *; }
-keep,allowobfuscation,allowshrinking @dagger.hilt.internal.ComponentEntryPoint class * { *; }
-keep,allowobfuscation,allowshrinking @dagger.hilt.internal.GeneratedEntryPoint class * { *; }

# =============================================
# Firebase
# =============================================
-keep class * implements com.google.firebase.components.ComponentRegistrar { *; }
-keep,allowshrinking interface com.google.firebase.components.ComponentRegistrar { *; }
-dontwarn com.google.firebase.**

# =============================================
# Coil (Image Loading)
# =============================================
-keep class coil.** { *; }
-dontwarn coil.*

# =============================================
# Jetpack Compose
# =============================================
-keep,allowshrinking class * extends androidx.compose.ui.node.ModifierNodeElement { *; }

# =============================================
# Google Play Services & Parcelables
# =============================================
-keep class com.google.android.gms.common.internal.ReflectedParcelable { *; }
-keep,allowshrinking class * implements com.google.android.gms.common.internal.ReflectedParcelable { *; }
-keep,allowshrinking class * implements androidx.versionedparcelable.VersionedParcelable { *; }
-keep public class androidx.versionedparcelable.ParcelImpl { *; }
-keep,allowshrinking class * extends androidx.startup.Initializer { *; }

# =============================================
# Kotlin Serialization
# =============================================
-keepattributes Annotation, InnerClasses
-dontnote kotlinx.serialization.SerializationStrategy
-dontnote kotlinx.serialization.DeserializationStrategy

# =============================================
# General Recommendations
# =============================================
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile