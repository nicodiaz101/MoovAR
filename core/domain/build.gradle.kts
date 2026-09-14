plugins {
    alias(libs.plugins.kotlin.jvm)
}
dependencies {
    implementation(libs.coroutines.android)
}
java {
    sourceCompatibility = JavaVersion.toVersion(25)
    targetCompatibility = JavaVersion.toVersion(25)
}
kotlin {
    jvmToolchain(25)
}
