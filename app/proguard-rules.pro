#Preserve Google Play licensing service

-keep public class com.google.android.vending.licensing.ILicensingService

Preserve annotations

-keep class android.support.annotation.Keep -keep @interface androidx.annotation.Keep { *; } -keep @androidx.annotation.Keep class * { *; } -keep @interface com.google.android.gms.common.annotation.KeepName { *; } -keep @com.google.android.gms.common.annotation.KeepName class * { *; } -keep @interface com.google.android.gms.common.util.DynamiteApi { *; }

#Keep Navigation classes

-keep,allowobfuscation,allowshrinking class * extends androidx.navigation.Navigator { *; }

#Keep Hilt entry points

-keep,allowobfuscation,allowshrinking @dagger.hilt.EntryPoint class * { *; } -keep,allowobfuscation,allowshrinking @dagger.hilt.android.EarlyEntryPoint class * { *; } -keep,allowobfuscation,allowshrinking @dagger.hilt.internal.ComponentEntryPoint class * { *; } -keep,allowobfuscation,allowshrinking @dagger.hilt.internal.GeneratedEntryPoint class * { *; }

#Keep Firebase components

-keep class * implements com.google.firebase.components.ComponentRegistrar { ; } -keep,allowshrinking interface com.google.firebase.components.ComponentRegistrar { ; } -dontwarn com.google.firebase.

Keep Coil classes

-keep class coil.** { ; } -dontwarn coil.*

Keep Compose classes

-keep,allowshrinking class * extends androidx.compose.ui.node.ModifierNodeElement { *; }

#Keep other classes

-keep class com.google.android.gms.common.internal.ReflectedParcelable { *; } -keep,allowshrinking class * implements com.google.android.gms.common.internal.ReflectedParcelable { *; } -keep,allowshrinking class * implements androidx.versionedparcelable.VersionedParcelable { *; } -keep public class androidx.versionedparcelable.ParcelImpl { *; } -keep,allowshrinking class * extends androidx.startup.Initializer { *; }

#Kotlin serialization

-keepattributes Annotation, InnerClasses -dontnote kotlinx.serialization.SerializationStrategy -dontnote kotlinx.serialization.DeserializationStrategy