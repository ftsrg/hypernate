/* SPDX-License-Identifier: Apache-2.0 */

/* Originally based on https://github.com/mingyang91/openjml-template */

import com.diffplug.gradle.spotless.SpotlessExtension
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import hu.bme.mit.ftsrg.gradle.openjml.*
import java.io.File

val openJMLDir: Directory = layout.projectDirectory.dir(".openjml")
val openJMLJavaHomeDir: Directory = openJMLDir.dir("jdk")
val downloadDir: Provider<Directory> = layout.buildDirectory.dir("tmp/download")

val jmlavac: RegularFile = openJMLJavaHomeDir.file("bin/jmlavac")
val jmlava: RegularFile = openJMLJavaHomeDir.file("bin/jmlava")

val withoutOpenJML: String? by project
val noOpenJML: Boolean = withoutOpenJML != null && withoutOpenJML.toBoolean() == true

plugins {
  `java-library`
  id("com.github.johnrengelman.shadow") version "8.1.1"
  id("com.diffplug.spotless") version "6.20.0"
}

group = "hu.bme.mit.ftsrg"

version = "0.1.0"

repositories {
  mavenCentral()
  maven { url = uri("https://jitpack.io") }
}

dependencies {
  implementation("ch.qos.logback:logback-classic:1.4.11")
  implementation("com.google.code.gson:gson:2.10.1")
  implementation("com.jcabi:jcabi-aspects:0.25.1")
  implementation("org.aspectj:aspectjrt:1.9.20")
  implementation("org.aspectj:aspectjweaver:1.9.20")
  implementation("org.hyperledger.fabric-chaincode-java:fabric-chaincode-shim:2.5.0")
  implementation("org.hyperledger.fabric:fabric-protos:0.3.0")
  implementation("org.projectlombok:lombok:1.18.28")

  testImplementation("org.assertj:assertj-core:3.24.2")
  testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
  testImplementation("org.mockito:mockito-core:5.4.0")
  testImplementation(files("$openJMLDir/jmlruntime.jar"))
}

if (!noOpenJML) {
  tasks.named<ShadowJar>("shadowJar") { dependsOn(tasks.named("initOpenJML")) }

  tasks.test { java { jvmArgs = listOf("-Dorg.jmlspecs.openjml.rac=exception") } }

  tasks.withType<JavaCompile>().configureEach {
    dependsOn(tasks.named("initOpenJML"))
    // Only when not compiling because of Spotless
    if (!gradle.startParameter.taskNames.any { it.contains("spotlessApply") }) {
      val mode =
          when (System.getenv("JML_MODE")) {
            "esc" -> "esc"
            else -> "rac"
          }
      options.isFork = true
      options.compilerArgs.addAll(
          listOf(
              "-jml",
              "-$mode",
              "-timeout",
              "30",
              "--nullable-by-default",
              "--specs-path",
              "specs/"))
      options.forkOptions.javaHome = openJMLJavaHomeDir.asFile
    }
  }
}

tasks.test { useJUnitPlatform() }

configure<SpotlessExtension> {
  java {
    importOrder()
    removeUnusedImports()
    googleJavaFormat()
    formatAnnotations()
    toggleOffOn()
  }
  kotlin {
    target("src/*/kotlin/**/*.kt", "buildSrc/src/*/kotlin/**/*.kt")
    ktfmt()
  }
  kotlinGradle { ktfmt() }
}

tasks.register("initOpenJML") {
  val openJMLVersion: String by project

  val zipFile: File = downloadDir.get().file("openjml.zip").asFile
  downloadOpenJML(openJMLVersion, zipFile, logger)
  extractOpenJML(zipFile, openJMLDir, logger)

  // `jmlavac' is what we call `javac' that is actually
  // OpenJML's javac; likewise, `jmlava' is a wrapper for `java' with
  // OpenJML already in the classpath
  generateJmlavac(jmlavac.asFile, openJMLJavaHomeDir, logger)
  replaceJavac(openJMLJavaHomeDir, jmlavac.asFile, logger)
  generateJmlava(jmlava.asFile, openJMLJavaHomeDir, logger)
  replaceJava(openJMLJavaHomeDir, jmlava.asFile, logger)
  logger.lifecycle("✅ OpenJML successfully initialized in $openJMLDir")
}
