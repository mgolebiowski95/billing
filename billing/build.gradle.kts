plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

kotlin {
    jvmToolchain(17)
}

android {
    namespace = "com.mgsoftware.billing"
    compileSdk = 36

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        version = "8.3.0-1.0.0"
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
}

dependencies {
    implementation("com.android.billingclient:billing-ktx:8.3.0")
    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    implementation(platform("io.insert-koin:koin-bom:4.2.0"))
    implementation("io.insert-koin:koin-android")
    implementation("io.insert-koin:koin-core")
}

afterEvaluate {
    tasks.named<AbstractArchiveTask>("debugSourcesJar") {
        archiveFileName.set("billing-$version-sources.jar")
    }
}

android.libraryVariants.all {
    val variantOutput = outputs.first() as com.android.build.gradle.internal.api.LibraryVariantOutputImpl
    variantOutput.outputFileName = "billing-$version.aar"
}

tasks.register<Copy>("prepareRelease") {
    dependsOn(":billing:debugSourcesJar", ":billing:assembleDebug")

    val sourcesJar = project.file("build/libs/billing-$version-sources.jar")
    val aar = project.file("build/outputs/aar/billing-$version.aar")
    val destDir = project.file("${rootProject.projectDir}/release")

    from(sourcesJar, aar)
    into(destDir)
}
