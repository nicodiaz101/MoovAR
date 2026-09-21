plugins {
    alias(libs.plugins.kotlin.jvm)
}
dependencies {
    implementation(project(":core:common"))
    implementation(libs.coroutines.core)
    implementation("javax.inject:javax.inject:1")

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
kotlin {
    jvmToolchain(21)
}
dependencies { compileOnly("androidx.compose.runtime:runtime:1.6.0") }
