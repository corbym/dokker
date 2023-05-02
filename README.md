# dokker

Simple Kotlin docker builder for tests.

## What is Dokker?

Dokker is a lightweight kotlin wrapper around a docker container and allows you to build, start, stop, execute commands on and remove your docker containers.

## Who is it for?

If you want a container for tests but need a more Kotlin way to do things, Dokker provides an alternative to libraries like [TestContainers](https://www.testcontainers.org/).

# Requirements

## What software is required to use Dokker?

You must have docker command line installed on your system:

https://docs.docker.com/get-docker/

## How do I install Dokker?

Dokker is distributed through Maven Central.

### Maven
```
<dependency>
  <groupId>io.github.corbym</groupId>
  <artifactId>dokker</artifactId>
  <version>0.0.1</version>
  <type>module</type>
  <scope>test</scope>
</dependency>
```
### Gradle
```
dependencies {
  testImplementation("io.github.corbym:dokker:0.0.1")
}
```

# Getting Started
## How do I create a Dokker container?

The dokker builder builds a docker command line and executes it via `java.lang.ProcessBuilder` process. This is encapsulated in a [DokkerContainer](https://github.com/corbym/dokker/blob/main/src/main/kotlin/io/github/corbym/dokker/dokker.kt#L271:~:text=271-,class%20DokkerContainer(,-274)) class.
e.g:

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
Calling `start()` on the resulting object attempts to `docker run` the container using `ProcessBuilder` to run the `docker` command line client.

Calling `stop()` on the resulting object attempts to `docker stop` the container using `ProcessBuilder` to run the `docker` command line client.

Calling `remove()` on the resulting object attempts to `docker rm` the container using `ProcessBuilder` to run the `docker` command line client.


`DokkerContainer` implements `DokkerLifeCycle` so you can manage containers in sets.

You can use the [`DockerContainer::String.runCommand`](https://github.com/corbym/dokker/blob/main/src/main/kotlin/io/github/corbym/dokker/dokker.kt#L271:~:text=271-,class%20DokkerContainer(,-274)) independently too:
e.g:
```
"docker ps".runCommand()`
```
## Access the command that was run

You can get the parameters that were used to run the docker command, e.g:
```
val containerPublishedPorts = myContainer.publishedPorts
val exposedPorts = myContainer.expose
val image = myContainer.image

.. etc ..
```

## Execute Health Check
Executes the health check given in the configuration with an initial delay, timeout and polling interval. E.g.

```kotlin
val dockerContainer = dokker {
    healthCheck {
      timeout(Duration.ofSeconds(30))
      pollingInterval(TEN_SECONDS)
      initialDelay(TEN_SECONDS)
      checking { "curl -i --fail http://localhost:8080/health?ready=1" to "HTTP/1.1 200 OK" }
    }
    onStartup { container, _ ->
      .. commands to run on docker etc..
      container.executeHealthCheck()
    }
}

```
Note: this does *not* use docker's built in healthCheck functionality.

## Other configuration

The `DokkerContainer` object reflects most of the commands you can execute on the command line, including:

* start
* stop
* exec
* execWithSpacedParameter - this allows you to pass ProcessBuilder a command parameter which may contain spaces, and is a workaround really.  

# DokkerNetwork
Running a network in docker is slightly different to starting a container. To help with this, a there is a special object for a network called [`DokkerNetwork`](https://github.com/corbym/dokker/blob/main/src/main/kotlin/io/github/corbym/dokker/dokker.kt#L271:~:text=364-,class%20DokkerNetwork(private%20val%20networkName%3A%20String)%20%3A%20DokkerLifecycle%20%7B,-Search%20for%20this):
```kotlin 
val myNetwork = DokkerNetwork("some-network").also { it.start() }
```
`DokkerNetwork` also implements the `DokkerLifeCycle` so it can be managed with containers (see below).

# DokkerLifecycle 

Every [`DokkerContainer`](https://github.com/corbym/dokker/blob/main/src/main/kotlin/io/github/corbym/dokker/dokker.kt#L271:~:text=271-,class%20DokkerContainer(,-274)) and [`DokkerNetwork`](https://github.com/corbym/dokker/blob/main/src/main/kotlin/io/github/corbym/dokker/dokker.kt#L271:~:text=364-,class%20DokkerNetwork(private%20val%20networkName%3A%20String)%20%3A%20DokkerLifecycle%20%7B,-Search%20for%20this) implements the [`DokkerLifecycle`](https://github.com/corbym/dokker/blob/main/src/main/kotlin/io/github/corbym/dokker/dokker.kt#L263:~:text=263-,interface%20DokkerLifecycle%20%7B,-275) interface:

```kotlin
interface DokkerLifecycle {
    val name: String
    fun start()
    fun stop()
    fun remove()
    fun hasStarted(): Boolean
}
```

# Junit5 Extension
You can extend a test with the junit5 `@ExtendWith` annotation and create your own `DokkerProvider`.

See [ExampleDokkerProvider.kt](src/test/kotlin/io/github/corbym/junit5/ExampleDokkerProvider.kt) for more details.

## Lifecycle

The docker container provider lifecycle is as follows:

BeforeAll:

1. Start up validation
2. Run the container
3. Start up execution

JVM Shutdown:

4. Tear down container

## Start up validation

If the container exists but is not running, the framework will not start it and error.
In this case, you should remove the container manually.

If the container with the same name is already running, the framework will not try to start the container, and will not try to
stop it when the JVM exits.

The principle being, "if its not mine, don't touch it."

## Run the container

The framework will attempt to run the container with `docker run`.

## Start up execution

After the container has been run, any executions you specify are performed. You can specify this with

```kotlin
onStartup { container, _ ->
    it.waitForHealthCheck()
}
```
## Tear down

The containers are only torn down when the JVM executes a shutdown hook.

If the container fails to start or was already running with the same container name, the framework will NOT run the shutdown hook, and leave the container up.

# Contributing
## How can I contribute to Dokker?

Please open an issue or fork and a PR for any changes you wish to be considered.
