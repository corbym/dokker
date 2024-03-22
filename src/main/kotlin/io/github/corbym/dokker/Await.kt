package io.github.corbym.dokker

import java.time.Duration
import java.time.Instant

internal fun awaitUntil(
    timeout: Duration,
    initialDelay: Duration? = null,
    pollInterval: Duration = Duration.ofMillis(100),
    condition: () -> Boolean,
) {
    initialDelay?.let { Thread.sleep(it.toMillis()) }
    val endTime = Instant.now().plus(timeout)
    var fullfilled = condition()
    while (true) {
        if (fullfilled) {
            break
        }
        if (Instant.now() >= endTime) {
            break
        }
        Thread.sleep(pollInterval.toMillis())
        fullfilled = condition()
    }
    if (!fullfilled) {
        error("condition was not completed within $timeout")
    }
}
