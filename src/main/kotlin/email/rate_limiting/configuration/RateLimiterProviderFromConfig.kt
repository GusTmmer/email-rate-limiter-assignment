package com.timmermans.email.rate_limiting.configuration

import com.timmermans.email.rate_limiting.RateLimiter
import com.timmermans.email.rate_limiting.RateLimiterProvider
import com.timmermans.email.rate_limiting.rate_limiter.ProhibitedRateLimiter
import com.timmermans.email.rate_limiting.rate_limiter.RegularRateLimiter
import com.timmermans.email.rate_limiting.rate_limiter.UnlimitedRateLimiter
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(RateLimiterProviderFromConfig::class.simpleName)

class RateLimiterProviderFromConfig(private val configs: List<RateLimitingConfig>) : RateLimiterProvider {

    private val cachedRateLimiters = mutableMapOf<String, RateLimiter>()

    private val prohibitedTopics: Set<String>
    private val unlimitedTopics: Set<String>
    private val isolatedRulesByTopic: Map<String, Set<RateLimitRule>>
    private val sharedRulesByTopic: Map<String, Set<SharedLimitRule>>

    init {
        val prohibitedTopics = mutableSetOf<String>()
        val unlimitedTopics = mutableSetOf<String>()

        val isolatedRulesByTopic = mutableMapOf<String, MutableSet<RateLimitRule>>()
        val sharedRulesByTopic = mutableMapOf<String, MutableSet<SharedLimitRule>>()

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

        this.prohibitedTopics = prohibitedTopics.map { it.uppercase() }.toSet()
        this.unlimitedTopics = unlimitedTopics.map { it.uppercase() }.toSet()
        this.isolatedRulesByTopic = isolatedRulesByTopic.map { it.key.uppercase() to it.value.toSet() }.toMap()
        this.sharedRulesByTopic = sharedRulesByTopic.map { it.key.uppercase() to it.value.toSet() }.toMap()
    }

    fun getSourceConfigs() = configs

    override fun forTopic(topic: String): RateLimiter {
        val topicUppercase = topic.uppercase()
        return cachedRateLimiters.getOrPut(topicUppercase) {
            when (topicUppercase) {
                in prohibitedTopics -> ProhibitedRateLimiter()
                in unlimitedTopics -> UnlimitedRateLimiter()
                in isolatedRulesByTopic, in sharedRulesByTopic -> RegularRateLimiter(
                    isolatedRules = isolatedRulesByTopic[topicUppercase] ?: emptySet(),
                    sharedRules = sharedRulesByTopic[topicUppercase] ?: emptySet(),
                )

                else -> {
                    logger.error("Topic $topicUppercase does not have rate-limiting configuration")
                    ProhibitedRateLimiter()
                }
            }
        }
    }
}