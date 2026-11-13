# kotlinx.serialization models
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.x2softinc.masthan.**$$serializer { *; }
-keepclassmembers class com.x2softinc.masthan.** { *** Companion; }
-keepclasseswithmembers class com.x2softinc.masthan.** { kotlinx.serialization.KSerializer serializer(...); }
