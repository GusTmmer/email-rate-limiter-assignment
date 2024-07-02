package rate_limiting

import MutableClock
import RateLimitTestHelper.assertNotRateLimited
import RateLimitTestHelper.assertRateLimited
import TestInjectionExtension
import TransactionalExtension
import com.timmermans.email.EmailTopic
import com.timmermans.email.rate_limiting.RateLimitedEmailSender
import com.timmermans.email.rate_limiting.definition.configuration.file.RateLimiterProvider
import com.timmermans.email.rate_limiting.definition.configuration.file.loadRateLimitConfig
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.koin.core.parameter.parametersOf
import org.koin.test.KoinTest
import org.koin.test.inject
import java.time.Instant

@ExtendWith(TestInjectionExtension::class, TransactionalExtension::class)
class RateLimitedEmailSenderTestConfigByFile : KoinTest {

    private val testClock: MutableClock by inject()

    @Test
    fun `test configuration using file`() {
        val provider = RateLimiterProvider(loadRateLimitConfig("src/test/resources/testRateLimitConfiguration.json"))
        val emailSender: RateLimitedEmailSender by inject { parametersOf(provider) }

        val userId = 1L

        testClock.set(Instant.parse("2024-01-01T00:00:00Z")) // Day 1
        assertNotRateLimited(userId, EmailTopic.NEWS, emailSender) // Total: N: 1 & M: 0
        // Blocked by regular limit -> 1 NEWS per day
        assertRateLimited(userId, EmailTopic.NEWS, emailSender)

        assertNotRateLimited(userId, EmailTopic.MARKETING, emailSender) // Total: N: 1 & M: 1
        // Blocked by regular limit and shared limit -> 1 MARKETING per day and 2 in total every 2 days
        assertRateLimited(userId, EmailTopic.MARKETING, emailSender)

        testClock.set(Instant.parse("2024-01-02T00:00:00Z")) // Day 2
        // Blocked by shared limit -> 2 every 2 days
        assertRateLimited(userId, EmailTopic.NEWS, emailSender)
        assertRateLimited(userId, EmailTopic.MARKETING, emailSender)

        testClock.set(Instant.parse("2024-01-03T00:00:01Z")) // Day 3
        assertNotRateLimited(userId, EmailTopic.NEWS, emailSender) // Total: N: 2 & M: 1
        // Blocked by regular limit -> 1 NEWS per day and 2 NEWS per week
        assertRateLimited(userId, EmailTopic.NEWS, emailSender)

        assertNotRateLimited(userId, EmailTopic.MARKETING, emailSender) // Total: N: 2 & M: 2
        // Blocked by regular limit and shared limit -> 1 MARKETING per day and 2 in total every 2 days
        assertRateLimited(userId, EmailTopic.MARKETING, emailSender)


        testClock.set(Instant.parse("2024-01-04T00:00:01Z")) // Day 4
        // Blocked by shared limit -> 2 every 2 days
        assertRateLimited(userId, EmailTopic.NEWS, emailSender)
        assertRateLimited(userId, EmailTopic.MARKETING, emailSender)

        testClock.set(Instant.parse("2024-01-05T00:00:02Z")) // Day 5
        // Blocked by regular limit -> 2 every 7 days
        assertRateLimited(userId, EmailTopic.NEWS, emailSender)

        assertNotRateLimited(userId, EmailTopic.MARKETING, emailSender) // Total: N: 2 & M: 3
        // Blocked by regular limit -> 3 every 7 days
        assertRateLimited(userId, EmailTopic.MARKETING, emailSender)

        testClock.set(Instant.parse("2024-01-07T00:00:02Z")) // Day 7
        // Blocked by regular limit -> 2 every 7 days
        assertRateLimited(userId, EmailTopic.NEWS, emailSender)
        // Blocked by regular limit -> 3 every 7 days
        assertRateLimited(userId, EmailTopic.MARKETING, emailSender)

        testClock.set(Instant.parse("2024-01-08T00:00:02Z")) // Day 8
        assertNotRateLimited(userId, EmailTopic.NEWS, emailSender)
        assertNotRateLimited(userId, EmailTopic.MARKETING, emailSender)
    }
}