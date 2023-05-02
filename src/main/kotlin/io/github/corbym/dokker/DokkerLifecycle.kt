package io.github.corbym.dokker

interface DokkerLifecycle {
    val name: String
    fun start()
    fun stop()
    fun remove()
    fun hasStarted(): Boolean
}