package com.timmermans.email.rate_limiting.configuration

import com.timmermans.email.EmailTopic
import com.timmermans.email.rate_limiting.RateLimiter
import com.timmermans.email.rate_limiting.RateLimiterProvider
import com.timmermans.email.rate_limiting.rate_limiter.ProhibitedRateLimiter
import com.timmermans.email.rate_limiting.rate_limiter.RegularRateLimiter
import com.timmermans.email.rate_limiting.rate_limiter.UnlimitedRateLimiter
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(RateLimiterProviderFromConfig::class.simpleName)

class RateLimiterProviderFromConfig(configs: List<RateLimitingConfig>) : RateLimiterProvider {

    private val cachedRateLimiters = mutableMapOf<EmailTopic, RateLimiter>()

    private val prohibitedTopics: Set<EmailTopic>
    private val unlimitedTopics: Set<EmailTopic>
    private val isolatedRulesByTopic: Map<EmailTopic, Set<RateLimitRule>>
    private val sharedRulesByTopic: Map<EmailTopic, Set<SharedLimitRule>>

    companion object {
        private fun validateConfig(configs: List<RateLimitingConfig>): List<RateLimitingConfig> {
            val configuredTopics = configs.flatMap { it.topics }.distinct().toSet()

            val missingConfiguration = EmailTopic.entries.filter { it !in configuredTopics }
                .takeIf { it.isNotEmpty() }
                ?.also { topics -> logger.warn("No configuration for $topics. Setting as PROHIBITED") }
                ?.let { topics -> RateLimitingConfig(Type.PROHIBITED, topics) }

            return missingConfiguration?.let { configs + it } ?: configs
        }
    }

    init {
        val validatedConfigs = validateConfig(configs)

        val prohibitedTopics = mutableSetOf<EmailTopic>()
        val unlimitedTopics = mutableSetOf<EmailTopic>()

        val isolatedRulesByTopic = mutableMapOf<EmailTopic, MutableSet<RateLimitRule>>()
        val sharedRulesByTopic = mutableMapOf<EmailTopic, MutableSet<SharedLimitRule>>()

        for (config in validatedConfigs) {
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