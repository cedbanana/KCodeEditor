plugins {
    kotlin("jvm") version "2.1.10"
    id("org.openjfx.javafxplugin") version "0.0.13"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.fxmisc.richtext:richtextfx:0.11.0")
}

javafx {
    version = "17"
    modules = listOf("javafx.controls", "javafx.fxml")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(19)
}
application {
    mainClass.set("com.app.MainKt")
}