package uk.co.binarysoup.junit5

import uk.co.binarysoup.dokker.dokker
import uk.co.binarysoup.dokker.junit5.BeforeAllStarter.Companion.register
import uk.co.binarysoup.dokker.junit5.DokkerProvider

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