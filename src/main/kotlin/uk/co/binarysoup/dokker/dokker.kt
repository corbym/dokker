package uk.co.binarysoup.dokker

import org.awaitility.Durations
import org.awaitility.Durations.ONE_SECOND
import org.awaitility.kotlin.*
import uk.co.binarysoup.dokker.DokkerContainer.Companion.runCommand
import uk.co.binarysoup.dokker.OptionType.*
import java.io.BufferedReader
import java.time.Duration
import java.time.Instant.now
import java.util.*

fun dokker(init: DockerContainerBuilder.() -> Unit): DokkerContainer {
    return DockerContainerBuilder().apply(init).build()
}

@Suppress("unused")
class DockerContainerBuilder {
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
            DockerCommand(
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
    private var pollingInterval: Duration = ONE_SECOND
    private var initialDelay: Duration = ONE_SECOND
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

data class Option(val type: OptionType, val enabled: Boolean)

enum class OptionType(val option: String) {
    DETACH("d"),
    INTERACTIVE("i")
}

class DockerCommand(
    val name: String,
    val networks: List<String>,
    var expose: List<String>,
    var env: List<String>,
    var publishedPorts: List<String>,
    var image: String,
    var version: String? = null,
    var command: String? = null,
    var memory: String? = null,
    var hostname: String? = null,
    var user: String? = null,
    vararg val options: Option,
) {
    fun buildRunCommand(): String = "docker run${buildOptions()} ${buildImage()}${command?.prefix(" ") ?: ""}"
    private fun buildOptions() = listOf(
        listOf(buildFlags()),
        buildName(),
        buildExpose(),
        buildPorts(),
        buildEnv(),
        buildNetworks(),
        buildMemoryLimit(),
        buildHostName(),
        buildUser(),
    ).flatten()
        .filter { it.isNotEmpty() }
        .joinToString(separator = " ", prefix = " ")

    private fun buildMemoryLimit(): List<String> =
        if (memory != null)
            listOf("--memory $memory")
        else emptyList()

    private fun buildHostName(): List<String> =
        if (hostname != null)
            listOf("--hostname $hostname")
        else emptyList()

    private fun buildImage() = "$image${version?.prefix(":") ?: ""}"

    private fun buildExpose(): List<String> = expose.map { "--expose $it" }
    private fun buildEnv(): List<String> = env.map { "--env $it" }

    private fun buildName(): List<String> = listOf("--name $name")

    private fun buildNetworks(): List<String> = networks.map { "--network $it" }

    private fun buildFlags(): String {
        val enabledOptions = options.filter { it.enabled }
        return if (enabledOptions.isNotEmpty()) enabledOptions.joinToString(
            prefix = "-",
            separator = ""
        ) { it.type.option } else ""
    }

    private fun buildPorts() = publishedPorts.map { "-p $it" }

    private fun buildUser(): List<String> = if (user != null) listOf("--user $user") else emptyList()
}

private fun String?.prefix(prefix: String): String = "$prefix$this"

interface DockerLifecycle {
    val name: String
    fun start()
    fun stop()
    fun remove()
    fun hasStarted(): Boolean
}

class DokkerContainer(
    val dockerCommand: DockerCommand,
    val healthCheck: HealthCheck?,
    var onStart: (dokker: DokkerContainer, runResponse: String) -> Unit = { _, _ -> },
) : DockerLifecycle {
    override val name = dockerCommand.name
    override fun start() {
        if (!hasStarted()) {
            debug("starting docker container $name")
            checkContainerStopped()
            onStart(this, dockerCommand.buildRunCommand().runCommand())
        }
    }

    private fun checkContainerStopped(errorMessage: String = "Container $name is already stopped and won't be restarted.") {
        val response = "docker container ls -a -f name=$name --format '{{.Status}}'".runCommand()
        if (response.contains("Exited")) {
            error(
                """$errorMessage
                |Run `docker rm $name` to remove it.
                """.trimMargin()
            )
        }
    }

    override fun hasStarted(): Boolean {
        return "docker ps --filter name=$name".runCommand().contains(name)
    }

    fun exec(command: String, parameter: String? = null, fail: Boolean = false): String =
        "docker exec $name $command".runCommand(parameter = parameter, fail)

    fun waitForHealthCheck() {
        val (timeout, pollingInterval, initialDelay, healthCheck) = healthCheck
            ?: error("waitForHealthCheck called when none specified in builder")

        debug("health check: starting in $initialDelay every $pollingInterval, for at lease $timeout")
        debug("executing: ${healthCheck.first}")

        val start = now().toEpochMilli()
        await atMost timeout withPollInterval pollingInterval withPollDelay initialDelay until {
            debug("waited ${now().toEpochMilli() - start}")
            checkContainerStopped("Container $name failed to start properly. Run `docker logs $name` to check why.")
            val response = exec(command = healthCheck.first, fail = false)
            response.contains(healthCheck.second)
        }
    }

    override fun stop() {
        "docker stop $name".runCommand(fail = false)
    }

    override fun remove() {
        "docker rm $name".runCommand(fail = false)
    }

    fun exec(command: () -> String) {
        this.exec(command = command(), fail = false)
    }

    fun execWithSpacedParameter(command: () -> Pair<String, String?>) {
        val commandLine = command()
        this.exec(command = commandLine.first, parameter = commandLine.second, fail = false)
    }

    companion object {
        var debug = false
        private fun debug(info: String) {
            if (debug) println(info)
        }

        fun String.runCommand(parameter: String? = null, fail: Boolean = true): String {
            val commandLine = split(" ").toMutableList()
            if (parameter != null) commandLine.add(parameter)
            val processBuilder = ProcessBuilder(commandLine)
            debug("> ${processBuilder.command()}")
            val proc = processBuilder.start()

            val result = proc.waitFor()
            val errorResponse = proc.errorStream.bufferedReader().use(BufferedReader::readText)
            return if (result != 0 && fail) {
                error("[$result] could not run docker command ${processBuilder.command()}: $errorResponse")
            } else if (result != 0) {
                debug("[$result] could not run docker command ${processBuilder.command()}: $errorResponse")
                errorResponse
            } else {
                proc.inputStream.bufferedReader().use(BufferedReader::readText)
                    .also { debug("> $it") }
            }
        }
    }
}

class DockerNetwork(private val networkName: String) : DockerLifecycle {
    override val name = networkName
    override fun start() {
        if (!hasStarted())
            "docker network create $networkName".runCommand()
    }

    override fun stop() {
        // no stop on networks
    }

    override fun remove() {
        await atMost Durations.ONE_MINUTE until {
            "docker network rm $networkName".runCommand() == networkName
        }
    }

    override fun hasStarted(): Boolean = "docker network ls".runCommand().contains(networkName)
}
