plugins {
    alias(libs.plugins.kotlin.jvm)
}
dependencies {
    implementation(libs.coroutines.android)
}
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
