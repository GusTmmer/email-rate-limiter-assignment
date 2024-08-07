package rate_limiting

import LogErrorVerifierExtension
import MutableClock
import RateLimitTestHelper.assertNotRateLimited
import RateLimitTestHelper.assertRateLimited
import TestInjectionExtension
import TransactionalExtension
import com.timmermans.email.rate_limiting.RateLimitedEmailSender
import com.timmermans.email.rate_limiting.configuration.dsl.buildRateLimiter
import com.timmermans.email.rate_limiting.configuration.dsl.every
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.koin.core.parameter.parametersOf
import org.koin.test.KoinTest
import org.koin.test.inject
import java.time.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@ExtendWith(TestInjectionExtension::class, TransactionalExtension::class, LogErrorVerifierExtension::class)
class RateLimitedEmailSenderTest : KoinTest {

    private val testClock: MutableClock by inject()

    @Test
    fun `topics configured with prohibited take precedence`() {
        val topic = "PROHIBITED"
        val allProhibitedRateLimiterDef = buildRateLimiter { prohibited(topic) }
        val emailSender: RateLimitedEmailSender by inject { parametersOf(allProhibitedRateLimiterDef) }

        assertRateLimited(1, topic, emailSender)
    }

    @Test
    fun `topics configured with unlimited`() {
        val topic = "UNLIMITED"
        val unlimitedRateLimiterDef = buildRateLimiter { unlimited(topic) }
        val emailSender: RateLimitedEmailSender by inject { parametersOf(unlimitedRateLimiterDef) }

        repeat(5) {
            assertNotRateLimited(1, topic, emailSender)
        }
    }

    @Test
    fun `topic with single regular rule`() {
        val rateLimiterDefinition = buildRateLimiter {
            limit("STATUS") {
                withRules(
                    1 every 1.hours
                )
            }
        }
        val emailSender: RateLimitedEmailSender by inject { parametersOf(rateLimiterDefinition) }

        assertNotRateLimited(1, "STATUS", emailSender)

        testClock.advanceBy(59.minutes)
        // Max 1 every hour
        assertRateLimited(1, "STATUS", emailSender)

        testClock.advanceBy(2.minutes)
        assertNotRateLimited(1, "STATUS", emailSender)
    }

    @Test
    fun `topic with multiple regular rules`() {
        val rateLimiterDefinition = buildRateLimiter {
            limit("STATUS") {
                withRules(
                    1 every 1.hours,
                    2 every 1.days,
                )
            }
        }
        val emailSender: RateLimitedEmailSender by inject { parametersOf(rateLimiterDefinition) }
        val userId = 2L

        testClock.set(Instant.parse("2024-01-01T00:00:00Z"))
        assertNotRateLimited(userId, "STATUS", emailSender)

        testClock.set(Instant.parse("2024-01-01T00:59:59Z")) // 59 minutes after start
        // Max 1 every hour
        assertRateLimited(userId, "STATUS", emailSender)

        testClock.set(Instant.parse("2024-01-01T01:00:01Z")) // 1 hour after start
        assertNotRateLimited(userId, "STATUS", emailSender)

        testClock.set(Instant.parse("2024-01-01T02:00:01Z")) // 2 hours after start
        // Max 2 every day
        assertRateLimited(userId, "STATUS", emailSender)

        testClock.set(Instant.parse("2024-01-02T00:00:01Z")) // 1 day after start
        assertNotRateLimited(userId, "STATUS", emailSender)
    }

    @Test
    fun `rate limiting rules affect users independently`() {
        val rateLimiterDefinition = buildRateLimiter {
            limit("NEWS") {
                withRules(
                    1 every 1.hours
                )
            }
        }

        val emailSender: RateLimitedEmailSender by inject { parametersOf(rateLimiterDefinition) }

        assertNotRateLimited(userId = 1, "NEWS", emailSender)
        assertNotRateLimited(userId = 2, "NEWS", emailSender)
    }

    @Test
    fun `rate limiting rules are independent when using 'limit' definition`() {
        val rateLimiterDefinition = buildRateLimiter {
            limit("NEWS", "MARKETING") {
                withRules(
                    1 every 1.hours
                )
            }
        }

        val emailSender: RateLimitedEmailSender by inject { parametersOf(rateLimiterDefinition) }
        val userId = 1L

        assertNotRateLimited(userId, "NEWS", emailSender)
        assertNotRateLimited(userId, "MARKETING", emailSender)

        assertRateLimited(userId, "NEWS", emailSender)
        assertRateLimited(userId, "MARKETING", emailSender)
    }

    @Test
    fun `shared rules`() {
        val rateLimiterDefinition = buildRateLimiter {
            sharedLimit("NEWS", "MARKETING") {
                withRules(
                    1 every 1.hours
                )
            }
        }

        val emailSender: RateLimitedEmailSender by inject { parametersOf(rateLimiterDefinition) }
        val userId = 1L

        testClock.set(Instant.parse("2024-01-01T00:00:00Z"))
        assertNotRateLimited(userId, "NEWS", emailSender)
        assertRateLimited(userId, "MARKETING", emailSender)

        testClock.set(Instant.parse("2024-01-01T01:00:01Z")) // 1 hour after start
        assertNotRateLimited(userId, "MARKETING", emailSender)
        assertRateLimited(userId, "NEWS", emailSender)
    }

    @Test
    fun `interaction between limit and sharedLimit rules`() {
        val rateLimiterDefinition = buildRateLimiter {
            limit("NEWS") {
                withRules(
                    1 every 1.days,
                )
            }

            sharedLimit("NEWS", "MARKETING") {
                withRules(
                    1 every 1.hours
                )
            }
        }

        val emailSender: RateLimitedEmailSender by inject { parametersOf(rateLimiterDefinition) }
        val userId = 1L

        testClock.set(Instant.parse("2024-01-01T00:00:00Z"))
        assertNotRateLimited(userId, "NEWS", emailSender)
        assertRateLimited(userId, "MARKETING", emailSender)

        testClock.set(Instant.parse("2024-01-01T02:00:01Z")) // 2 hours after start
        assertRateLimited(userId, "NEWS", emailSender)
        assertNotRateLimited(userId, "MARKETING", emailSender)

        testClock.set(Instant.parse("2024-01-02T00:00:01Z")) // 1 day after start
        assertNotRateLimited(userId, "NEWS", emailSender)
        assertRateLimited(userId, "MARKETING", emailSender)
    }

    @Test
    fun `interaction between multiple limit and sharedLimit rules`() {
        val rateLimiterDefinition = buildRateLimiter {
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
        }

        val emailSender: RateLimitedEmailSender by inject { parametersOf(rateLimiterDefinition) }
        val userId = 1L

        testClock.set(Instant.parse("2024-01-01T00:00:00Z")) // Day 1
        assertNotRateLimited(userId, "NEWS", emailSender) // Total: N: 1 & M: 0
        // Blocked by regular limit -> 1 NEWS per day
        assertRateLimited(userId, "NEWS", emailSender)

        assertNotRateLimited(userId, "MARKETING", emailSender) // Total: N: 1 & M: 1
        // Blocked by regular limit and shared limit -> 1 MARKETING per day and 2 in total every 2 days
        assertRateLimited(userId, "MARKETING", emailSender)

        testClock.set(Instant.parse("2024-01-02T00:00:00Z")) // Day 2
        // Blocked by shared limit -> 2 every 2 days
        assertRateLimited(userId, "NEWS", emailSender)
        assertRateLimited(userId, "MARKETING", emailSender)

        testClock.set(Instant.parse("2024-01-03T00:00:01Z")) // Day 3
        assertNotRateLimited(userId, "NEWS", emailSender) // Total: N: 2 & M: 1
        // Blocked by regular limit -> 1 NEWS per day and 2 NEWS per week
        assertRateLimited(userId, "NEWS", emailSender)

        assertNotRateLimited(userId, "MARKETING", emailSender) // Total: N: 2 & M: 2
        // Blocked by regular limit and shared limit -> 1 MARKETING per day and 2 in total every 2 days
        assertRateLimited(userId, "MARKETING", emailSender)


        testClock.set(Instant.parse("2024-01-04T00:00:01Z")) // Day 4
        // Blocked by shared limit -> 2 every 2 days
        assertRateLimited(userId, "NEWS", emailSender)
        assertRateLimited(userId, "MARKETING", emailSender)

        testClock.set(Instant.parse("2024-01-05T00:00:02Z")) // Day 5
        // Blocked by regular limit -> 2 every 7 days
        assertRateLimited(userId, "NEWS", emailSender)

        assertNotRateLimited(userId, "MARKETING", emailSender) // Total: N: 2 & M: 3
        // Blocked by regular limit -> 3 every 7 days
        assertRateLimited(userId, "MARKETING", emailSender)

        testClock.set(Instant.parse("2024-01-07T00:00:02Z")) // Day 7
        // Blocked by regular limit -> 2 every 7 days
        assertRateLimited(userId, "NEWS", emailSender)
        // Blocked by regular limit -> 3 every 7 days
        assertRateLimited(userId, "MARKETING", emailSender)

        testClock.set(Instant.parse("2024-01-08T00:00:02Z")) // Day 8
        assertNotRateLimited(userId, "NEWS", emailSender)
        assertNotRateLimited(userId, "MARKETING", emailSender)
    }
}
