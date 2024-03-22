package io.github.corbym.junit5

import io.github.corbym.dokker.DokkerContainer.Companion.runCommand
import io.github.corbym.dokker.awaitUntil
import io.github.corbym.dokker.junit5.BeforeAllStarter
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration
import kotlin.test.assertTrue

@ExtendWith(ExampleDokkerProvider::class)
class ExampleJUnit5DokkerTest {
    @Test
    fun `can start couchbase docker container`() {
        val name = "couchbase"
        assertTrue(BeforeAllStarter.started[name]!!)
        awaitUntil(Duration.ofMinutes(5)) {
            "docker ps --filter name=$name".runCommand().contains(name)
        }
    }
}