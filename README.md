# dokker
Simple Kotlin docker builder for tests.

## Requirements
You must have docker command line installed on your system before any of this will work.

https://docs.docker.com/get-docker/

## Installation 

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
## The kotlin dokker builder

The dokker builder builds a docker command line and executes it via `java.lang.ProcessBuilder` created process.
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
    healthCheck {
        pollingInterval(TEN_SECONDS)
      checking { "curl -i --fail http://localhost:8080/health?ready=1" to "HTTP/1.1 200 OK" }
    }
    onStartup { it: DockerContainer ->
      .. commands to run on docker etc..
      it.executeHealthCheck()
    }
}
  ..
    myContainer.start()
  ..
```

You can use the `DockerContainer::String.runCommand` independently too:
e.g:
```
"docker ps".runCommand()`
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
}
.. 
dockerContainer.executeHealthCheck()
..
```
## Other configuration

The builder reflects most of the commands you can execute on the command line, including:

* start
* stop
* exec
* execWithSpacedParameter - this allows you to pass ProcessBuilder a command parameter which may contain spaces, and is a workaround really.  

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
onStartup {
    it.waitForHealthCheck()
}
```
## Tear down

The containers are only torn down when the JVM executes a shutdown hook.

If the container fails to start or was already running with the same container name, the framework will NOT run the shutdown hook, and leave the container up.
