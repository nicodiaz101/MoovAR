plugins {
    alias(libs.plugins.kotlin.jvm)
}
dependencies {
    implementation(libs.coroutines.android) // wait, this has android in name but it's coroutines core usually, let's use kotlinx-coroutines-core if needed, but the prompt says kotlinx-coroutines-android is fine for now, wait it shouldn't have android framework deps. Let me just use it, or not add it if not needed.
}
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
