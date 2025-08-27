allprojects {
    group = "com.yourname.phoneos"
    version = "1.0-SNAPSHOT"
}

subprojects {
    apply(plugin = "java")
    
    repositories {
        mavenCentral()
        // JOGL repository for Processing 4 dependencies
        maven {
            url = uri("https://jogamp.org/deployment/maven/")
        }
        // Backup repository for JOGL
        maven {
            url = uri("https://www.jogamp.org/deployment/maven/")
        }
    }
    
    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}