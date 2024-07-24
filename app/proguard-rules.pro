# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
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

# With R8 full mode generic signatures are stripped for classes that are not
# kept. Suspend functions are wrapped in continuations where the type argument
# is used.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep class androidx.media3.** { *; }
## Remove the logs
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
#    public static int e(...);
}

# Media3 ShuffleOrder; Dont rename
# Media3 ShuffleOrder; Dont rename

-keepnames class androidx.media3.exoplayer.ExoPlayerImpl {
    private androidx.media3.exoplayer.source.ShuffleOrder shuffleOrder;
}

-keepnames class androidx.media3.exoplayer.source.ShuffleOrder$DefaultShuffleOrder {
    private int[] shuffled;
}

## Don't remove classes from feature modules
-keepclassmembers class com.prime.codex.** {
    public static * Codex(android.content.Context);
}
-keep class io.github.anilbeesetti.nextlib.media3ext.ffdecoder.** { *; }