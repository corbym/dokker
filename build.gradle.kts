group = "io.github.corbym"

plugins {
    kotlin("jvm") version "1.8.0"
    `maven-publish`
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
    id("com.bnc.gradle.travis-ci-versioner") version "1.1.0"
}
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    withJavadocJar()
    withSourcesJar()
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "11"
    }
}
dependencies {
    implementation("org.awaitility:awaitility:4.2.0")
    implementation("org.awaitility:awaitility-kotlin:4.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.junit.jupiter:junit-jupiter-api:5.9.1")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.1")
}
tasks.test {
    useJUnitPlatform()
}

repositories {
    mavenCentral()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                groupId = project.group.toString()
                artifactId = project.name
                name.set(project.name)
                version = project.version.toString()
                description.set("dokker - a kotlin docker library")
                url.set("https://github.com/corbym/dokker")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("http://www.opensource.org/licenses/mit-license.php")
                    }
                }
                developers {
                    developer {
                        id.set("corbymatt")
                        name.set("Matt Corby-Eaglen")
                        email.set("matt.corby@gmail.com")
                    }
                }
                scm {
                    connection.set("scm:git:git@github.com:corbym/dokker.git")
                    url.set("https://github.com/corbym/dokker")
                }

            }
        }
    }
}
travisVersioner {
    major = 0
    minor = 0
    qualifiedBranch = "release"
}
if (System.getenv()["OSSRH_PASSWORD"] != null) {
    val sonatypePassword = System.getenv()["OSSRH_PASSWORD"]

    nexusPublishing {
        repositories {
            sonatype {
                nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
                snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
                username.set("corbymatt")
                password.set(sonatypePassword)
            }
        }
    }
}
