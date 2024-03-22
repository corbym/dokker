package io.github.corbym.dokker

import io.github.corbym.dokker.OptionType.DETACH
import io.github.corbym.dokker.OptionType.INTERACTIVE
import java.time.Duration
import java.util.*

fun dokker(init: DokkerContainerBuilder.() -> Unit): DokkerContainer {
    return DokkerContainerBuilder().apply(init).build()
}

@Suppress("unused")
class DokkerContainerBuilder {
    private var onStart: (DokkerContainer, String) -> Unit = { _, _ -> }
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
