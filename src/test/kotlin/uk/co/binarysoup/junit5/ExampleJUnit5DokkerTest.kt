package uk.co.binarysoup.junit5

import org.awaitility.Durations
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.co.binarysoup.dokker.DokkerContainer.Companion.runCommand
import uk.co.binarysoup.dokker.junit5.BeforeAllStarter
import kotlin.test.assertTrue

@ExtendWith(ExampleDokkerProvider::class)
class ExampleJUnit5DokkerTest {
    @Test
    fun `can start couchbase docker container`() {
        val name = "couchbase"
        assertTrue(BeforeAllStarter.started[name]!!)
        await atMost Durations.FIVE_MINUTES until {
            "docker ps --filter name=$name".runCommand().contains(name)
        }
    }
}