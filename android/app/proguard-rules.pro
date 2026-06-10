# Default ProGuard rules for Opencode2Phone

# Keep Moshi adapters
-keep class com.opencode2phone.data.remote.dto.** { *; }

# Keep Room entities
-keep class com.opencode2phone.data.local.entity.** { *; }
