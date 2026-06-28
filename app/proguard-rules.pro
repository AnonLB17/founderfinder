# =============================================
# Google Play Licensing & Basics
# =============================================
-keep public class com.google.android.vending.licensing.ILicensingService { *; }

# =============================================
# Keep Annotations & Metadata
# =============================================
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep class android.support.annotation.Keep
-keep @interface androidx.annotation.Keep { *; }
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.DocumentId <fields>;
}

# =============================================
# YOUR DOMAIN MODELS (Most Critical)
# =============================================
-keep class com.phoenixcorp.founderfinder.domain.model.** { *; }
-keepclassmembers class com.phoenixcorp.founderfinder.domain.model.** { *; }
-keepnames class com.phoenixcorp.founderfinder.domain.model.**

# Extra strong protection for Firestore serialization
-keepclassmembers class * extends com.phoenixcorp.founderfinder.domain.model.** { *; }
-keepclassmembers class * implements com.phoenixcorp.founderfinder.domain.model.** { *; }

# =============================================
# Firebase / Firestore
# =============================================
-keep class com.google.firebase.** { *; }
-keepclassmembers class com.google.firebase.** { *; }
-keep class com.google.firebase.firestore.** { *; }
-keepclassmembers class com.google.firebase.firestore.** { *; }
-dontwarn com.google.firebase.**

# =============================================
# Hilt / Dagger
# =============================================
-keep,allowobfuscation,allowshrinking @dagger.hilt.EntryPoint class * { *; }
-keep,allowobfuscation,allowshrinking @dagger.hilt.android.EarlyEntryPoint class * { *; }
-keep class com.phoenixcorp.founderfinder.**_Hilt* { *; }
-keep class dagger.hilt.** { *; }

# =============================================
# Jetpack Compose & Navigation
# =============================================
-keep,allowshrinking class * extends androidx.compose.ui.node.ModifierNodeElement { *; }
-keep,allowobfuscation,allowshrinking class * extends androidx.navigation.Navigator { *; }

# =============================================
# Coil Image Loading
# =============================================
-keep class coil.** { *; }
-dontwarn coil.**

# =============================================
# Parcelable & Google Play Services
# =============================================
-keep class * implements android.os.Parcelable { *; }
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}
-keep class com.google.android.gms.common.internal.ReflectedParcelable { *; }

# =============================================
# Kotlin & Coroutines
# =============================================
-keepattributes Annotation, InnerClasses
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlinx.coroutines.** { *; }

# =============================================
# General
# =============================================
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# =============================================
# FCM & Notification Sending
# =============================================
-keep class com.google.firebase.messaging.** { *; }
-keep class com.phoenixcorp.founderfinder.data.repository.ChatRepositoryImpl { *; }
-keepclassmembers class com.phoenixcorp.founderfinder.data.repository.ChatRepositoryImpl { *; }

# Allow FirebaseMessaging.send() to work after R8
-dontwarn com.google.firebase.messaging.FirebaseMessaging
-keep class com.google.firebase.messaging.RemoteMessage { *; }
-keep class com.google.firebase.messaging.RemoteMessage$Builder { *; }
# Optional: Uncomment during debugging to see what is being removed
# -printmapping mapping.txt