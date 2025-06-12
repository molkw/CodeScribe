plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm") version "1.9.22"
  id("org.jetbrains.intellij") version "1.16.0"
}

repositories {
  mavenCentral()
  google() // optional but useful
}

intellij {
  version.set("2023.2.5")
  type.set("IC")

  plugins.set(listOf(
          "java",
          "org.jetbrains.kotlin"
  ))
}

dependencies {
  // Add any required dependencies here if needed
}
