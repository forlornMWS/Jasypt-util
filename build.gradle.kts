plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.intellij") version "1.17.4"
    id("org.jetbrains.changelog") version "2.2.1"
}

group = "xyz.mwszksnmdys"
version = "1.0.5"

repositories {
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2023.2.8")
    type.set("IC") // Target IDE Platform

    plugins.set(listOf("java","yaml"))
}

dependencies {
// https://mvnrepository.com/artifact/org.jasypt/jasypt
    implementation("org.jasypt:jasypt:1.9.3")
    compileOnly("org.projectlombok:lombok:1.18.20")
    annotationProcessor("org.projectlombok:lombok:1.18.20")
}

changelog {
    version.set(project.version.toString())
    path.set(file("CHANGELOG.md").canonicalPath)
    groups.set(listOf("Added", "Changed", "Deprecated", "Removed", "Fixed", "Security"))
    itemPrefix.set("-")
    keepUnreleasedSection.set(true)
    unreleasedTerm.set("[Unreleased]")
    lineSeparator.set("\n")
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }

    patchPluginXml {
        sinceBuild.set("232")
        untilBuild.set("243.*")
        changeNotes.set(provider {
            changelog.render(
                org.jetbrains.changelog.Changelog.OutputType.HTML
            )
        })
    }
}

