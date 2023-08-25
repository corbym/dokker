package io.github.corbym

import io.github.corbym.dokker.dokker
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DokkerHealthcheckTest {
    val container = dokker {
        detach()
        name("db")
        image("postgres:15")
        env(
            "POSTGRES_HOST_AUTH_METHOD" to "trust"
        )
        publish(
            "5432" to "5432"
        )
        healthCheck {
            checking {
                """psql -U postgres -c "SELECT 'ready'"""" to "ready"
            }
        }
    }
    val initContainer = dokker {
        name("db-init")
        image("postgres:15")
        command {
            """ls"""
        }
    }

    @BeforeEach
    fun `start containers`() {
        container.start()
        container.waitForHealthCheck()
        initContainer.start()
    }

    @AfterEach
    fun `stop containers`() {
        initContainer.stop()
        initContainer.remove()
        container.stop()
        container.remove()
    }

    @Test
    fun `healthcheck runs against correct container`() {
        container.waitForHealthCheck()
        assertThat(container.hasStarted(), equalTo(true))
    }
}