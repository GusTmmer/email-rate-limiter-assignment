package rate_limiting

import com.timmermans.email.rate_limiting.configuration.RateLimiterProviderFromConfig
import com.timmermans.email.rate_limiting.configuration.dsl.buildRateLimiter
import com.timmermans.email.rate_limiting.configuration.dsl.every
import com.timmermans.email.rate_limiting.configuration.file.loadRateLimitConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.koin.test.KoinTest
import kotlin.time.Duration.Companion.days

class RateLimitingConfigSourceTest : KoinTest {

    @Test
    fun `two sources for the same rate-limiting spec should produce equal config`() {
        val providerForFile = RateLimiterProviderFromConfig(
            loadRateLimitConfig("src/test/resources/testRateLimitConfiguration.json")
        )

        val providerForBuilder = buildRateLimiter {
            limit("NEWS") {
                withRules(
                    1 every 1.days,
                    2 every 7.days,
                )
            }
            limit("MARKETING") {
                withRules(
                    1 every 1.days,
                    3 every 7.days,
                )
            }
            sharedLimit("NEWS", "MARKETING") {
                withRules(
                    2 every 2.days
                )
            }
        } as RateLimiterProviderFromConfig

        assertEquals(providerForFile.getSourceConfigs(), providerForBuilder.getSourceConfigs())
    }
}