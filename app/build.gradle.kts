plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.moovar.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.moovar.android"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {class com.android.build.gradle.internal.dsl.ApplicationExtensionImpl$AgpDecorated_Decorated cannot be cast to class com.android.build.gradle.BaseExtension (com.android.build.gradle.internal.dsl.ApplicationExtensionImpl$AgpDecorated_Decorated and com.android.build.gradle.BaseExtension are in unnamed module of loader org.gradle.internal.classloader.VisitableURLClassLoader$InstrumentingVisitableURLClassLoader @36a58f4e)
            class com.android.build.gradle.internal.dsl.ApplicationExtensionImpl$AgpDecorated_Decorated cannot be cast to class com.android.build.gradle.BaseExtension (com.android.build.gradle.internal.dsl.ApplicationExtensionImpl$AgpDecorated_Decorated and com.android.build.gradle.BaseExtension are in unnamed module of loader org.gradle.internal.classloader.VisitableURLClassLoader$InstrumentingVisitableURLClassLoader @36a58f4e)

            Gradle's dependency cache may be corrupt (this sometimes occurs after a network connection timeout.)

            Re-download dependencies and sync project (requires network)
            The state of a Gradle build process (daemon) may be corrupt. Stopping all Gradle daemons may solve this problem.

            Stop Gradle build processes (requires restart)
            Your project may be using a third-party plugin which is not compatible with the other plugins in the project or the version of Gradle requested by the project.

            In the case of corrupt Gradle processes, you can also try closing the IDE and then killing all Java processes.

            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:network"))
    implementation(project(":core:database"))
    
    implementation(project(":feature:home"))
    implementation(project(":feature:departures"))
    implementation(project(":feature:journey"))
    implementation(project(":feature:alerts"))
    implementation(project(":feature:map"))
    implementation(project(":feature:favorites"))

    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.navigation.compose)

    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}
