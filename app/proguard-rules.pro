# Keep CameraX & Camera2Interop classes used via reflection
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# Keep ML Kit & Play Services classes for R8 release builds
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.**
