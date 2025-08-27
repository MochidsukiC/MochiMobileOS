plugins {
    application
}

dependencies {
    // Dependency on the core module
    implementation(project(":core"))
    // Processing 4 Core library (needed for Main.java)
    implementation("org.processing:core:4.4.4")
}

// Configuration for the main class
application {
    mainClass.set("com.yourname.phoneos.standalone.Main")
}