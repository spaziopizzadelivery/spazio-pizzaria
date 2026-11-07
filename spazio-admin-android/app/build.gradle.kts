plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "br.com.spazio.admin"
    compileSdk = 35
    defaultConfig {
        applicationId = "br.com.spazio.admin"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }
}
