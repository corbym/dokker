group = "io.github.corbym"
version = "0.2.3"
description = "dokker: Simple Kotlin docker builder for tests."

plugins {
    kotlin("jvm") version "1.8.0"
    `maven-publish`
    id("com.dipien.semantic-version") version "2.0.0" apply false
    signing
}

signing {
    useInMemoryPgpKeys(
        System.getenv("ORG_GRADLE_PROJECT_signingKey"),
        System.getenv("ORG_GRADLE_PROJECT_signingPassword")
    )

    setRequired({
        gradle.taskGraph.hasTask("publish")
    })
    sign(publishing.publications)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    withJavadocJar()
    withSourcesJar()
}

tasks.create("applySemanticVersionPlugin") {
    dependsOn("prepareKotlinBuildScriptModel")
    apply(plugin = "com.dipien.semantic-version")
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
    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                artifactId = "dokker"
                from(components["java"])
                versionMapping {
                    usage("java-api") {
                        fromResolutionOf("runtimeClasspath")
                    }
                    usage("java-runtime") {
                        fromResolutionResult()
                    }
                }
                pom {
                    name.set(project.name)
                    description.set(project.description)
                    url.set("https://github.com/corbym/dokker")
                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://github.com/corbym/dokker/blob/main/LICENSE")
                        }
                    }
                    developers {
                        developer {
                            id.set("corbym")
                            name.set("Matt Corby-Eaglen")
                            email.set("matt.corby@gmail.com")
                        }
                    }
                    scm {
                        connection.set("scm:git:git@github.com:corbym/dokker.git")
                        developerConnection.set("scm:git:ssh://github.com:corbym/dokker.git")
                        url.set("https://github.com/corbym/dokker")
                    }
                }
            }
        }
    }
    repositories {
        maven {
            name = "OSSRH"
            val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/service/local/snapshot/deploy/maven2/")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
            credentials {
                username = System.getenv("MAVEN_USERNAME")
                password = System.getenv("MAVEN_PASSWORD")
            }
        }
    }
}

