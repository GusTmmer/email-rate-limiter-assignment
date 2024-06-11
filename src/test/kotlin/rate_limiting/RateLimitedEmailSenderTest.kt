package rate_limiting

import MutableClock
import TestInjectionExtension
import TransactionalExtension
import com.timmermans.SendEmailRequest
import com.timmermans.email.EmailSender
import com.timmermans.email.EmailTopic
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

@ExtendWith(*[TestInjectionExtension::class, TransactionalExtension::class])
class RateLimitedEmailSenderTest : KoinTest {

    private val testClock: MutableClock by inject()

    @ParameterizedTest
    @EnumSource(EmailTopic::class)
    fun prohibitedTopics(emailTopic: EmailTopic) {
        val allProhibitedRateLimiterDef = rateLimited { prohibited(*EmailTopic.entries.toTypedArray()) }
        val rateLimitedEmailSender: RateLimitedEmailSender by inject { parametersOf(allProhibitedRateLimiterDef) }

        assertThrows<RateLimitedException> {
            SendEmailRequest(toUserId = 1, topic = emailTopic, contents = "")
                .let { rateLimitedEmailSender.send(it) }
        }
    }

    @ParameterizedTest
    @EnumSource(EmailTopic::class)
    fun unlimitedTopics(emailTopic: EmailTopic) {
        val unlimitedRateLimiterDef = rateLimited { unlimited(*EmailTopic.entries.toTypedArray()) }
        val rateLimitedEmailSender: RateLimitedEmailSender by inject { parametersOf(unlimitedRateLimiterDef) }

        repeat(5) {
            assertDoesNotThrow {
                SendEmailRequest(toUserId = 1, topic = emailTopic, contents = "")
                    .let { rateLimitedEmailSender.send(it) }
            }
        }
    }

    @Test
    fun topicWithSingleRule() {
        val rateLimiterDefinition = rateLimited {
            prohibited(*allTopicsExcept(EmailTopic.STATUS))
            limit(EmailTopic.STATUS) {
                withRules(
                    1 every 1.hours
                )
            }
        }
        val rateLimitedEmailSender: RateLimitedEmailSender by inject { parametersOf(rateLimiterDefinition) }

        sendMessageToUserWithTopic(1, EmailTopic.STATUS, rateLimitedEmailSender)

        testClock.advanceBy(59.minutes)

        assertThrows<RateLimitedException> {
            sendMessageToUserWithTopic(1, EmailTopic.STATUS, rateLimitedEmailSender)
        }

        testClock.advanceBy(2.minutes)

        assertDoesNotThrow {
            sendMessageToUserWithTopic(1, EmailTopic.STATUS, rateLimitedEmailSender)
        }
    }

    @Test
    fun topicWithMultipleRules() {
        val rateLimiterDefinition = rateLimited {
            prohibited(*allTopicsExcept(EmailTopic.STATUS))
            limit(EmailTopic.STATUS) {
                withRules(
                    1 every 1.hours,
                    2 every 1.days,
                )
            }
        }
        val rateLimitedEmailSender: RateLimitedEmailSender by inject { parametersOf(rateLimiterDefinition) }
        val userId = 2L

        testClock.set(Instant.parse("2024-01-01T00:00:00Z"))
        sendMessageToUserWithTopic(userId, EmailTopic.STATUS, rateLimitedEmailSender)

        testClock.set(Instant.parse("2024-01-01T00:59:59Z"))
        assertThrows<RateLimitedException> {
            sendMessageToUserWithTopic(userId, EmailTopic.STATUS, rateLimitedEmailSender)
        }

        testClock.set(Instant.parse("2024-01-01T01:00:01Z"))
        assertDoesNotThrow { sendMessageToUserWithTopic(userId, EmailTopic.STATUS, rateLimitedEmailSender) }

        testClock.set(Instant.parse("2024-01-01T02:00:01Z"))
        assertThrows<RateLimitedException> {
            sendMessageToUserWithTopic(userId, EmailTopic.STATUS, rateLimitedEmailSender)
        }

        testClock.set(Instant.parse("2024-01-02T00:00:01Z"))
        assertDoesNotThrow { sendMessageToUserWithTopic(userId, EmailTopic.STATUS, rateLimitedEmailSender) }
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

        val rateLimitedEmailSender: RateLimitedEmailSender by inject { parametersOf(rateLimiterDefinition) }

        assertDoesNotThrow {
            sendMessageToUserWithTopic(userId = 1, EmailTopic.NEWS, rateLimitedEmailSender)
            sendMessageToUserWithTopic(userId = 2, EmailTopic.NEWS, rateLimitedEmailSender)
        }
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

        val rateLimitedEmailSender: RateLimitedEmailSender by inject { parametersOf(rateLimiterDefinition) }

        assertDoesNotThrow {
            sendMessageToUserWithTopic(userId = 1, EmailTopic.NEWS, rateLimitedEmailSender)
            sendMessageToUserWithTopic(userId = 1, EmailTopic.MARKETING, rateLimitedEmailSender)
        }

        assertThrows<RateLimitedException> {
            sendMessageToUserWithTopic(userId = 1, EmailTopic.NEWS, rateLimitedEmailSender)
        }

        assertThrows<RateLimitedException> {
            sendMessageToUserWithTopic(userId = 1, EmailTopic.MARKETING, rateLimitedEmailSender)
        }
    }

    @Test
    fun sharedRules() {
        val rateLimiterDefinition = rateLimited {
            prohibited(*allTopicsExcept(EmailTopic.NEWS, EmailTopic.MARKETING))
            sharedLimit(EmailTopic.NEWS, EmailTopic.MARKETING) {
                withRules(
                    1 every 1.hours
                )
            }
        }

        val rateLimitedEmailSender: RateLimitedEmailSender by inject { parametersOf(rateLimiterDefinition) }
        testClock.set(Instant.parse("2024-01-01T00:00:00Z"))

        assertDoesNotThrow {
            sendMessageToUserWithTopic(userId = 1, EmailTopic.NEWS, rateLimitedEmailSender)
        }

        assertThrows<RateLimitedException> {
            sendMessageToUserWithTopic(userId = 1, EmailTopic.MARKETING, rateLimitedEmailSender)
        }

        testClock.set(Instant.parse("2024-01-01T01:00:01Z"))
        assertDoesNotThrow {
            sendMessageToUserWithTopic(userId = 1, EmailTopic.MARKETING, rateLimitedEmailSender)
        }

        assertThrows<RateLimitedException> {
            sendMessageToUserWithTopic(userId = 1, EmailTopic.NEWS, rateLimitedEmailSender)
        }
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

        val rateLimitedEmailSender: RateLimitedEmailSender by inject { parametersOf(rateLimiterDefinition) }
        testClock.set(Instant.parse("2024-01-01T00:00:00Z"))

        assertDoesNotThrow {
            sendMessageToUserWithTopic(userId = 1, EmailTopic.NEWS, rateLimitedEmailSender)
        }

        assertThrows<RateLimitedException> {
            sendMessageToUserWithTopic(userId = 1, EmailTopic.MARKETING, rateLimitedEmailSender)
        }

        testClock.set(Instant.parse("2024-01-01T02:00:01Z"))
        assertThrows<RateLimitedException> {
            sendMessageToUserWithTopic(userId = 1, EmailTopic.NEWS, rateLimitedEmailSender)
        }

        assertDoesNotThrow {
            sendMessageToUserWithTopic(userId = 1, EmailTopic.MARKETING, rateLimitedEmailSender)
        }

        testClock.set(Instant.parse("2024-01-02T00:00:01Z"))
        assertDoesNotThrow {
            sendMessageToUserWithTopic(userId = 1, EmailTopic.NEWS, rateLimitedEmailSender)
        }

        assertThrows<RateLimitedException> {
            sendMessageToUserWithTopic(userId = 1, EmailTopic.MARKETING, rateLimitedEmailSender)
        }
    }

    private fun sendMessageToUserWithTopic(userId: Long, topic: EmailTopic, emailSender: EmailSender) {
        emailSender.send(SendEmailRequest(toUserId = userId, topic = topic, contents = ""))
    }

    private fun allTopicsExcept(vararg topicToExclude: EmailTopic): Array<EmailTopic> {
        return (EmailTopic.entries.toSet() - topicToExclude.toSet()).toTypedArray()
    }
}

private fun getRateLimitDefinition() = rateLimited {
    prohibited(EmailTopic.UNDEFINED)

    unlimited(EmailTopic.SECURITY)

    limit(EmailTopic.NEWS) {
        withRules(
            1 every 2.days,
            3 every 7.days,
        )
    }

    sharedLimit(EmailTopic.NEWS, EmailTopic.MARKETING, EmailTopic.STATUS) {
        withRules(
            1 every 1.days,
            1 every 3.hours,
        )
    }

}