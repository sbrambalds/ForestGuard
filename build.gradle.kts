plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.jason-lang:jason-interpreter:3.2.1")
    testImplementation("junit", "junit", "4.13.2")
}

file(projectDir).listFiles().filter { it.extension == "mas2j" }.forEach { mas2jFile ->
    tasks.register<JavaExec>("run${mas2jFile.nameWithoutExtension.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}Mas") {
        group = "run"
        classpath = sourceSets.getByName("main").runtimeClasspath
        mainClass.set("jason.infra.centralised.RunCentralisedMAS")
        args(mas2jFile.path)
        standardInput = System.`in`
        javaLauncher.set(javaToolchains.launcherFor(java.toolchain))
    }
}

tasks.test {
    useJUnitPlatform()
}