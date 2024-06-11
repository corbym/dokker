package io.github.corbym

import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import io.github.corbym.dokker.dokker

class DokkerTest {

    @Test
    fun `can start helloworld docker container`() {
        val dokkerContainer = dokker {
            name("hello-world")
            image { "hello-world" }
            version { "latest" }
            debug()
        }

        try {
            dokkerContainer.also {
                it.onStart = { _, runResponse ->
                    assertThat(runResponse, containsString("Hello "))
                }
            }.start()
        } finally {
            dokkerContainer.remove()
        }
    }

//    @Test
//    fun `can use alternate like podman`() {
//        val dokkerContainer = dokker {
//            process("podman")
//            name("hello-podman-world")
//            image { "hello-world" }
//            version { "latest" }
//            debug()
//        }
//
//        try {
//            dokkerContainer.also {
//                it.onStart = { _, runResponse ->
//                    assertThat(runResponse, containsString("Hello Podman World"))
//                }
//            }.start()
//        } finally {
//            dokkerContainer.remove()
//        }
//    }
}