package com.timmermans.email.rate_limiting.definition.configuration.file

import com.timmermans.email.EmailTopic
import com.timmermans.email.rate_limiting.definition.RateLimiter
import com.timmermans.email.rate_limiting.definition.RateLimiterProvider
import com.timmermans.email.rate_limiting.definition.configuration.RateLimitRule
import com.timmermans.email.rate_limiting.definition.configuration.SharedLimitRule
import com.timmermans.email.rate_limiting.definition.rate_limiter.ProhibitedRateLimiter
import com.timmermans.email.rate_limiting.definition.rate_limiter.RegularRateLimiter
import com.timmermans.email.rate_limiting.definition.rate_limiter.UnlimitedRateLimiter

class RateLimiterProvider(configs: List<RateLimitingConfig>) : RateLimiterProvider {

    private val cachedRateLimiters = mutableMapOf<EmailTopic, RateLimiter>()

    private val prohibitedTopics: Set<EmailTopic>
    private val unlimitedTopics: Set<EmailTopic>
    private val isolatedRulesByTopic: Map<EmailTopic, Set<RateLimitRule>>
    private val sharedRulesByTopic: Map<EmailTopic, Set<SharedLimitRule>>

    init {
        val prohibitedTopics = mutableSetOf<EmailTopic>()
        val unlimitedTopics = mutableSetOf<EmailTopic>()

        val isolatedRulesByTopic = mutableMapOf<EmailTopic, MutableSet<RateLimitRule>>()
        val sharedRulesByTopic = mutableMapOf<EmailTopic, MutableSet<SharedLimitRule>>()

        for (config in configs) {
            when (config.type) {
                Type.PROHIBITED -> {
                    prohibitedTopics.addAll(config.topics)
                }

                Type.UNLIMITED -> {
                    unlimitedTopics.addAll(config.topics)
                }

                Type.REGULAR -> {
                    for (t in config.topics.toSet()) {
                        isolatedRulesByTopic.getOrPut(t) { mutableSetOf() }.addAll(config.rules!!)
                    }
                }

                Type.SHARED -> {
                    val topics = config.topics.toSet()
                    val sharedLimitRule = SharedLimitRule(topics, config.rules!!.toSet())
                    for (t in topics) {
                        sharedRulesByTopic.getOrPut(t) { mutableSetOf() }.add(sharedLimitRule)
                    }
                }
            }
        }

        this.prohibitedTopics = prohibitedTopics.toSet()
        this.unlimitedTopics = unlimitedTopics.toSet()
        this.isolatedRulesByTopic = isolatedRulesByTopic.mapValues { it.value.toSet() }.toMap()
        this.sharedRulesByTopic = sharedRulesByTopic.mapValues { it.value.toSet() }.toMap()
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
}