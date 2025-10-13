# 1. 保留 Gson 库本身
-keep class com.google.gson.** { *; }
-keepclassmembers class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# 2. 保留泛型签名 —— 反序列化 List<T>、Map<K,V> 等必需
-keepattributes Signature

# 3. 保留注解 —— @SerializedName、@Expose 等
-keepattributes *Annotation*

# 4. 保留 TypeToken 的匿名子类（gson.fromJson(json, new TypeToken<List<Bean>>(){}))
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken { *; }

# 5. 保留所有使用到的 JavaBean（把 com.example.your.model 换成你实际放实体类的包）
-keep class com.bbkb.sc.schedule.** { *; }
-keep class com.bbkb.sc.schedule.database.** { *; }
-keep class com.bbkb.sc.schedule.gripper.** { *; }

