package com.timmermans.email.rate_limiting.definition

import com.timmermans.email.EmailTopic

interface RateLimiterProvider {
    fun forTopic(topic: EmailTopic): RateLimiter
}