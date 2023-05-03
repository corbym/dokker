package io.github.corbym.junit5

import io.github.corbym.dokker.dokker
import io.github.corbym.dokker.junit5.DokkerExtension
import io.github.corbym.dokker.junit5.dokkerExtension
import io.github.corbym.dokker.junit5.findFreePort
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import kotlin.test.assertTrue

class ExampleJUnit5RegisteredDokkerTest {
    companion object {
        @JvmStatic
        @RegisterExtension
        val server: DokkerExtension = dokkerExtension {
            container {
                val freePort = findFreePort()

                dokker {
                    name("couchbase")
                    detach()
                    debug()
                    expose(freePort)
                    publish(freePort to freePort)
                    image { "arungupta/couchbase" }
                    version { "latest" }
                }
            }
        }
    }

    @Test
    fun `can start couchbase docker container`() {
        assertTrue { server.hasStarted() }
    }
}