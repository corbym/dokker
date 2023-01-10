package org.corbym

import org.corbym.dokker.dokker
import org.corbym.dokker.junit5.BeforeAllStarter.Companion.register
import org.corbym.dokker.junit5.DokkerProvider

class ExampleDokkerProvider : DokkerProvider {
    override val dokkerContainer = dokker {
        name("couchbase")
        detach()
        expose("8091")
        image { "arungupta/couchbase" }
        version { "latest" }
    }

    init {
        register(dokkerContainer.dockerCommand.name, dokkerContainer.hasStarted())
    }
}