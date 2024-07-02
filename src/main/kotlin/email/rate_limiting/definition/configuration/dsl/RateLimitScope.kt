package com.timmermans.email.rate_limiting.definition.configuration.dsl

import com.timmermans.email.EmailTopic
import com.timmermans.email.rate_limiting.definition.RateLimiter
import com.timmermans.email.rate_limiting.definition.RateLimiterProvider
import com.timmermans.email.rate_limiting.definition.configuration.RateLimitRule
import com.timmermans.email.rate_limiting.definition.configuration.SharedLimitRule
import com.timmermans.email.rate_limiting.definition.rate_limiter.ProhibitedRateLimiter
import com.timmermans.email.rate_limiting.definition.rate_limiter.RegularRateLimiter
import com.timmermans.email.rate_limiting.definition.rate_limiter.UnlimitedRateLimiter
import kotlin.time.Duration

fun rateLimited(init: RateLimitScope.() -> Unit): RateLimitScope {
    return RateLimitScope().apply(init).also { it.validate() }
}


@DslMarker
annotation class RateLimitingDefinition

@RateLimitingDefinition
class RateLimitScope : RateLimiterProvider {
    private val prohibitedTopics = mutableSetOf<EmailTopic>()
    private val unlimitedTopics = mutableSetOf<EmailTopic>()

    private val isolatedRulesByTopic = mutableMapOf<EmailTopic, MutableSet<RateLimitRule>>()
    private val sharedRulesByTopic = mutableMapOf<EmailTopic, MutableSet<SharedLimitRule>>()

    private val cachedRateLimiters = mutableMapOf<EmailTopic, RateLimiter>()

    /**
     * Applies N rules to multiples topics at once.
     * These rules are completely independent by topic.
     * If you're looking for a codependent behavior between topics, see to `sharedLimit()`
     */
    fun limit(vararg topics: EmailTopic, init: RulesScope.() -> Unit) {
        val rulesScope = RulesScope().apply(init)

        for (t in setOf(*topics)) {
            isolatedRulesByTopic.getOrPut(t) { mutableSetOf() }
                .addAll(rulesScope.rules)
        }
    }

    /**
     * These topics are not subject to rate limiting.
     */
    fun unlimited(vararg topics: EmailTopic) {
        unlimitedTopics.addAll(topics)
    }

    /**
     * These topics are always blocked.
     */
    fun prohibited(vararg topics: EmailTopic) {
        prohibitedTopics.addAll(topics)
    }

    /**
     * These rules apply to a group of topics in the same way a shared pool behaves.
     *
     * Assuming topics A, B and C and rule of `1 every 1.hours`,
     * if at 00:00:00 an email is sent with topic A, no other emails can be sent with topics A, B, C until 01:00:00
     */
    fun sharedLimit(vararg topics: EmailTopic, init: RulesScope.() -> Unit) {
        val rulesScope = RulesScope().apply(init)
        val argTopics = setOf(*topics)

        val sharedLimitRule = SharedLimitRule(argTopics, rulesScope.rules)

        for (t in argTopics) {
            sharedRulesByTopic.getOrPut(t) { mutableSetOf() }.add(sharedLimitRule)
        }
    }

    override fun forTopic(topic: EmailTopic): RateLimiter {
        return cachedRateLimiters.getOrPut(topic) {
            when (topic) {
                in prohibitedTopics -> ProhibitedRateLimiter()
                in unlimitedTopics -> UnlimitedRateLimiter()
                else -> RegularRateLimiter(
                    isolatedRules = isolatedRulesByTopic[topic] ?: setOf(),
                    sharedRules = sharedRulesByTopic[topic] ?: setOf(),
                )
            }
        }
    }

    fun validate() {
        for (entry in EmailTopic.entries) {
            if (entry in prohibitedTopics) continue
            if (entry in unlimitedTopics) continue
            if (entry in isolatedRulesByTopic) continue
            if (entry in sharedRulesByTopic) continue

            throw IllegalArgumentException("All EmailTopics must have a RateLimiting definition")
        }
    }

}

@RateLimitingDefinition
class RulesScope {
    private val _rules = mutableSetOf<RateLimitRule>()

    val rules: Set<RateLimitRule>
        get() = _rules

    fun withRules(vararg rules: RateLimitRule) {
        _rules.addAll(rules)
    }

}

infix fun Int.every(period: Duration) = RateLimitRule(this, period)
