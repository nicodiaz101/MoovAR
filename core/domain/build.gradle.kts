plugins {
    alias(libs.plugins.kotlin.jvm)
}
dependencies {
    implementation(project(":core:common"))
    implementation(libs.coroutines.core)
    implementation("javax.inject:javax.inject:1")
}
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
kotlin {
    jvmToolchain(21)
}
