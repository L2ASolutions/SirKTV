# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.sirktv.app.**$$serializer { *; }
-keepclassmembers class com.sirktv.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.sirktv.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
