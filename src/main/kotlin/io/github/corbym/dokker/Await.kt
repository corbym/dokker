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
    var fulfilled = condition()
    while (true) {
        if (fulfilled) {
            break
        }
        if (Instant.now() >= endTime) {
            break
        }
        Thread.sleep(pollInterval.toMillis())
        fulfilled = condition()
    }
    if (!fulfilled) {
        error("condition was not completed within $timeout")
    }
}
