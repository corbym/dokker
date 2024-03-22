package io.github.corbym.dokker

import io.github.corbym.dokker.DokkerContainer.Companion.runCommand
import java.time.Duration

class DokkerNetwork(private val networkName: String) : DokkerLifecycle {
    override val name = networkName
    override fun start() {
        if (!hasStarted())
            "docker network create $networkName".runCommand()
    }

    override fun stop() {
        // no stop on networks
    }

    override fun remove() {
        awaitUntil(timeout = Duration.ofMinutes(1)) {
            "docker network rm $networkName".runCommand() == networkName
        }
    }

    override fun hasStarted(): Boolean = "docker network ls".runCommand().contains(networkName)
}