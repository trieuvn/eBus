import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget // Import hỗ trợ JvmTarget mới

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.project_bus"
    compileSdk = 36

    // Load keys from local.properties
    val localProps = Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }

    // Lấy key, nếu null thì báo lỗi rõ ràng để bạn biết đường sửa
    val supabaseUrl = localProps.getProperty("SUPABASE_URL")
        ?: throw GradleException("Lỗi: Thiếu 'SUPABASE_URL' trong file local.properties. Hãy xem lại Bước 1.")
    val supabaseAnonKey = localProps.getProperty("SUPABASE_ANON_KEY")
        ?: throw GradleException("Lỗi: Thiếu 'SUPABASE_ANON_KEY' trong file local.properties. Hãy xem lại Bước 1.")

    defaultConfig {
        applicationId = "com.example.project_bus"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Inject biến vào BuildConfig để code Kotlin dùng được
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
    }

    buildFeatures {
        buildConfig = true
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }

    // --- PHẦN ĐÃ SỬA: Thay kotlinOptions bằng compilerOptions ---
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // coroutines + lifecycleScope
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)

    // Supabase (BOM) + modules
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.auth)
    implementation(libs.ktor.client.android)

    // Java desugaring libs
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
