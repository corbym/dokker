package org.corbym

import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.corbym.dokker.dokker

class DokkerTest {
    val dokkerContainer = dokker {
        name("hello-world")
        image { "hello-world" }
        version { "latest" }
        debug()
    }

    @Test
    fun `can start helloworld docker container`() {
        dokkerContainer.also {
            it.onStart = { _, runResponse ->
                assertThat(runResponse, containsString("Hello from Docker!"))
            }
        }.start()
        dokkerContainer.remove()
    }
}