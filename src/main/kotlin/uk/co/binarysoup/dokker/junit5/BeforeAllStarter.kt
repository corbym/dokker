package uk.co.binarysoup.dokker.junit5

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext

interface BeforeAllStarter : BeforeAllCallback {
    val id: String
    val startCommand: () -> Unit
    val stopCommand: (() -> Unit)?

    companion object {
        private val mutex = Mutex()
        var started: MutableMap<String, Boolean> = mutableMapOf()
        fun register(name: String, hasStarted: Boolean) = runBlocking {
            withContext(Dispatchers.Default) {
                mutex.withLock {
                    if (!started.containsKey(name)) {
                        started[name] = hasStarted
                    }
                }
            }
        }
    }

    override fun beforeAll(context: ExtensionContext?) = runBlocking {
        withContext(Dispatchers.Default) {
            mutex.withLock {
                val hasStarted = started[id] ?: error("beforeAll runner $id was not registered")
                if (!hasStarted) {
                    started[id] = true
                    startCommand()
                    if (stopCommand != null) // only stop if the provider started it
                        Runtime.getRuntime().addShutdownHook(object : Thread() {
                            override fun run() {
                                stopCommand!!()
                            }
                        })
                }
            }
        }
    }
}
