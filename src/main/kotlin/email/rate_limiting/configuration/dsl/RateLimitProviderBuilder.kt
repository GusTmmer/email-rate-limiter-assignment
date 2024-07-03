package com.timmermans.email.rate_limiting.configuration.dsl

import com.timmermans.email.EmailTopic
import com.timmermans.email.rate_limiting.RateLimiterProvider
import com.timmermans.email.rate_limiting.configuration.RateLimitRule
import com.timmermans.email.rate_limiting.configuration.RateLimiterProviderFromConfig
import com.timmermans.email.rate_limiting.configuration.RateLimitingConfig
import com.timmermans.email.rate_limiting.configuration.Type
import kotlin.time.Duration

fun buildRateLimiter(init: RateLimitProviderBuilder.() -> Unit): RateLimiterProvider {
    return RateLimitProviderBuilder().apply(init).build()
}


@DslMarker
annotation class RateLimitingDefinition

@RateLimitingDefinition
class RateLimitProviderBuilder {

    private val configs = mutableListOf<RateLimitingConfig>()

    /**
     * Applies N rules to multiples topics at once.
     * These rules are completely independent by topic.
     * If you're looking for a codependent behavior between topics, see to `sharedLimit()`
     */
    fun limit(vararg topics: EmailTopic, init: RulesScope.() -> Unit) {
        val rulesScope = RulesScope().apply(init)
        configs.add(RateLimitingConfig(Type.REGULAR, topics.toList(), rulesScope.rules))
    }

    /**
     * These topics are not subject to rate limiting.
     */
    fun unlimited(vararg topics: EmailTopic) {
        configs.add(RateLimitingConfig(Type.UNLIMITED, topics.toList()))
    }

    /**
     * These topics are always blocked.
     */
    fun prohibited(vararg topics: EmailTopic) {
        configs.add(RateLimitingConfig(Type.PROHIBITED, topics.toList()))
    }

    /**
     * These rules apply to a group of topics in the same way a shared pool behaves.
     *
     * Assuming topics A, B and C and rule of `1 every 1.hours`,
     * if at 00:00:00 an email is sent with topic A, no other emails can be sent with topics A, B, C until 01:00:00
     */
    fun sharedLimit(vararg topics: EmailTopic, init: RulesScope.() -> Unit) {
        val rulesScope = RulesScope().apply(init)
        configs.add(RateLimitingConfig(Type.SHARED, topics.toList(), rulesScope.rules))
    }

    fun build(): RateLimiterProvider {
        return RateLimiterProviderFromConfig(configs.toList())
    }
}

@RateLimitingDefinition
class RulesScope {
    private val _rules = mutableListOf<RateLimitRule>()

    val rules: List<RateLimitRule>
        get() = _rules

    fun withRules(vararg rules: RateLimitRule) {
        _rules.addAll(rules)
    }

}

infix fun Int.every(period: Duration) = RateLimitRule(this, period)
