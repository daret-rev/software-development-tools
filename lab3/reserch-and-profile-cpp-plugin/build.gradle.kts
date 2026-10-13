
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier

//import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.changelog")
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.dokka")
}

val mainSourceSet = sourceSets.main.get()
val samplesSourceSet: SourceSet = sourceSets.create("samples")

samplesSourceSet.compileClasspath += mainSourceSet.output + mainSourceSet.compileClasspath
samplesSourceSet.runtimeClasspath += mainSourceSet.output + mainSourceSet.runtimeClasspath

kotlin {
    target.compilations.getByName("samples").associateWith(target.compilations.getByName("main"))
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    testImplementation(libs.junit)

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        local("/home/daret/opt/clion")

        // Add plugin dependencies for compilation here, for example:
        bundledPlugins("com.intellij.clion", "com.intellij.clion.cmake")
        bundledModule("intellij.platform.jewel.foundation")
        bundledModule("intellij.platform.jewel.ui")
        bundledModule("intellij.platform.jewel.ideLafBridge")
        bundledModule("intellij.libraries.compose.foundation.desktop")
        bundledModule("intellij.libraries.skiko")
    }
}

intellijPlatform {
    instrumentCode = false
}


dokka {
    moduleName.set("Reserch and Profile C++")
    dokkaSourceSets.configureEach {
        documentedVisibilities.set(
            setOf(VisibilityModifier.Public, VisibilityModifier.Internal, VisibilityModifier.Private)
        )
        samples.from("src/samples/kotlin/Sample.kt")
    }
    dokkaPublications.html {
        outputDirectory.set(layout.projectDirectory.dir("docs"))
    }
}