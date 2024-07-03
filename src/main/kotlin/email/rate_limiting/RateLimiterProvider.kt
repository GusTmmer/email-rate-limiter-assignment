package com.timmermans.email.rate_limiting

import com.timmermans.email.EmailTopic

interface RateLimiterProvider {
    fun forTopic(topic: EmailTopic): RateLimiter
}