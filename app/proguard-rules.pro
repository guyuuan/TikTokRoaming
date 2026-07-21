# Modern libxposed API 102 entry-point rules. The entry class may be renamed, but it must not be
# removed and its name in java_init.list must be rewritten to match the obfuscated class name.
-dontwarn io.github.libxposed.annotation.**
-adaptresourcefilecontents META-INF/xposed/java_init.list
-keep,allowoptimization,allowobfuscation public class * extends io.github.libxposed.api.XposedModule {
    public <init>();
}
