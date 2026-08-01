import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.sqldelight)
}

kotlin {
    compilerOptions {
        // expect/actual em classe ainda é marcado como Beta pelo compilador. É
        // o mecanismo padrão pra abrir SQLite por plataforma (KT-61573) — o
        // aviso é ruído, não sinal.
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

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
            // api, não implementation: Flow e LocalDate aparecem na API pública
            // do repositório e do modelo — quem consome :shared precisa deles.
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.datetime)
            implementation(libs.sqldelight.coroutines.extensions)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
        }
        // Driver JDBC só nos testes de host: exercita o schema num SQLite de
        // verdade (em memória) em vez de dublê, sem precisar de emulador.
        getByName("androidHostTest").dependencies {
            implementation(libs.sqldelight.sqlite.driver)
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
