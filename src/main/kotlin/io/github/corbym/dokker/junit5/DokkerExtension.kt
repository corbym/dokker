package io.github.corbym.dokker.junit5

import io.github.corbym.dokker.DokkerContainer
import io.github.corbym.dokker.DokkerLifecycle
import io.github.corbym.dokker.DokkerProperties
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.net.ServerSocket

fun dokkerExtension(init: DokkerExtensionBuilder.() -> Unit): DokkerExtension {
    return DokkerExtensionBuilder().apply(init).build()
}

fun findFreePort() = ServerSocket(0).use { "${it.localPort}" }

class DokkerExtensionBuilder {
    private lateinit var dokkerContainer: DokkerContainer
    private var remove = true
    private var stop = true

    fun container(container: () -> DokkerContainer) {
        dokkerContainer = container()
    }

    fun doNotRemove() {
        remove = false
    }

    fun doNotStop() {
        stop = false
    }

    fun build(): DokkerExtension {
        return DokkerExtension(dokkerContainer, stopAfter = stop, removeAfter = remove)
    }
}

class DokkerExtension(
    dokkerContainer: DokkerContainer,
    val stopAfter: Boolean = true,
    val removeAfter: Boolean = true,
) : BeforeAllCallback,
    AfterAllCallback,
    DokkerProperties by dokkerContainer, DokkerLifecycle by dokkerContainer {
    override val name: String = dokkerContainer.name
    override fun beforeAll(context: ExtensionContext?) {
        if (!hasStarted()) start()
    }

    override fun afterAll(context: ExtensionContext?) {
        if (stopAfter) stop()
        if (removeAfter) remove()
    }
}