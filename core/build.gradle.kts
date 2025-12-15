plugins {
    id("java-library")
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://jogamp.org/deployment/maven/")
    }
    maven {
        url = uri("https://www.jogamp.org/deployment/maven/")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

val os = org.gradle.internal.os.OperatingSystem.current()
val platform = when {
    os.isWindows -> "win"
    os.isMacOsX -> "mac"
    os.isLinux -> "linux"
    else -> "win" // fallback
}

dependencies {
    // MMOS API module - public SDK for external app developers
    api(project(":api"))

    // Processing 4 Core library (compatible with Forge 1.20.1 Java 17)
    implementation("org.processing:core:4.4.4")

    // Minim audio library for Processing
    implementation("net.compartmental.code:minim:2.2.2")

    // JSON processing library for layout persistence
    implementation("com.google.code.gson:gson:2.10.1")

    // JCEF (jcefmaven) - Chromium Embedded Frameworkの実装
    // coreモジュールでJCEFを一元管理し、standaloneとforgeの両方で使用
    // jcefmavenはネイティブライブラリの自動ダウンロード機能を提供
    implementation("me.friwi:jcefmaven:135.0.20")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.forkOptions.jvmArgs?.addAll(listOf("-Dfile.encoding=UTF-8", "-Dsun.jnu.encoding=UTF-8"))
}