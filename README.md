[![Maven Central](https://img.shields.io/maven-central/v/io.github.corbym/dokker?color=4caf50&label=latest%20release)](https://maven-badges.herokuapp.com/maven-central/io.github.corbym/dokker)
[![Build Status](https://github.com/corbym/dokker/actions/workflows/build-dokker-project.yml/badge.svg)](https://github.com/corbym/dokker/actions/workflows/build-dokker-project.yml)

# Dokker

Simple Kotlin Docker builder for tests.

## What is Dokker?

Dokker is a lightweight Kotlin wrapper around the Docker command line that lets you build, start, stop, execute commands on, and remove Docker containers from your tests.

If you want a container for tests but prefer a Kotlin-idiomatic API, Dokker provides an alternative to libraries like [TestContainers](https://www.testcontainers.org/).

## Requirements

You must have the Docker command line installed on your system:

https://docs.docker.com/get-docker/

## Installation

Dokker is distributed through Maven Central.

### Maven

```xml
<dependency>
  <groupId>io.github.corbym</groupId>
  <artifactId>dokker</artifactId>
  <version>0.5.2</version>
  <scope>test</scope>
</dependency>
```

### Gradle

```kotlin
dependencies {
  testImplementation("io.github.corbym:dokker:0.5.2")
}
```

## Getting Started

### Creating a container

The `dokker { }` builder constructs a Docker command line and executes it via `java.lang.ProcessBuilder`. It returns a [`DokkerContainer`](https://github.com/corbym/dokker/blob/main/src/main/kotlin/io/github/corbym/dokker/DokkerContainer.kt):

```kotlin
val myContainer = dokker {
    name("my-container")
    detach()
    expose("9092", "29092", "9101")
    image { "my/container" }
    version { "1.1" }
    publish("12300" to "12300")
    network("local-network")
    env(
        "MY_CONFIG_PROP" to "hello"
    )
}
```

### Starting, stopping, and removing

| Method | Docker command |
|---|---|
| `myContainer.start()` | `docker run` |
| `myContainer.stop()` | `docker stop` |
| `myContainer.remove()` | `docker rm` |

### Accessing container properties

You can read back the configuration used to run the container:

```kotlin
val publishedPorts = myContainer.publishedPorts
val exposedPorts = myContainer.expose
val image = myContainer.image
// etc.
```

### Health checks

Dokker can poll a command until an expected response is returned, with a configurable initial delay, polling interval, and timeout. Note that this polls via `exec` inside the container and does **not** use Docker's built-in health check functionality.

```kotlin
val myContainer = dokker {
    healthCheck {
        timeout(Duration.ofSeconds(30))
        pollingInterval(Duration.ofSeconds(10))
        initialDelay(Duration.ofSeconds(10))
        checking { "curl -i --fail http://localhost:8080/health?ready=1" to "HTTP/1.1 200 OK" }
    }
    onStartup { container, _ ->
        container.waitForHealthCheck()
    }
}
```

### Running arbitrary commands

You can run commands against a container with `exec`:

```kotlin
myContainer.exec("some-command")
```

Use `execWithSpacedParameter` when a command argument contains spaces (a limitation of `ProcessBuilder`):

```kotlin
myContainer.execWithSpacedParameter { "some-command" to "argument with spaces" }
```

You can also run any Docker command directly using the [`String.runCommand`](https://github.com/corbym/dokker/blob/main/src/main/kotlin/io/github/corbym/dokker/DokkerContainer.kt) extension:

```kotlin
"docker ps".runCommand()
```

## DokkerNetwork

[`DokkerNetwork`](https://github.com/corbym/dokker/blob/main/src/main/kotlin/io/github/corbym/dokker/DokkerNetwork.kt) manages a Docker network. It requires the container runtime process name and the network name. Use `DokkerProcessName.processName` to pick up whichever process is configured (see [Podman support](#podman-support)):

```kotlin
val myNetwork = DokkerNetwork(DokkerProcessName.processName, "some-network").also { it.start() }
```

`DokkerNetwork` implements [`DokkerLifecycle`](https://github.com/corbym/dokker/blob/main/src/main/kotlin/io/github/corbym/dokker/DokkerLifecycle.kt) and can be managed alongside containers.

## DokkerLifecycle

Both [`DokkerContainer`](https://github.com/corbym/dokker/blob/main/src/main/kotlin/io/github/corbym/dokker/DokkerContainer.kt) and [`DokkerNetwork`](https://github.com/corbym/dokker/blob/main/src/main/kotlin/io/github/corbym/dokker/DokkerNetwork.kt) implement the [`DokkerLifecycle`](https://github.com/corbym/dokker/blob/main/src/main/kotlin/io/github/corbym/dokker/DokkerLifecycle.kt) interface, which allows them to be managed together in sets:

```kotlin
interface DokkerLifecycle {
    val name: String
    fun start()
    fun stop()
    fun remove()
    fun hasStarted(): Boolean
}
```

## JUnit 5 Integration

### DokkerProvider extension

You can implement `DokkerProvider` and use it with JUnit 5's `@ExtendWith` annotation. The container lifecycle is managed automatically:

| Phase | Action |
|---|---|
| BeforeAll | Validate, run the container, execute startup commands |
| JVM Shutdown | Tear down the container |

**Startup validation:** if a container with the same name exists but is stopped, Dokker will error rather than restart it — remove the container manually first. If the container is already running, Dokker leaves it alone and will not stop it on shutdown ("if it's not mine, don't touch it").

See [ExampleDokkerProvider.kt](src/test/kotlin/io/github/corbym/junit5/ExampleDokkerProvider.kt) for a full example.

### DokkerExtension

`DokkerExtension` supports the JUnit 5 `@RegisterExtension` annotation. It starts the container in `BeforeAll` and stops and removes it in `AfterAll`:

```kotlin
import io.github.corbym.dokker.junit5.DokkerExtension
import io.github.corbym.dokker.junit5.dokkerExtension
import io.github.corbym.dokker.junit5.findFreePort
import org.junit.jupiter.api.extension.RegisterExtension

class ExampleJUnit5RegisteredDokkerTest {
    companion object {
        val couchbasePort = findFreePort()

        @JvmStatic
        @RegisterExtension
        val server: DokkerExtension = dokkerExtension {
            container {
                dokker {
                    name("couchbase")
                    detach()
                    debug()
                    expose(couchbasePort)
                    publish(couchbasePort to couchbasePort)
                    image { "arungupta/couchbase" }
                    version { "latest" }
                }
            }
            doNotStop()
        }
    }
    // ... rest of test ...
}
```

`findFreePort()` is a utility function that returns a random available port as a `String`. You can prevent shutdown and removal by calling `doNotStop()` and `doNotRemove()` in the `dokkerExtension { }` builder.

See [`ExampleJUnit5RegisteredDokkerTest`](src/test/kotlin/io/github/corbym/junit5/ExampleJUnit5RegisteredDokkerTest.kt) for a full example.

## Podman support

Dokker supports alternative container runtimes such as Podman. The process used can be configured in three ways (in order of precedence):

1. In code: `dokker { process("podman") }`
2. Via the environment variable `DOKKER_PROCESS`
3. Default: `docker`

## Contributing

Please open an issue or fork the repository and open a PR for any changes you wish to be considered.
