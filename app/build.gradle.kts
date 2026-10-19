plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.quranichorizons"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.quranichorizons"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        // التلاوة معطّلة حتى يُوثَّق ترخيص التسجيلات (انظر docs/EXTERNAL_DEPENDENCIES.md)
        buildConfigField("boolean", "AUDIO_ENABLED", "false")
        // عنوان مستودع المحتوى البعيد (Supabase Storage). فارغ = المحتوى المضمَّن فقط
        buildConfigField("String", "CONTENT_BASE_URL", "\"\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // المحتوى المضمَّن (محتوى النموذج الأول) مصدره content/bootstrap في جذر المستودع
    sourceSets["main"].assets.srcDirs("src/main/assets", "../content/bootstrap")

    testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    debugImplementation(libs.compose.ui.tooling)
    testImplementation(libs.junit)
}
