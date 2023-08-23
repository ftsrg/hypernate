plugins {
  `kotlin-dsl`
}

repositories {
  mavenCentral()
}

dependencies {
  implementation(gradleApi())
  implementation("net.lingala.zip4j:zip4j:2.11.5")
}

group = "hu.bme.mit.ftsrg"
version = "0.1.0"
