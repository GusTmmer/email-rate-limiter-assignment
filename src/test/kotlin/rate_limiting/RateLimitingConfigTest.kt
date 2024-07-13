package rate_limiting

import ErrorLogAppender
import LogErrorVerifierExtension
import com.timmermans.email.rate_limiting.configuration.RateLimiterProviderFromConfig
import com.timmermans.email.rate_limiting.configuration.dsl.buildRateLimiter
import com.timmermans.email.rate_limiting.rate_limiter.ProhibitedRateLimiter
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

class RateLimitingConfigTest {

    @Test
    @ExtendWith(LogErrorVerifierExtension::class)
    fun `when requesting a provider for a topic without config, use 'prohibited' and produce error log`(
        errorLogAppender: ErrorLogAppender,
    ) {
        val provider = RateLimiterProviderFromConfig(emptyList())
        assertInstanceOf(ProhibitedRateLimiter::class.java, provider.forTopic("TOPIC"))
        errorLogAppender.assertHasErrorLogs()
    }

    @Test
    fun `uses of topic are case insensitive`() {
        val provider = buildRateLimiter { unlimited("topic") }

        val rateLimiters = listOf("topic", "Topic", "TOPIC").map { provider.forTopic(it) }

        assertTrue(rateLimiters.all { it == rateLimiters.first() })
    }
}