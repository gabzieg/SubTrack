import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.sqldelight)
}

kotlin {
    // Sob AGP 9 o alvo Android do módulo compartilhado é declarado aqui dentro —
    // o antigo com.android.library não convive mais com o plugin KMP.
    android {
        namespace = "com.subtrack.shared"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()

        // Sem isto o commonTest não é ligado a nenhuma compilação e os testes
        // simplesmente não rodam (o build passa em silêncio, que é pior).
        withHostTest {}

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    // Alvos Apple entram na Fase 4 (exigem macOS pra compilar — ver ROADMAP.md).
    // commonMain/commonTest já isolam a lógica pra que adicionar iosArm64 e
    // iosSimulatorArm64 não force refatoração.

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.sqldelight.coroutines.extensions)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
        }
    }
}

sqldelight {
    databases {
        create("SubTrackDatabase") {
            packageName.set("com.subtrack.shared.db")
        }
    }
}
