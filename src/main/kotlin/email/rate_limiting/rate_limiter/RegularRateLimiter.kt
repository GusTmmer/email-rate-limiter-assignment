package com.timmermans.email.rate_limiting.rate_limiter

import com.timmermans.email.EmailTopic
import com.timmermans.email.SendEmailRequest
import com.timmermans.email.rate_limiting.RateLimiter
import com.timmermans.email.rate_limiting.configuration.RateLimitRule
import com.timmermans.email.rate_limiting.configuration.SharedLimitRule
import com.timmermans.persistence.OutboundEmail
import com.timmermans.persistence.OutboundEmailsRepository
import com.timmermans.persistence.Transactor
import org.hibernate.Session
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

private val logger = LoggerFactory.getLogger(RegularRateLimiter::class.simpleName)

typealias TimeWindow = Duration
typealias EmailCountByTopic = Map<EmailTopic, Int>

class RegularRateLimiter(
    private val isolatedRules: Set<RateLimitRule>,
    private val sharedRules: Set<SharedLimitRule>,
) : RateLimiter, KoinComponent {

    private val transactor: Transactor by inject()
    private val outboundEmailsRepository: OutboundEmailsRepository by inject()
    private val clock: Clock by inject()

    override fun shouldRateLimit(emailRequest: SendEmailRequest): Boolean {
        val now = clock.instant()

        val outboundEmails = getRelevantEmails(emailRequest, now)

        val emailCountByTopicInTimeWindow = countEmailsByTopicArrangedByTimeWindow(outboundEmails, now)

        return hasMaxEmailsInTimeWindow(emailRequest.topic, emailCountByTopicInTimeWindow)
    }

    private fun hasMaxEmailsInTimeWindow(
        messageTopic: EmailTopic,
        emailCountByTopicInTimeWindow: Map<TimeWindow, EmailCountByTopic>,
    ): Boolean {
        for (rule in isolatedRules) {
            if ((emailCountByTopicInTimeWindow[rule.timeWindow]!![messageTopic] ?: 0) >= rule.limit) {
                logger.info("Rate limited message with topic $messageTopic using rule $rule")
                return true
            }
        }

        for (sharedRule in sharedRules) {
            for (rule in sharedRule.rules) {
                val totalSent = sharedRule.groupTopics.sumOf { topic ->
                    emailCountByTopicInTimeWindow[rule.timeWindow]!![topic] ?: 0
                }

                if (totalSent >= rule.limit) {
                    logger.info("Rate limited message with topic $messageTopic using rule $rule from shared limit")
                    return true
                }
            }
        }

        return false
    }

    private fun countEmailsByTopicArrangedByTimeWindow(
        outboundEmails: List<OutboundEmail>,
        now: Instant
    ): Map<TimeWindow, EmailCountByTopic> {
        val outboundEmailsByTimeWindow = mutableMapOf<Duration, List<OutboundEmail>>()

        val allEmailsInTimeWindow = mutableListOf<OutboundEmail>()
        var outboundEmailsLeftToAnalyze = outboundEmails

        for ((timeWindow, cutoffTime) in getAllRuleTimeWindowsInAscOrder().map { it to (now - it.toJavaDuration()) }) {
            val (emailsInTimeWindow, emailsOutOfTimeWindow) = outboundEmailsLeftToAnalyze
                .partition { it.sentAt >= cutoffTime }

            allEmailsInTimeWindow.addAll(emailsInTimeWindow)
            outboundEmailsByTimeWindow[timeWindow] = allEmailsInTimeWindow.toList()

            outboundEmailsLeftToAnalyze = emailsOutOfTimeWindow
        }

        return outboundEmailsByTimeWindow.mapValues { (_, emails) ->
            emails.groupingBy { it.topic }.eachCount()
        }
    }

    private fun getRelevantEmails(emailRequest: SendEmailRequest, now: Instant): List<OutboundEmail> {
        return transactor.withSession { session: Session ->
            outboundEmailsRepository.getOutboundEmailsWithTopicsForUser(
                session = session,
                userId = emailRequest.toUserId,
                topics = getAllTopicsUsedBySharedRules() + emailRequest.topic,
                after = now - getLargestTimeWindowAmongRules().toJavaDuration()
            ).map {
                it.toPlainEntity()
            }
        }
    }

    private fun getAllRuleTimeWindowsInAscOrder(): List<Duration> {
        val isolatedRuleTimeWindows = isolatedRules.map { it.timeWindow }
        val sharedRuleTimeWindows = sharedRules.flatMap { it.rules.map { rule -> rule.timeWindow } }

        return (isolatedRuleTimeWindows + sharedRuleTimeWindows).sorted()
    }

    private fun getLargestTimeWindowAmongRules(): Duration {
        val isolatedRulesMaxTimeWindow = isolatedRules.maxTimeWindow()
        val sharedRulesMaxTimeWindow = sharedRules.maxOfOrNull { it.rules.maxTimeWindow() } ?: 0.seconds

        return maxOf(isolatedRulesMaxTimeWindow, sharedRulesMaxTimeWindow)
    }

    private fun getAllTopicsUsedBySharedRules(): Set<EmailTopic> {
        return sharedRules.map { it.groupTopics }.fold(setOf()) { acc, emailTopics -> acc + emailTopics }
    }
}

private fun Collection<RateLimitRule>.maxTimeWindow() = maxOfOrNull { it.timeWindow } ?: 0.seconds
