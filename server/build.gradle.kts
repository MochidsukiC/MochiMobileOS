plugins {
    application
    id("java-library")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
    // MMOS API module - public SDK
    implementation(project(":api"))

    // Dependency on the core module
    implementation(project(":core"))

    // Processing 4 Core library
    implementation("org.processing:core:4.4.4")

    // JOGAMP for OpenGL (Processing依存)
    implementation("org.jogamp.gluegen:gluegen-rt:2.4.0")
    implementation("org.jogamp.jogl:jogl-all:2.4.0")

    // JOGAMP native libraries for Windows
    val os = org.gradle.internal.os.OperatingSystem.current()
    if (os.isWindows) {
        runtimeOnly("org.jogamp.gluegen:gluegen-rt:2.4.0:natives-windows-amd64")
        runtimeOnly("org.jogamp.jogl:jogl-all:2.4.0:natives-windows-amd64")
    } else if (os.isMacOsX) {
        runtimeOnly("org.jogamp.gluegen:gluegen-rt:2.4.0:natives-macosx-universal")
        runtimeOnly("org.jogamp.jogl:jogl-all:2.4.0:natives-macosx-universal")
    } else if (os.isLinux) {
        runtimeOnly("org.jogamp.gluegen:gluegen-rt:2.4.0:natives-linux-amd64")
        runtimeOnly("org.jogamp.jogl:jogl-all:2.4.0:natives-linux-amd64")
    }

    // Logging dependencies
    implementation("org.apache.logging.log4j:log4j-api:2.20.0")
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.20.0")

    // JCEF (jcefmaven) - coreモジュールのJCEFChromiumProviderが使用
    implementation("me.friwi:jcefmaven:135.0.20")
    implementation("me.friwi:jcef-api:jcef-ca49ada+cef-135.0.20+ge7de5c3+chromium-135.0.7049.85")

    // JOGL 2.5.0 for JCEF's CefBrowserOsr
    implementation("org.jogamp.jogl:jogl-all-main:2.5.0")
    implementation("org.jogamp.gluegen:gluegen-rt-main:2.5.0")
}

// Configuration for the main class
application {
    mainClass.set("jp.moyashi.phoneos.server.MMOSServer")

    // JVM引数でメモリとGCを最適化
    applicationDefaultJvmArgs = listOf(
        "-Xmx4G",            // 最大ヒープサイズ4GB（Chromium用に十分）
        "-Xms512M",          // 初期ヒープサイズ512MB
        "-XX:+UseG1GC",      // G1 GCを使用（低レイテンシ）
        "-XX:MaxGCPauseMillis=50"  // GC停止時間を最大50msに制限
    )
}

// Shadow JAR configuration for fat jar with all dependencies
tasks.shadowJar {
    archiveClassifier.set("")
    mergeServiceFiles()

    // Main class for executable jar
    manifest {
        attributes(
            "Main-Class" to "jp.moyashi.phoneos.server.MMOSServer",
            "Implementation-Title" to "MochiMobileOS Server",
            "Implementation-Version" to project.version,
            "Add-Opens" to "java.base/java.lang",
            "Add-Exports" to "java.base/java.lang java.desktop/sun.awt java.desktop/sun.java2d"
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

// Configure jar task
tasks.jar {
    // Include core module classes
    from(project(":core").sourceSets.main.get().output)

    manifest {
        attributes(
            "Main-Class" to "jp.moyashi.phoneos.server.MMOSServer",
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
    rename { "MochiMobileOS-Server.jar" }
}

// Task to copy JOGAMP native libraries to libs directory for development
tasks.register<Copy>("copyJogampNatives") {
    val os = org.gradle.internal.os.OperatingSystem.current()
    val nativesClassifier = when {
        os.isWindows -> "natives-windows-amd64"
        os.isMacOsX -> "natives-macosx-universal"
        os.isLinux -> "natives-linux-amd64"
        else -> throw GradleException("Unsupported OS: ${os.name}")
    }

    doFirst {
        file("libs").mkdirs()
        println("Copying JOGAMP native libraries to libs/ for development environment")
    }

    from(configurations.runtimeClasspath.get().filter {
        it.name.contains("gluegen-rt") && it.name.contains(nativesClassifier)
    }.map { zipTree(it) })

    from(configurations.runtimeClasspath.get().filter {
        it.name.contains("jogl-all") && it.name.contains(nativesClassifier)
    }.map { zipTree(it) })

    into(file("libs"))
    include("**/*.dll", "**/*.so", "**/*.dylib", "**/*.jnilib")

    doLast {
        println("JOGAMP native libraries copied to libs/")
    }
}

// Make run task depend on copying JOGAMP natives
tasks.named<JavaExec>("run") {
    dependsOn("copyJogampNatives")

    // Set java.library.path to include native libraries directory
    val os = org.gradle.internal.os.OperatingSystem.current()
    val nativesPath = when {
        os.isWindows -> "libs/natives/windows-amd64"
        os.isMacOsX -> "libs/natives/macosx-universal"
        os.isLinux -> "libs/natives/linux-amd64"
        else -> "libs"
    }

    jvmArgs(
        "-Djava.library.path=$nativesPath",
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-exports=java.base/java.lang=ALL-UNNAMED",
        "--add-exports=java.desktop/sun.awt=ALL-UNNAMED",
        "--add-exports=java.desktop/sun.java2d=ALL-UNNAMED"
    )
}

// Java compatibility
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
