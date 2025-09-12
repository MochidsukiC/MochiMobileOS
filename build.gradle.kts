allprojects {
    group = "jp.moyashi.phoneos"
    version = "1.1-SNAPSHOT"
}

subprojects {
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
    
    // Apply Java plugin only if not already applied
    if (!plugins.hasPlugin("java-library") && !plugins.hasPlugin("java")) {
        apply(plugin = "java")
        
        configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
        
        tasks.withType<JavaCompile> {
            options.encoding = "UTF-8"
            options.forkOptions.jvmArgs?.addAll(listOf("-Dfile.encoding=UTF-8", "-Dsun.jnu.encoding=UTF-8"))
        }
    }
}

