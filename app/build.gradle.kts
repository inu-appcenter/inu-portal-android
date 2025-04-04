plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "inu.appcenter.intip_android"
    compileSdk = 35

    signingConfigs {
        create("release") {
            val storeFilePath =
                project.findProperty("android.injected.signing.store.file")?.toString()
                    ?: "app/keystore.jks"

            storeFile = file("$rootDir/$storeFilePath")

            storePassword =
                project.findProperty("android.injected.signing.store.password")?.toString() ?: ""
            keyAlias =
                project.findProperty("android.injected.signing.key.alias")?.toString() ?: ""
            keyPassword =
                project.findProperty("android.injected.signing.key.password")?.toString() ?: ""
        }
    }

    defaultConfig {
        applicationId = "inu.appcenter.intip_android"
        minSdk = 25
        targetSdk = 34
        versionCode = 13
        versionName = "0.1.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            isShrinkResources = false
            isPseudoLocalesEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    //Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.0.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.espresso.core)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.androidx.material.icons.extended)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Retrofit2
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)

    // Gson
    implementation(libs.gson)

    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Koin(DI)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    // Coil core
    implementation(libs.coil)
    // Coil for Compose
    implementation(libs.coil.compose.v240)
    // Coil SVG support
    implementation(libs.coil.svg.v240)

    // For AGP 7.4+ Java 8+ API 디슈가링 지원
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}
