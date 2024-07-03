package rate_limiting

import com.timmermans.email.EmailTopic
import com.timmermans.email.rate_limiting.configuration.RateLimiterProviderFromConfig
import com.timmermans.email.rate_limiting.rate_limiter.ProhibitedRateLimiter
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class RateLimitingConfigTest {

    @ParameterizedTest
    @EnumSource(EmailTopic::class)
    fun `when there is no config for a topic, set it to prohibited`(emailTopic: EmailTopic) {
        val provider = RateLimiterProviderFromConfig(listOf())
        assertInstanceOf(ProhibitedRateLimiter::class.java, provider.forTopic(emailTopic))
    }
}