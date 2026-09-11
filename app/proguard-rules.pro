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

-keep class com.raival.compose.file.explorer.** { *; }
-keep class org.eclipse.tm4e.** { *; }
-keep class org.joni.** { *; }
-keep class android.content.** { *; }
-keep class com.android.apksig.** { *; }

-keep class com.tom.roush.pdfbox.** { *; }
-keep class org.apache.fontbox.** { *; }
-keep class org.apache.commons.logging.** { *; }
-dontwarn com.tom.roush.pdfbox.**
-dontwarn org.apache.fontbox.**

# Apache POI - Office document processing
-keep class org.apache.poi.** { *; }
-keep class org.apache.poi.hssf.** { *; }
-keep class org.apache.poi.hwpf.** { *; }
-keep class org.apache.poi.xssf.** { *; }
-keep class org.apache.poi.xwpf.** { *; }
-keep class org.apache.poi.xslf.** { *; }
-keep class org.apache.poi.hslf.** { *; }
-keep class org.apache.poi.openxml4j.** { *; }
-keep class org.apache.poi.poifs.** { *; }
-keep class org.apache.poi.openxml4j.opc.** { *; }
-dontwarn org.apache.poi.**
-dontwarn org.apache.logging.**
-dontwarn aQute.bnd.annotation.**
-dontwarn java.awt.**
-dontwarn javax.xml.stream.**
-dontwarn net.sf.saxon.**
-dontwarn com.gemalto.jp2.**
-dontwarn org.osgi.framework.**

-keepnames interface * { *; }

# Shizuku
-keep class rikka.shizuku.** { *; }
-keep class moe.shizuku.** { *; }
-dontwarn rikka.shizuku.**