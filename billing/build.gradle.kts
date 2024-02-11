import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.archivesName

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.mgsoftware.billing"
    compileSdk = 34

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        version = "6.2.0"
        archivesName.set("billing-$version")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
}

dependencies {
    implementation("com.android.billingclient:billing-ktx:6.2.0")
    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    implementation(platform("io.insert-koin:koin-bom:3.5.3"))
    implementation("io.insert-koin:koin-android")
    implementation("io.insert-koin:koin-core")
}

tasks.whenTaskAdded { ->
    if (name == "debugSourcesJar") {
        tasks.named<Jar>("debugSourcesJar") {
            archiveFileName.set("${archivesName.get()}-debug-sources.jar")
        }
    }
}

tasks.whenTaskAdded { ->
    if (name == "releaseSourcesJar") {
        tasks.named<Jar>("releaseSourcesJar") {
            archiveFileName.set("${archivesName.get()}-release-sources.jar")
        }
    }
}
