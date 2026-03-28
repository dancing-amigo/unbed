plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.unbed.core.database"
    compileSdk = 34

    defaultConfig {
        minSdk = 29
        consumerProguardFiles("consumer-rules.pro")

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":apps:android:core:model"))
    implementation(project(":apps:android:domain"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.coroutines.core)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    testImplementation(libs.junit4)
}
