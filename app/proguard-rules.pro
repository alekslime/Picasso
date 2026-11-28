# Picasso release R8/ProGuard rules.

# Keep domain models & Room entities/DAOs intact for reflection/serialization if needed
-keep class app.picasso.tracer.domain.model.** { *; }
-keep class app.picasso.tracer.data.local.** { *; }

# ARCore (Google Play Services for AR)
-keep class com.google.ar.core.** { *; }
-dontwarn com.google.ar.core.**

# Google Play Billing
-keep class com.android.billingclient.api.** { *; }

# Credential Manager & Google Identity
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# Strip debug/verbose logs in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}
