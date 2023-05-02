package io.github.corbym.dokker

import io.github.corbym.dokker.DokkerContainer.Companion.runCommand
import org.awaitility.Durations
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until

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
        await atMost Durations.ONE_MINUTE until {
            "docker network rm $networkName".runCommand() == networkName
        }
    }

    override fun hasStarted(): Boolean = "docker network ls".runCommand().contains(networkName)
}