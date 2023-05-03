package io.github.corbym.junit5

import io.github.corbym.dokker.dokker
import io.github.corbym.dokker.junit5.DokkerExtension
import io.github.corbym.dokker.junit5.dokkerExtension
import io.github.corbym.dokker.junit5.findFreePort
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import kotlin.test.assertTrue

class ExampleJUnit5RegisteredDokkerTest {
    companion object {
        val couchbasePort = findFreePort()

        @JvmStatic
        @RegisterExtension
        val server: DokkerExtension = dokkerExtension {
            container {
                dokker {
                    name("couchbase")
                    detach()
                    debug()
                    expose(couchbasePort)
                    publish(couchbasePort to couchbasePort)
                    image { "arungupta/couchbase" }
                    version { "latest" }
                }
            }
        }
    }

    @Test
    fun `can start couchbase docker container`() {
        assertTrue { server.hasStarted() }
        assertThat(server.expose.first(), equalTo(couchbasePort))
    }
}