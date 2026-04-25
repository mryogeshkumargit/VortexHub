# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
# http://developer.android.com/guide/developing/tools/proguard.html

# Keep all classes that might be used in XML layouts
-keep public class * extends android.view.View
-keep public class * extends android.app.Activity
-keep public class * extends androidx.fragment.app.Fragment
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# Keep the annotations
-keepattributes *Annotation*

# Keep Retrofit and OkHttp
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-dontwarn okio.**

# Keep Gson
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**

# Keep Room
-keep class androidx.room.** { *; }

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Keep Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** { volatile <fields>; }

# Keep Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keep,includedescriptorclasses class com.vortexai.android.**$$serializer { *; }
-keepclassmembers class com.vortexai.android.** {
    *** Companion;
}
-keepclasseswithmembers class com.vortexai.android.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Application specific rules
-keep class com.vortexai.android.data.models.** { *; }
-keep class com.vortexai.android.domain.models.** { *; } 