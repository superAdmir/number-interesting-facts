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
# WorkManager 2.7.0 (a transitive dependency of the Google Mobile Ads SDK) uses Room 2.2.5,
# which creates its generated database (WorkDatabase_Impl) reflectively through the no-arg
# constructor. Room 2.2.5's consumer rule keeps the class but not that constructor, and R8 full
# mode (the AGP default) removes it, crashing the app at start-up ("Failed to create an instance
# of androidx.work.impl.WorkDatabase"). Found by running the shrunk QA build on a device.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
