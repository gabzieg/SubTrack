plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

// Sob AGP 9 este módulo NÃO aplica o plugin KMP — com.android.application e o
// plugin multiplataforma não convivem mais. O Kotlin já vem embutido no AGP 9.
// Aqui é Android puro com androidx.compose; todo código compartilhável mora
// em :shared, que é quem carrega o Compose Multiplatform quando o iOS entrar.

android {
    namespace = "com.subtrack.android"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.subtrack.android"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        lintConfig = file("lint.xml")
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.work.runtime.ktx)
}
