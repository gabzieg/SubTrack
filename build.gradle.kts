// Declara os plugins uma vez na raiz com `apply false` — sem isto o Kotlin
// Gradle plugin é carregado duas vezes (:shared e :androidApp) e o próprio
// Gradle avisa que isso pode quebrar o build.
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidKotlinMultiplatformLibrary) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.ktlint)
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        filter {
            // Código gerado pelo SQLDelight não é nosso pra formatar.
            exclude { it.file.path.contains("generated") }
        }
    }
}
