plugins {
    kotlin("jvm") version "2.0.0"
    `maven-publish`
    signing
}

group = "dev.averspec"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("reflect"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            groupId = project.group.toString()
            artifactId = "averspec"
            version = project.version.toString()

            pom {
                name.set("Aver for JVM")
                description.set("Domain-driven acceptance testing for Kotlin and the JVM")
                url.set("https://github.com/AverSpec/aver-jvm")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("averspec")
                        name.set("AverSpec Contributors")
                        url.set("https://github.com/AverSpec")
                    }
                }

                scm {
                    url.set("https://github.com/AverSpec/aver-jvm")
                    connection.set("scm:git:git://github.com/AverSpec/aver-jvm.git")
                    developerConnection.set("scm:git:ssh://git@github.com/AverSpec/aver-jvm.git")
                }
            }
        }
    }

    repositories {
        maven {
            name = "sonatype"
            val releasesUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsUrl else releasesUrl

            credentials {
                username = providers.environmentVariable("MAVEN_USERNAME").orNull
                password = providers.environmentVariable("MAVEN_PASSWORD").orNull
            }
        }
    }
}

signing {
    val signingKey = providers.environmentVariable("GPG_SIGNING_KEY").orNull
    val signingPassword = providers.environmentVariable("GPG_SIGNING_PASSWORD").orNull
    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications["mavenJava"])
    }
}
