package com.timmermans.email.rate_limiting

interface RateLimiterProvider {
    fun forTopic(topic: String): RateLimiter
}