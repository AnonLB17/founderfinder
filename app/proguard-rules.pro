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
# WORKMANAGER + ROOM FIX
# =============================================
-keep class androidx.work.impl.WorkDatabase_Impl { *; }
-keepclassmembers class androidx.work.impl.WorkDatabase_Impl { *; }
-keep class androidx.work.** { *; }
-keepclassmembers class * extends androidx.work.ListenableWorker { *; }
-keepclassmembers class * extends androidx.work.InputMerger { void <init>(); }

# =============================================
# FCM PUSH NOTIFICATIONS - STRONG RULES
# =============================================
-keep class com.google.firebase.messaging.** { *; }
-keep class com.google.android.gms.cloudmessaging.** { *; }
-keep class com.google.firebase.iid.** { *; }

-keepclassmembers class * extends com.google.firebase.messaging.FirebaseMessagingService {
    public void onMessageReceived(com.google.firebase.messaging.RemoteMessage);
    public void onNewToken(java.lang.String);
}

-keep class com.phoenixcorp.founderfinder.**FirebaseMessagingService { *; }  # if you have custom one

-dontwarn com.google.firebase.messaging.**
-dontwarn com.google.android.gms.cloudmessaging.**

# =============================================
# YOUR DOMAIN MODELS
# =============================================
-keep class com.phoenixcorp.founderfinder.domain.model.** { *; }
-keepclassmembers class com.phoenixcorp.founderfinder.domain.model.** { *; }
-keepnames class com.phoenixcorp.founderfinder.domain.model.**

# Extra strong protection for Firestore
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
# Hilt / Dagger + Other
# =============================================
-keep,allowobfuscation,allowshrinking @dagger.hilt.EntryPoint class * { *; }
-keep,allowobfuscation,allowshrinking @dagger.hilt.android.EarlyEntryPoint class * { *; }
-keep class com.phoenixcorp.founderfinder.**_Hilt* { *; }
-keep class dagger.hilt.** { *; }

# Jetpack Compose, Coil, Parcelable, Kotlin, etc. (unchanged)
-keep,allowshrinking class * extends androidx.compose.ui.node.ModifierNodeElement { *; }
-keep,allowobfuscation,allowshrinking class * extends androidx.navigation.Navigator { *; }
-keep class coil.** { *; }
-keep class * implements android.os.Parcelable { *; }
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}
-keepattributes Annotation, InnerClasses
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlinx.coroutines.** { *; }

# General
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# Optional: Uncomment for debugging
# -printmapping mapping.txt