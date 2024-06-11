package io.github.corbym.dokker

import io.github.corbym.dokker.DokkerContainer.Companion.runCommand
import java.time.Duration

class DokkerNetwork(private val process: String, private val networkName: String) : DokkerLifecycle {
    override val name = networkName
    override fun start() {
        if (!hasStarted())
            "$process network create $networkName".runCommand()
    }

    override fun stop() {
        // no stop on networks
    }

    override fun remove() {
        awaitUntil(timeout = Duration.ofMinutes(1)) {
            "$process network rm $networkName".runCommand() == networkName
        }
    }

    override fun hasStarted(): Boolean = "$process network ls".runCommand().contains(networkName)
}