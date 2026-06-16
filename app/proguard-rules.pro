# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault

# Gson DTOs and saved scan snapshots depend on stable field names.
-keepclassmembers class org.zacsn.signal_dectect.data.api.** {
    <fields>;
    public <init>(...);
    public *;
}
-keepclassmembers class org.zacsn.signal_dectect.domain.model.SignalDevice {
    <fields>;
    public <init>(...);
    public *;
}
-keepclassmembers enum org.zacsn.signal_dectect.domain.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Room generates adapters from entity and DAO metadata.
-keep class org.zacsn.signal_dectect.data.database.** { *; }
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# Retrofit creates implementations from annotated interfaces.
-keep interface org.zacsn.signal_dectect.data.api.AuthApiService { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class retrofit2.HttpException
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-dontwarn javax.annotation.**

# Hilt entry points and generated components are annotation driven.
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-dontwarn dagger.hilt.**
-dontwarn javax.inject.**
-dontwarn kotlin.**
