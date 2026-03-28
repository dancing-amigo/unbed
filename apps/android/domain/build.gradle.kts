plugins {
    alias(libs.plugins.kotlinJvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":apps:android:core:model"))
    implementation(libs.coroutines.core)
    testImplementation(libs.junit4)
    testImplementation(libs.truth)
}
