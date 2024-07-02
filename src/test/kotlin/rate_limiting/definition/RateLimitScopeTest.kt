package rate_limiting.definition

import com.timmermans.email.EmailTopic
import com.timmermans.email.rate_limiting.definition.configuration.dsl.rateLimited
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RateLimitScopeTest {

    @Test
    fun `all topics must have a rate limiting rule defined for them`() {
        assertThrows<IllegalArgumentException>("All EmailTopics must have a RateLimiting definition") {
            rateLimited {
                unlimited(EmailTopic.NEWS)
            }
        }
    }
}