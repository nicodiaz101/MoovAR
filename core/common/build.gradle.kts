plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}
android {
    namespace = "com.moovar.android.core.common"
    compileSdk = 36
    defaultConfig {
        minSdk = 30
    }
    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(25)
        targetCompatibility = JavaVersion.toVersion(25)
    }
    kotlinOptions {
        jvmTarget = "25"
    }
}
dependencies {
    implementation(libs.coroutines.android)
}
