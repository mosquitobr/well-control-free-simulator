# ProGuard / R8 rules for Well Control Free Simulator (Android release)
# Keep enum values used by valueOf() during JSON persistence decoding
-keepclassmembers enum com.mosquitobr.controlepoco.core.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Preserve source file names and line numbers for clean stack traces
-keepattributes SourceFile,LineNumberTable
