plugins {
    id("java-library")
}

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    // Core module dependency (IPvMAddress, VirtualPacket等)
    implementation(project(":core"))

    // JSON processing library
    implementation("com.google.code.gson:gson:2.10.1")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.forkOptions.jvmArgs?.addAll(listOf("-Dfile.encoding=UTF-8", "-Dsun.jnu.encoding=UTF-8"))
}
