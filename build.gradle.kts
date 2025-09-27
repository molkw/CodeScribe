import org.jetbrains.intellij.tasks.PatchPluginXmlTask

plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm") version "1.9.22"
  id("org.jetbrains.intellij") version "1.16.0"
}


repositories {
  mavenCentral()
  google()
}

// Configure Gradle IntelliJ Plugin
intellij {
  version.set("2023.2.5")
  type.set("IC") // Target IDE Platform

  plugins.set(listOf(
          "java",
          "org.jetbrains.kotlin"
  ))
}

// Configure Gradle IntelliJ Plugin - extension
tasks {
  // Set the JVM compatibility versions
  withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
  }

  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
  }

  patchPluginXml {
    version.set("1.0.0")
    sinceBuild.set("232")
    untilBuild.set("241.*")

    // Optional: Add change notes and description
    changeNotes.set("""
            Initial release of the plugin.
            Add your change notes here.
        """.trimIndent())

    pluginDescription.set("""
            Your plugin description goes here.
            Describe what your plugin does.
        """.trimIndent())
  }

  signPlugin {
    // Optional: Configure plugin signing
    // certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
    // privateKey.set(System.getenv("PRIVATE_KEY"))
    // password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
  }

  publishPlugin {
    // Optional: Configure plugin publishing
    // token.set(System.getenv("PUBLISH_TOKEN"))
  }
}

// Optional: Configure Java compatibility
java {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}