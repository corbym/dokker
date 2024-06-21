package io.github.corbym

import io.github.corbym.dokker.dokker
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

class DokkerTest {

    @Test
    fun `can start helloworld docker container`() {
        // By setting the environment variable, we can toggle on the 'process' name
        // this test has out-of-process dependencies, so unable to split the test out
        val dokkerContainer = dokker {
            name("hello-world")
            image { "hello-world" }
            version { "latest" }
            debug()
        }

        try {

            dokkerContainer.also {
                it.onStart = { _, runResponse ->
                    assertThat(runResponse, containsString("Hello"))
                }
            }.start()
        } finally {
            dokkerContainer.remove()
        }
    }
}