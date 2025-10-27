group = "io.github.corbym"
version = "0.5.0"
description = "dokker: Simple Kotlin docker builder for tests."

plugins {
    kotlin("jvm") version "2.2.21"
    `maven-publish`
    id("com.dipien.semantic-version") version "2.0.0" apply false
    signing
}

apply(plugin = "kotlin")
kotlin {
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
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

tasks.register("applySemanticVersionPlugin") {
    dependsOn("prepareKotlinBuildScriptModel")
    apply(plugin = "com.dipien.semantic-version")
}

dependencies {
    compileOnly("org.junit.jupiter:junit-jupiter-api:6.0.0")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-api:6.0.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:6.0.0")
    testImplementation("org.hamcrest:hamcrest:3.0")
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
