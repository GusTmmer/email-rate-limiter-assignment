package rate_limiting

import MutableClock
import TestInjectionExtension
import TransactionalExtension
import com.timmermans.email.EmailSender
import com.timmermans.email.EmailTopic
import com.timmermans.email.SendEmailRequest
import com.timmermans.email.rate_limiting.RateLimitedEmailSender
import com.timmermans.email.rate_limiting.RateLimitedException
import com.timmermans.email.rate_limiting.definition.every
import com.timmermans.email.rate_limiting.definition.rateLimited
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.koin.core.parameter.parametersOf
import org.koin.test.KoinTest
import org.koin.test.inject
import java.time.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@ExtendWith(TestInjectionExtension::class, TransactionalExtension::class)
class RateLimitedEmailSenderTest : KoinTest {

    private val testClock: MutableClock by inject()

    @ParameterizedTest
    @EnumSource(EmailTopic::class)
    fun `topics configured with prohibited`(emailTopic: EmailTopic) {
        val allProhibitedRateLimiterDef = rateLimited { prohibited(*EmailTopic.entries.toTypedArray()) }
        val emailSender: RateLimitedEmailSender by inject { parametersOf(allProhibitedRateLimiterDef) }

        assertRateLimited(1, emailTopic, emailSender)
    }

    @ParameterizedTest
    @EnumSource(EmailTopic::class)
    fun `topics configured with unlimited`(emailTopic: EmailTopic) {
        val unlimitedRateLimiterDef = rateLimited { unlimited(*EmailTopic.entries.toTypedArray()) }
        val emailSender: RateLimitedEmailSender by inject { parametersOf(unlimitedRateLimiterDef) }

        repeat(5) {
            assertNotRateLimited(1, emailTopic, emailSender)
        }
    }

    @Test
    fun `topic with single regular rule`() {
        val rateLimiterDefinition = rateLimited {
            prohibited(*allTopicsExcept(EmailTopic.STATUS))
            limit(EmailTopic.STATUS) {
                withRules(
                    1 every 1.hours
                )
            }
        }
        val emailSender: RateLimitedEmailSender by inject { parametersOf(rateLimiterDefinition) }

        assertNotRateLimited(1, EmailTopic.STATUS, emailSender)

        testClock.advanceBy(59.minutes)
        // Max 1 every hour
        assertRateLimited(1, EmailTopic.STATUS, emailSender)

        testClock.advanceBy(2.minutes)
        assertNotRateLimited(1, EmailTopic.STATUS, emailSender)
    }

    @Test
    fun `topic with multiple regular rules`() {
        val rateLimiterDefinition = rateLimited {
            prohibited(*allTopicsExcept(EmailTopic.STATUS))
            limit(EmailTopic.STATUS) {
                withRules(
                    1 every 1.hours,
                    2 every 1.days,
                )
            }
        }
        val emailSender: RateLimitedEmailSender by inject { parametersOf(rateLimiterDefinition) }
        val userId = 2L

        testClock.set(Instant.parse("2024-01-01T00:00:00Z"))
        assertNotRateLimited(userId, EmailTopic.STATUS, emailSender)

        testClock.set(Instant.parse("2024-01-01T00:59:59Z")) // 59 minutes after start
        // Max 1 every hour
        assertRateLimited(userId, EmailTopic.STATUS, emailSender)

        testClock.set(Instant.parse("2024-01-01T01:00:01Z")) // 1 hour after start
        assertNotRateLimited(userId, EmailTopic.STATUS, emailSender)

        testClock.set(Instant.parse("2024-01-01T02:00:01Z")) // 2 hours after start
        // Max 2 every day
        assertRateLimited(userId, EmailTopic.STATUS, emailSender)

        testClock.set(Instant.parse("2024-01-02T00:00:01Z")) // 1 day after start
        assertNotRateLimited(userId, EmailTopic.STATUS, emailSender)
    }

    @Test
    fun `rate limiting rules affect users independently`() {
        val rateLimiterDefinition = rateLimited {
            prohibited(*allTopicsExcept(EmailTopic.NEWS))
            limit(EmailTopic.NEWS) {
                withRules(
                    1 every 1.hours
                )
            }
        }

        val emailSender: RateLimitedEmailSender by inject { parametersOf(rateLimiterDefinition) }

        assertNotRateLimited(userId = 1, EmailTopic.NEWS, emailSender)
        assertNotRateLimited(userId = 2, EmailTopic.NEWS, emailSender)
    }

    @Test
    fun `rate limiting rules are independent when using 'limit' definition`() {
        val rateLimiterDefinition = rateLimited {
            prohibited(*allTopicsExcept(EmailTopic.NEWS, EmailTopic.MARKETING))
            limit(EmailTopic.NEWS, EmailTopic.MARKETING) {
                withRules(
                    1 every 1.hours
                )
            }
        }

        val emailSender: RateLimitedEmailSender by inject { parametersOf(rateLimiterDefinition) }
        val userId = 1L

        assertNotRateLimited(userId, EmailTopic.NEWS, emailSender)
        assertNotRateLimited(userId, EmailTopic.MARKETING, emailSender)

        assertRateLimited(userId, EmailTopic.NEWS, emailSender)
        assertRateLimited(userId, EmailTopic.MARKETING, emailSender)
    }

    @Test
    fun `shared rules`() {
        val rateLimiterDefinition = rateLimited {
            prohibited(*allTopicsExcept(EmailTopic.NEWS, EmailTopic.MARKETING))
            sharedLimit(EmailTopic.NEWS, EmailTopic.MARKETING) {
                withRules(
                    1 every 1.hours
                )
            }
        }

        val emailSender: RateLimitedEmailSender by inject { parametersOf(rateLimiterDefinition) }
        val userId = 1L

        testClock.set(Instant.parse("2024-01-01T00:00:00Z"))
        assertNotRateLimited(userId, EmailTopic.NEWS, emailSender)
        assertRateLimited(userId, EmailTopic.MARKETING, emailSender)

        testClock.set(Instant.parse("2024-01-01T01:00:01Z")) // 1 hour after start
        assertNotRateLimited(userId, EmailTopic.MARKETING, emailSender)
        assertRateLimited(userId, EmailTopic.NEWS, emailSender)
    }

    @Test
    fun `interaction between limit and sharedLimit rules`() {
        val rateLimiterDefinition = rateLimited {
            prohibited(*allTopicsExcept(EmailTopic.NEWS, EmailTopic.MARKETING))

            limit(EmailTopic.NEWS) {
                withRules(
                    1 every 1.days,
                )
            }

            sharedLimit(EmailTopic.NEWS, EmailTopic.MARKETING) {
                withRules(
                    1 every 1.hours
                )
            }
        }

        val emailSender: RateLimitedEmailSender by inject { parametersOf(rateLimiterDefinition) }
        val userId = 1L

        testClock.set(Instant.parse("2024-01-01T00:00:00Z"))
        assertNotRateLimited(userId, EmailTopic.NEWS, emailSender)
        assertRateLimited(userId, EmailTopic.MARKETING, emailSender)

        testClock.set(Instant.parse("2024-01-01T02:00:01Z")) // 2 hours after start
        assertRateLimited(userId, EmailTopic.NEWS, emailSender)
        assertNotRateLimited(userId, EmailTopic.MARKETING, emailSender)

        testClock.set(Instant.parse("2024-01-02T00:00:01Z")) // 1 day after start
        assertNotRateLimited(userId, EmailTopic.NEWS, emailSender)
        assertRateLimited(userId, EmailTopic.MARKETING, emailSender)
    }

    @Test
    fun `interaction between multiple limit and sharedLimit rules`() {
        val rateLimiterDefinition = rateLimited {
            prohibited(*allTopicsExcept(EmailTopic.NEWS, EmailTopic.MARKETING))

            limit(EmailTopic.NEWS) {
                withRules(
                    1 every 1.days,
                    2 every 7.days,
                )
            }
            limit(EmailTopic.MARKETING) {
                withRules(
                    1 every 1.days,
                    3 every 7.days,
                )
            }
            sharedLimit(EmailTopic.NEWS, EmailTopic.MARKETING) {
                withRules(
                    2 every 2.days
                )
            }
        }

        val emailSender: RateLimitedEmailSender by inject { parametersOf(rateLimiterDefinition) }
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

    private fun assertNotRateLimited(userId: Long, topic: EmailTopic, emailSender: RateLimitedEmailSender) {
        assertDoesNotThrow { sendMessageToUserWithTopic(userId, topic, emailSender) }
    }

    private fun assertRateLimited(userId: Long, topic: EmailTopic, emailSender: RateLimitedEmailSender) {
        assertThrows<RateLimitedException> { sendMessageToUserWithTopic(userId, topic, emailSender) }
    }

    private fun sendMessageToUserWithTopic(userId: Long, topic: EmailTopic, emailSender: EmailSender) {
        emailSender.send(SendEmailRequest(toUserId = userId, topic = topic, contents = ""))
    }

    private fun allTopicsExcept(vararg topicToExclude: EmailTopic): Array<EmailTopic> {
        return (EmailTopic.entries.toSet() - topicToExclude.toSet()).toTypedArray()
    }
}
