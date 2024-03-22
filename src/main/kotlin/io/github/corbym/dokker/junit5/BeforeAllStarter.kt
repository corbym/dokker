package io.github.corbym.dokker.junit5

import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.util.concurrent.ConcurrentHashMap

/**
 * BeforeAllStarter starts ONE instance of the container before ALL tests that can run
 * in a single JVM instance. It will stop the container only on JVM shutdown.
 *
 * This will also NOT start the container if it detects the container is already
 * running for any reason.
 *
 * The underlying container will NOT start if the container is stopped, but wasn't removed
 * and will error. This is how Docker behaves.
 *
 * Consider using `DokkerExtension` if  you need more fine control over the container.
 */
interface BeforeAllStarter : BeforeAllCallback {
    val id: String
    val startCommand: () -> Unit
    val stopCommand: (() -> Unit)?

    companion object {
        private val started = ConcurrentHashMap<String, Boolean>()

        fun hasStarted(id: String): Boolean? = started[id]

        fun register(name: String, hasStarted: Boolean) {
            started.compute(name) { _, value ->
                value ?: hasStarted
            }
        }
    }

    override fun beforeAll(context: ExtensionContext?) {
        started.compute(id) { _, hasStarted ->
            requireNotNull(hasStarted) { "beforeAll runner $id was not registered" }
            if (!hasStarted) {
                startCommand()
                // only stop if the provider started it
                stopCommand?.let { command ->
                    Runtime.getRuntime().addShutdownHook(object : Thread() {
                        override fun run() {
                            command()
                        }
                    })
                }
            }
            true
        }
    }
}
