plugins {
    id("com.android.application")
}

android {
    namespace = "com.malanchuk.svitlo"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.malanchuk.svitlo"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures.buildConfig = true

    sourceSets {
        getByName("test") {
            resources.srcDir(rootProject.file("fixtures"))
        }
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation("androidx.activity:activity-ktx:1.13.0")
    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.11.0")
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")
    implementation("com.google.android.material:material:1.13.0")

    implementation("androidx.work:work-runtime-ktx:2.12.0")
    implementation("org.jsoup:jsoup:1.24.1")

    testImplementation("junit:junit:4.13.2")
}
