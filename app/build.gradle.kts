plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "co.kr.checkmate"
    compileSdk = 35

    defaultConfig {
        applicationId = "co.kr.checkmate"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    kotlin {
        jvmToolchain(17)
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // AndroidX Core 라이브러리
    implementation(libs.bundles.androidx.core)

    // Compose BOM
    implementation(platform(libs.compose.bom))

    // Compose 핵심 라이브러리
    implementation(libs.bundles.compose.core)

    // Compose Navigation
    implementation(libs.bundles.compose.navigation)
    ksp(libs.compose.destinations.ksp)

    // Room Database
    implementation(libs.bundles.room)
    ksp(libs.room.compiler)

    // Koin DI
    implementation(libs.bundles.koin)

    // Coroutines
    implementation(libs.bundles.coroutines)

    // 날짜/시간 처리
    implementation(libs.threetenabp)

    // 위젯 (Glance)
    implementation(libs.bundles.glance)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Accompanist
    implementation(libs.bundles.accompanist)

    // 로깅
    implementation(libs.timber)

    // 테스트 의존성
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.androidx.test.espresso)

    // Compose 테스트
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test)

    // 디버그 전용 의존성
    debugImplementation(libs.bundles.compose.debug)
}