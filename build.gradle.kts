plugins {
    id("com.android.library") version "8.3.0"
    id("org.jetbrains.kotlin.android") version "1.9.23"
}

android {
    namespace = "com.oxylab.sdk.startup"
    compileSdk = 36

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    
    // ViewPager2 and RecyclerView for SDK features
    implementation("androidx.lifecycle:lifecycle-process:2.7.0")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    
    // Google AdMob
    implementation("com.google.android.gms:play-services-ads:23.0.0")
    
    // Firebase Auth
    implementation(platform("com.google.firebase:firebase-bom:32.7.4"))
    implementation("com.google.firebase:firebase-auth-ktx")
    
    // Shimmer
    implementation("com.facebook.shimmer:shimmer:0.5.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    
    // Lottie
    implementation("com.airbnb.android:lottie:6.7.1")
}

// ── JitPack distribution metadata ──────────────────────────────────────────
// JitPack calls :bundleReleaseAar directly — no maven-publish plugin needed.
// group and version are all JitPack requires at the root level.
group = "com.github.Zaquwan"
version = "1.2.5"

kotlin {
    jvmToolchain(17)
}

