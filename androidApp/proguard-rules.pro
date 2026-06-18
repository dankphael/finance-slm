# ── General ─────────────────────────────────────────────────────────────────
-keepattributes *Annotation*, Signature, Exceptions, InnerClasses, EnclosingMethod
-keepattributes SourceFile, LineNumberTable

# ── JNI bridge ───────────────────────────────────────────────────────────────
# JNI_OnLoad + RegisterNatives looks the class up by name; keep it and its
# native methods so the lookup and bindings survive shrinking/obfuscation.
-keep class com.habibi.financeslm.inference.LlamaEngineAndroid { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

# ── Kotlinx Serialization ────────────────────────────────────────────────────
-keepattributes RuntimeVisibleAnnotations, AnnotationDefault
-keep,includedescriptorclasses class com.habibi.financeslm.**$$serializer { *; }
-keepclassmembers class com.habibi.financeslm.** {
    *** Companion;
}
-keepclasseswithmembers class com.habibi.financeslm.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-dontwarn kotlinx.serialization.**

# ── Koin ─────────────────────────────────────────────────────────────────────
-keep class org.koin.** { *; }
-keepclassmembers class * {
    public <init>(...);
}
-dontwarn org.koin.**

# ── SQLDelight ───────────────────────────────────────────────────────────────
-keep class app.cash.sqldelight.** { *; }
-dontwarn app.cash.sqldelight.**

# ── WorkManager worker (instantiated reflectively) ───────────────────────────
-keep class com.habibi.financeslm.android.service.ModelDownloadWorker { *; }

# ── Ktor / coroutines ────────────────────────────────────────────────────────
-dontwarn io.ktor.**
-dontwarn kotlinx.coroutines.**
