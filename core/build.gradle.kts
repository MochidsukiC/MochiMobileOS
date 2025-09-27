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

dependencies {
    // Processing 4 Core library (compatible with Forge 1.20.1 Java 17)
    implementation("org.processing:core:4.4.4")
    
    // Minim audio library for Processing
    implementation("net.compartmental.code:minim:2.2.2")
    
    // JSON processing library for layout persistence
    implementation("com.google.code.gson:gson:2.10.1")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.forkOptions.jvmArgs?.addAll(listOf("-Dfile.encoding=UTF-8", "-Dsun.jnu.encoding=UTF-8"))
}