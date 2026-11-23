# Keep line numbers & annotations for reliable crash diagnostics and framework reflection
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable

# Preserve explicit @Keep classes and members (e.g., Firestore backup DTOs)
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# Preserve Room entities, DAOs, database classes, and KSP-generated *_Impl classes
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep class com.example.data.** { *; }
-keep class **_Impl { *; }

# Preserve Manifest-registered Android components
-keep public class com.example.MainActivity { *; }
-keep public class com.example.ui.PaymentAlarmActivity { *; }
-keep public class com.example.util.PaymentReminderReceiver { *; }
-keep public class com.example.widget.MasrofyWidgetProvider { *; }

# Preserve Firestore model annotations & fields
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName *;
    @com.google.firebase.firestore.DocumentId *;
    @com.google.firebase.firestore.ServerTimestamp *;
    @com.google.firebase.firestore.Exclude *;
}

# Preserve Credential Manager & Google Identity Services reflection hooks
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# Preserve BiometricPrompt callbacks
-keep class androidx.biometric.** { *; }

# Suppress benign warnings from optional/platform-specific transitive classes
-dontwarn org.codehaus.mojo.animal_sniffer.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn sun.misc.Unsafe
