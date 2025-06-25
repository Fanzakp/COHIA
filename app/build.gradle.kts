plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.cohia"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.cohia"
        minSdk = 24
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    // Enable ViewBinding untuk ResultActivity (opsional, tapi direkomendasikan)
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Versi dependensi CameraX
    val cameraxVersion = "1.3.1"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    implementation("androidx.camera:camera-extensions:$cameraxVersion")

    // Dependensi OkHttp (digunakan oleh RoboflowAPI.java)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Dependensi Glide untuk memuat gambar dengan efisien
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // TAMBAHAN BARU: Gson untuk JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")

    // TAMBAHAN BARU: CardView untuk UI yang lebih baik
    implementation("androidx.cardview:cardview:1.0.0")

    // Dependensi UI standar
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.activity:activity:1.8.2") // Ganti dengan versi non-KTX karena menggunakan Java

    // Dependensi untuk testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}