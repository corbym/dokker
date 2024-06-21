package io.github.corbym.dokker

import java.io.BufferedReader
import java.time.Instant

class DokkerContainer(
    private val dokkerRunCommandBuilder: DokkerRunCommandBuilder,
    val healthCheck: HealthCheck?,
    var onStart: (dokker: DokkerContainer, runResponse: String) -> Unit = { _, _ -> },
) : DokkerLifecycle, DokkerProperties by dokkerRunCommandBuilder {
    private val withPublishedPorts = if (publishedPorts.isNotEmpty()) " with published ports $publishedPorts" else ""

    override fun start() {
        if (!hasStarted()) {
            debug("starting container: $name with exposed ports $expose$withPublishedPorts ")
            checkContainerStopped()
            onStart(this, dokkerRunCommandBuilder.buildRunCommand().runCommand())
        } else {
            waitForHealthCheck()
        }
    }

    private fun checkContainerStopped(errorMessage: String = "Container $name is already stopped and won't be restarted.") {
        val response = "$process container ls --all --filter name=^/$name$ --format '{{.Status}}'".runCommand()
        if (response.contains("Exited")) {
            error(
                """$errorMessage
                |Run `$process rm $name` to remove it.
                """.trimMargin()
            )
        }
    }

    override fun hasStarted(): Boolean {
        return "$process ps --filter name=^/$name\$".runCommand().contains(name)
    }

    fun exec(command: String, parameter: String? = null, fail: Boolean = false): String =
        "$process exec -i $name $command".runCommand(parameter = parameter, fail)

    fun waitForHealthCheck() {
        val (timeout, pollingInterval, initialDelay, healthCheck) = healthCheck
            ?: error("waitForHealthCheck called when none specified in builder")

        debug("health check: starting in $initialDelay every $pollingInterval, for at lease $timeout")
        debug("executing: ${healthCheck.first}")

        val start = Instant.now().toEpochMilli()
        awaitUntil(
            timeout = timeout,
            initialDelay = initialDelay,
            pollInterval = pollingInterval,
        ) {
            debug("waited ${Instant.now().toEpochMilli() - start}")
            checkContainerStopped("Container $name failed to start properly. Run `$process logs $name` to check why.")
            val response = exec(command = healthCheck.first, fail = false)
            response.contains(healthCheck.second)
        }
    }

    override fun stop() {
        debug("stopping container: $name")
        "$process stop $name".runCommand(fail = false)
    }

    override fun remove() {
        debug("removing container: $name")
        "$process rm $name".runCommand(fail = false)
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
        internal fun debug(info: String) {
            if (debug) println(info)
        }

        fun String.runCommand(parameter: String? = null, fail: Boolean = true): String {
            val command = if (parameter != null) "$this $parameter" else this
            val processBuilder = ProcessBuilder("sh", "-c", command)

            debug("> ${processBuilder.command()}")
            val proc = processBuilder.start()

            val result = proc.waitFor()
            val errorResponse = proc.errorStream.bufferedReader().use(BufferedReader::readText)
            return if (result != 0 && fail) {
                error("[$result] could not run dokker command ${processBuilder.command()}: $errorResponse")
            } else if (result != 0) {
                debug("[$result] could not run dokker command ${processBuilder.command()}: $errorResponse")
                errorResponse
            } else {
                proc.inputStream.bufferedReader().use(BufferedReader::readText).trim()
                    .also { debug("> $it") }
            }
        }
    }
}