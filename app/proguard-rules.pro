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

# SSHJ - SFTP support
-keep class net.schmizz.** { *; }
-keep class com.hierynomus.** { *; }
-keep class org.bouncycastle.** { *; }
-dontwarn net.schmizz.**
-dontwarn com.hierynomus.**

# Commons Compress - 7z/tar/gz/bz2/xz support
-keep class org.apache.commons.compress.** { *; }
-dontwarn org.apache.commons.compress.**
-dontwarn org.tukaani.**

# Optional desktop/JDK APIs referenced by mbassy, BouncyCastle and EdDSA but not
# present on Android. These code paths are unreachable at runtime; without these
# rules R8 fails release builds with "Missing classes detected".
-dontwarn javax.el.BeanELResolver
-dontwarn javax.el.ELContext
-dontwarn javax.el.ELResolver
-dontwarn javax.el.ExpressionFactory
-dontwarn javax.el.FunctionMapper
-dontwarn javax.el.ValueExpression
-dontwarn javax.el.VariableMapper
-dontwarn javax.naming.Binding
-dontwarn javax.naming.NamingEnumeration
-dontwarn javax.naming.NamingException
-dontwarn javax.naming.directory.Attribute
-dontwarn javax.naming.directory.Attributes
-dontwarn javax.naming.directory.DirContext
-dontwarn javax.naming.directory.InitialDirContext
-dontwarn javax.naming.directory.SearchControls
-dontwarn javax.naming.directory.SearchResult
-dontwarn org.bouncycastle.pqc.jcajce.interfaces.KyberPublicKey
-dontwarn org.bouncycastle.pqc.jcajce.spec.KyberParameterSpec
-dontwarn sun.security.x509.X509Key