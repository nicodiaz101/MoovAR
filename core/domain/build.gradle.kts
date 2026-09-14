plugins {
    alias(libs.plugins.kotlin.jvm)
}
dependencies {
    implementation(libs.coroutines.android)
}
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
