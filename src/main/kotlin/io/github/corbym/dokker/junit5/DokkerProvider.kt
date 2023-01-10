package io.github.corbym.dokker.junit5

import io.github.corbym.dokker.DokkerContainer

interface DokkerProvider : BeforeAllStarter {
    val dokkerContainer: DokkerContainer

    override val id: String
        get() = dokkerContainer.name
    override val startCommand: () -> Unit get() = { dokkerContainer.start() }
    override val stopCommand: () -> Unit get() = {
        dokkerContainer.stop()
        dokkerContainer.remove()
    }
}
