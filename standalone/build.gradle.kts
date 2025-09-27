plugins {
    application
    id("java-library")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
    // Dependency on the core module
    implementation(project(":core"))

    // Processing 4 Core library (needed for Main.java)
    implementation("org.processing:core:4.4.4")

    // Additional Processing dependencies that might be needed
    implementation("org.jogamp.gluegen:gluegen-rt:2.4.0")
    implementation("org.jogamp.jogl:jogl-all:2.4.0")

    // Logging dependencies
    implementation("org.apache.logging.log4j:log4j-api:2.20.0")
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.20.0")
}

// Configuration for the main class
application {
    mainClass.set("jp.moyashi.phoneos.standalone.Main")
}

// Shadow JAR configuration for fat jar with all dependencies
tasks.shadowJar {
    archiveClassifier.set("")
    mergeServiceFiles()

    // Main class for executable jar
    manifest {
        attributes(
            "Main-Class" to "jp.moyashi.phoneos.standalone.Main",
            "Implementation-Title" to "MochiMobileOS Standalone",
            "Implementation-Version" to project.version
        )
    }

    // Exclude conflicting files
    exclude("META-INF/*.DSA", "META-INF/*.RSA", "META-INF/*.SF")
    exclude("module-info.class")

    // Handle duplicate files
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Make build task depend on shadowJar
tasks.build {
    dependsOn(tasks.shadowJar)
}

// Fix distribution task dependencies
tasks.distZip {
    dependsOn(tasks.shadowJar)
}

tasks.distTar {
    dependsOn(tasks.shadowJar)
}

tasks.startScripts {
    dependsOn(tasks.shadowJar)
}

// Fix startShadowScripts dependency
tasks.named("startShadowScripts") {
    dependsOn(tasks.jar)
}

// Configure jar task to create a runnable jar
tasks.jar {
    // Include core module classes
    from(project(":core").sourceSets.main.get().output)

    manifest {
        attributes(
            "Main-Class" to "jp.moyashi.phoneos.standalone.Main",
            "Class-Path" to configurations.runtimeClasspath.get().joinToString(" ") { it.name }
        )
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Create a task for creating a distribution with all dependencies
tasks.register<Copy>("createDist") {
    dependsOn(tasks.shadowJar)
    from(tasks.shadowJar.get().archiveFile)
    into(layout.buildDirectory.dir("dist"))
    rename { "MochiMobileOS-Standalone.jar" }
}

// Java compatibility
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}