package io.github.corbym

import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import io.github.corbym.dokker.dokker

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
                    if (dokkerContainer.process.endsWith("docker")) {
                        assertThat(runResponse, containsString("Hello from Docker!"))
                    } else {
                        assertThat(runResponse, containsString("Hello Podman World"))
                    }
                }
            }.start()
        } finally {
            dokkerContainer.remove()
        }
    }
}