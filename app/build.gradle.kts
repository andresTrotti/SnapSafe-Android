plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.google.gms.google-services")

    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20" // this version matches your Kotlin version
    // settings.gradle.kts

   // id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" // Use the latest version


}



android {
    namespace = "com.snapcompany.snapsafe"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.snapcompany.snapsafe"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
//    kotlinOptions {
//        jvmTarget = "1.8"
//    }

    kotlin {
        jvmToolchain(17) // This is a modern way to also set the JDK toolchain
        // And to be doubly sure about the target for the compiler itself:
        sourceSets.all {
            languageSettings.optIn("kotlin.RequiresOptIn") // Example of another language setting
        }
        // If you still need to set jvmTarget explicitly within this block:
        // (Though jvmToolchain often covers this implicitly for the target bytecode)
        // targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmTarget> { // If using KMP structure
        //     compilations.all {
        //         compilerOptions.options.jvmTarget.set(JvmTarget.JVM_1_8)
        //     }
        // }
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17) )// e.g., JavaLanguageVersion.of(17)
        }
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}




dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.firebase.firestore)

    // Import the Firebase BoM
    implementation(platform(libs.firebase.bom))

    // TODO: Add the dependencies for Firebase products you want to use
    // When using the BoM, don't specify versions in Firebase dependencies
    implementation(libs.firebase.analytics)


    implementation(libs.androidx.credentials)
    implementation(libs.googleid)
    implementation(libs.play.services.auth.v2030)
    // optional - needed for credentials support from play services, for devices running
    // Android 13 and below.
    implementation(libs.androidx.credentials.play.services.auth)

    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.crashlytics.buildtools)

    debugImplementation(libs.androidx.ui.test.manifest)
    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)

    implementation(libs.guava)

    implementation(libs.barcode.scanning)
    implementation(libs.core)

    implementation(libs.androidx.core.splashscreen)



    //Room
    implementation(libs.androidx.room.runtime)
   // annotationProcessor(libs.androidx.room.compiler)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.security.crypto) // Use the latest version


    // optional - Kotlin Extensions and Coroutines support for Room
    implementation(libs.androidx.room.ktx)

    //Preferences store
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.gson)

    //Async image
    implementation(libs.coil.compose)



    implementation(libs.androidx.activity.ktx) // For activity results

    androidTestImplementation (libs.ui.test.junit4)

}