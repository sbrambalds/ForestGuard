plugins {
    id("java")
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

javafx {
    version = "21"
    modules = listOf("javafx.controls", "javafx.fxml")
}

dependencies {
    implementation("io.github.jason-lang:jason-interpreter:3.2.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
}

file(projectDir).listFiles().filter { it.extension == "mas2j" }.forEach { mas2jFile ->
    tasks.register<JavaExec>("run${mas2jFile.nameWithoutExtension.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}Mas") {
        group = "run"
        classpath = sourceSets.getByName("main").runtimeClasspath
        mainClass.set("jason.infra.centralised.RunCentralisedMAS")
        args(mas2jFile.path)
        standardInput = System.`in`
    }
}

tasks.test {
    useJUnitPlatform()
}
