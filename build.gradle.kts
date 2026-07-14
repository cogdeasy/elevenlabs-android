// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.dokka) apply false
}

// Repository configuration is handled in settings.gradle.kts

subprojects {
    // Common configuration for all modules
    ext {
        set("compileSdkVersion", 35)
        set("minSdkVersion", 21)
        set("targetSdkVersion", 34)
    }

    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        buildUponDefaultConfig = true
        config.setFrom(rootProject.file("config/detekt/detekt.yml"))
        baseline = file("detekt-baseline.xml")
        source.setFrom(
            files(
                "src/main/java",
                "src/main/kotlin",
                "src/test/java",
                "src/test/kotlin",
                "src/androidTest/java",
                "src/androidTest/kotlin",
            ),
        )
    }

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set("1.5.0")
        android.set(true)
        baseline.set(file("ktlint-baseline.xml"))
        filter {
            exclude { it.file.path.contains("/build/") }
        }
    }
}