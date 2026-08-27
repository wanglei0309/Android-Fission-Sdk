# SPDX-FileCopyrightText: 2025 Infineon Technologies AG
# SPDX-License-Identifier: MIT
#
# Description:
# ProGuard/R8 rules used to control code shrinking, optimization,
# and obfuscation for this Android module.

##################################################
# REQUIRED FIXES for SEID failure in release builds
##################################################

# 1. CRITICAL: Keep JNI methods used by Secora SE / BLE
-keepclasseswithmembernames class * {
    native <methods>;
}

# 2. CRITICAL: Prevent enum optimization used in APDU / SEID scripts
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 3. REQUIRED: Keep constructors used via reflection
-keepclassmembers class * {
    <init>(...);
}

# 4. REQUIRED: Secure Element + Wearable SDK (SEID logic lives here)
-keep class com.infineon.secora.wearable.** { *; }

# 5. REQUIRED: ScriptHandler executes SEID fetch scripts
-keep class com.infineon.secora.wallet.domain.wearable.ble.script.ScriptHandler { *; }

# 6. REQUIRED: Crypto used during SEID fetch
-keep class javax.crypto.** { *; }
-keep class java.security.** { *; }
-dontwarn javax.crypto.**

# 7. SAFE: Bluetooth stack (prevents edge-case stripping)
-keep class android.bluetooth.** { *; }
-dontwarn android.bluetooth.**

-dontwarn java.beans.**
-dontwarn com.fasterxml.jackson.databind.ext.Java7SupportImpl

-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

#-keepattributes Signature
-keepattributes RuntimeVisibleAnnotations
-keepattributes KotlinMetadata  # If you're using Kotlin

-keep class com.infineon.secora.wallet.** { *; }
# Gson (if you're using GsonConverterFactory)
-keep class com.google.gson.** { *; }
-keep class com.infineon.secora.wallet.client.model.** { *; }  # Keep your model classes
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Retrofit interfaces
-keep interface com.infineon.secora.wallet.client.api.** { *; }
-keep class com.infineon.secora.wallet.client.api.** { *; }

# For OIDC (Google Sign-In, MSAL, etc., adjust based on what you use)
-dontwarn com.google.android.gms.**
-keep class com.google.android.gms.auth.api.identity.** { *; }
-keep class com.google.android.gms.auth.api.signin.** { *; }

# Keep Google Sign-In classes for Firebase Analytics compatibility
-keep class com.google.android.gms.auth.api.identity.** { *; }
-keep class com.google.android.gms.auth.api.signin.** { *; }

# Jackson
-keep class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.**

# Keep Parcelable classes to prevent deserialization errors
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# NOTE: Firebase Analytics ClassNotFoundException for BeginSignInRequest
# This is a known harmless warning that occurs because:
# 1. Firebase Analytics runs in a separate Dynamite module process
# 2. It tries to deserialize Parcelables containing references to BeginSignInRequest
# 3. The class isn't accessible in Firebase Analytics' isolated classloader
# 4. This is NON-FATAL - Firebase Analytics catches the exception and continues
# 5. The app uses Credential Manager API (not BeginSignInRequest), so this doesn't affect functionality
#
# This warning can be safely ignored. It doesn't crash the app or affect user experience.
# Suppress Firebase Analytics warnings about missing classes during Parcelable deserialization
-dontwarn com.google.android.gms.auth.api.identity.BeginSignInRequest

# Keep generic wrapper types
-keep class com.infineon.secora.wallet.client.data.models.common.UserResponseBody { *; }

# Optional: keep any generic response types
-keep class com.infineon.secora.wallet.client.model.** { *; }

-dontwarn com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
