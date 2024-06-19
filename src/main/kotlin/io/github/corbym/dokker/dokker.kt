package io.github.corbym.dokker

import io.github.corbym.dokker.DokkerContainer.Companion.runCommand
import io.github.corbym.dokker.OptionType.DETACH
import io.github.corbym.dokker.OptionType.INTERACTIVE
import java.time.Duration
import java.util.*

fun dokker(init: DokkerContainerBuilder.() -> Unit): DokkerContainer {
    return DokkerContainerBuilder().apply(init).build()
}

// Search order is
// 1. process name set in code (if set, DokkerAutoProcessSearchResult.processName is not used)
// 2. environment variable (if found in path)
// 3. hardcoded "docker" (if found in path)
// 3. hardcoded "podman" (if found in path)
object DokkerAutoProcessSearchResult {
    private fun checkEnvVar(): String? {
        return try {
            System.getenv("DOKKER_PROCESS")
        } catch (e: Exception) {
            println(e)
            null
        }
    }

    // Note that "command -v" - this is best-effort as adding an 'alias' will cause issues
    // MacOS defaults to zsh shell.
    //   Intellij does not read ~/.zprofile nor ~/.zshrc scripts so set environment variables in ~/.profile
    //   Otherwise, "command -v" may not find the executable
    private fun processFullPath(process: String): String? {
        return try {
            "command -v $process".runCommand()
        } catch (e: Exception) {
            println(e)
            null
        }
    }

    // This name is in an object so that we do this once per process, as this check is expensive
    val processName: String = run {
        val configured = checkEnvVar()
        try {
            listOf(configured, "docker", "podman").firstNotNullOf { it?.let { processFullPath(it) } }
        } catch (ex: NoSuchElementException) {
            throw Exception("""
                Unable to find an underlying process executable.
                Please make sure that any of the supporting application process is available and on the PATH
                1. Current env configured DOKKER_PROCESS: $configured
                2. docker
                3. podman
            """.trimIndent())
        }
    }
}

@Suppress("unused")
class DokkerContainerBuilder() {
    private var onStart: (DokkerContainer, String) -> Unit = { _, _ -> }
    private var process: String? = null
    private var name: String = "dockerContainer-${UUID.randomUUID()}"
    private val networks: MutableList<String> = mutableListOf()
    private var expose: MutableList<String> = mutableListOf()
    private var env: MutableList<String> = mutableListOf()
    private var publishedPorts: MutableList<String> = mutableListOf()
    private var image: String? = null
    private var version: String? = null
    private var command: String? = null
    private var memory: String? = null
    private var hostname: String? = null
    private var user: String? = null

    private var healthCheck: HealthCheck? = null

    private var interactive: Boolean = false
    private var detach: Boolean = false

    private var debug: Boolean = false

    fun process(process: String) {
        this.process = process
    }

    fun name(name: String) {
        this.name = name
    }

    fun name(name: () -> String) {
        name(name())
    }

    fun detach() {
        this.detach = true
    }

    fun image(function: () -> String) {
        image(function())
    }

    fun image(image: String) {
        this.image = image
    }

    fun version(function: () -> String) {
        version(function())
    }

    fun version(version: String) {
        this.version = version
    }

    fun command(function: () -> String) {
        this.command = function()
    }

    fun expose(vararg expose: String) {
        expose.forEach { this.expose.add(it) }
    }

    fun publish(vararg publish: Pair<String, String>) {
        publish.forEach {
            this.publishedPorts.add("${it.first}:${it.second}")
        }
    }

    fun network(vararg networks: String) {
        this.networks.addAll(networks)
    }

    fun env(vararg pair: Pair<String, String>) {
        pair.forEach { (first, second) ->
            this.env.add("$first=$second")
        }
    }

    fun memory(limit: String) {
        this.memory = limit
    }

    fun memory(limit: () -> String) {
        memory(limit())
    }

    fun onStartup(onStart: (DokkerContainer, String) -> Unit) {
        this.onStart = onStart
    }

    fun healthCheck(init: HealthCheckBuilder.() -> Unit) {
        this.healthCheck = HealthCheckBuilder().apply(init).build()
    }

    fun build(): DokkerContainer {

        return DokkerContainer(
            DokkerRunCommandBuilder(
                process = process?: DokkerAutoProcessSearchResult.processName,
                name = name,
                networks = networks,
                expose = expose,
                env = env,
                publishedPorts = publishedPorts,
                image = image!!,
                version = version,
                command = command,
                memory = memory,
                hostname = hostname,
                user = user,
                Option(DETACH, detach),
                Option(INTERACTIVE, interactive)
            ),
            healthCheck = healthCheck,
            onStart = onStart
        ).also {
            DokkerContainer.debug = debug
        }
    }

    fun hostname(hostname: String) {
        this.hostname = hostname
    }

    fun debug() {
        debug = true
    }

    fun user(user: String) {
        this.user = user
    }
}

data class HealthCheck(
    val timeout: Duration,
    val pollingInterval: Duration,
    val initialDelay: Duration,
    val check: Pair<String, String>,
)

class HealthCheckBuilder {
    private var healthCheck: Pair<String, String>? = null
    private var pollingInterval: Duration = Duration.ofSeconds(1)
    private var initialDelay: Duration = Duration.ofSeconds(1)
    private var timeout: Duration = initialDelay.multipliedBy(5)

    fun pollingInterval(pollingInterval: Duration) {
        this.pollingInterval = pollingInterval
    }

    fun checking(value: () -> Pair<String, String>) {
        healthCheck = value()
    }

    fun checking(value: Pair<String, String>) {
        healthCheck = value
    }

    fun pollingInterval(pollingInterval: () -> Duration) {
        pollingInterval(pollingInterval())
    }

    fun initialDelay(initialDelay: Duration) {
        this.initialDelay = initialDelay
    }

    fun timeout(timout: Duration) {
        this.timeout = timout
    }

    fun initialDelay(initialDelay: () -> Duration) {
        initialDelay(initialDelay())
    }

    fun build(): HealthCheck {
        return HealthCheck(
            timeout,
            pollingInterval,
            initialDelay,
            healthCheck ?: error("nothing specified to check in healthCheck")
        )
    }
}
