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

    // Apply maven-publish plugin to publishable subprojects (excluding forge - it has its own ForgeGradle publishing)
    if (name in listOf("core", "standalone")) {
        apply(plugin = "maven-publish")

        configure<PublishingExtension> {
            publications {
                create<MavenPublication>("maven") {
                    groupId = project.group.toString()
                    artifactId = project.name
                    version = project.version.toString()

                    from(components["java"])

                    pom {
                        name.set("MochiMobileOS ${project.name.capitalize()}")
                        description.set("MochiMobileOS ${project.name} module - A virtual mobile OS implementation")
                        url.set("https://github.com/MochidsukiC/MochiMobileOS")

                        licenses {
                            license {
                                name.set("MIT License")
                                url.set("https://opensource.org/licenses/MIT")
                            }
                        }

                        developers {
                            developer {
                                id.set("MochidsukiC")
                                name.set("MochidsukiC")
                                email.set("contact@moyashi.jp")
                            }
                        }

                        scm {
                            connection.set("scm:git:git://github.com/MochidsukiC/MochiMobileOS.git")
                            developerConnection.set("scm:git:ssh://github.com/MochidsukiC/MochiMobileOS.git")
                            url.set("https://github.com/MochidsukiC/MochiMobileOS")
                        }
                    }
                }
            }

            repositories {
                maven {
                    name = "GitHubPackages"
                    url = uri("https://maven.pkg.github.com/MochidsukiC/MochiMobileOS")
                    credentials {
                        username = project.findProperty("github.user") as String? ?: System.getenv("GITHUB_ACTOR")
                        password = project.findProperty("github.token") as String? ?: System.getenv("GITHUB_TOKEN")
                    }
                }

                // Optional: Local repository for testing
                maven {
                    name = "Local"
                    url = uri(layout.buildDirectory.dir("repo"))
                }
            }
        }
    }
}

// Task to publish all modules at once
tasks.register("publishAllModules") {
    description = "Publishes all modules to the configured repositories"
    group = "publishing"

    dependsOn(subprojects.mapNotNull { subproject ->
        if (subproject.name in listOf("core", "standalone", "forge")) {
            "${subproject.path}:publish"
        } else null
    })
}

// Task to publish all modules to GitHub Packages
tasks.register("publishAllToGitHub") {
    description = "Publishes all modules to GitHub Packages"
    group = "publishing"

    dependsOn(subprojects.mapNotNull { subproject ->
        if (subproject.name in listOf("core", "standalone", "forge")) {
            "${subproject.path}:publishMavenPublicationToGitHubPackagesRepository"
        } else null
    })
}

// Task to publish all modules to local repository (including forge)
tasks.register("publishAllToLocal") {
    description = "Publishes all modules to local repository"
    group = "publishing"

    dependsOn(subprojects.mapNotNull { subproject ->
        if (subproject.name in listOf("core", "standalone", "forge")) {
            "${subproject.path}:publishToMavenLocal"
        } else null
    })
}

