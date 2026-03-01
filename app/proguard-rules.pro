# EigoSage ProGuard Rules

# ── ML Kit ──
-keep class com.google.mlkit.** { *; }

# ── Room entities ──
-keep class com.jworks.eigosage.data.local.entities.** { *; }

# ── Hilt / Dagger ──
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# ── kotlinx.serialization (critical for Supabase JSON) ──
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class **$$serializer {
    *** INSTANCE;
}

# ── Ktor client ──
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# ── Supabase ──
-keep class io.github.jan.supabase.** { *; }
-dontwarn io.github.jan.supabase.**

# ── SLF4J (Ktor dependency) ──
-dontwarn org.slf4j.**

# ── Firebase (graceful when missing) ──
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.jworks.eigosage.service.FeedbackFCMService { *; }

# ── Google Credentials / Sign-In ──
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.** { *; }
