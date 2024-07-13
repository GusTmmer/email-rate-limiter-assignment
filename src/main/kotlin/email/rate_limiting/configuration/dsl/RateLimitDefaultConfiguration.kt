package com.timmermans.email.rate_limiting.configuration.dsl

import com.timmermans.email.rate_limiting.RateLimiterProvider
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

val rateLimiterProvider: RateLimiterProvider = buildRateLimiter {
    limit("NEWS") {
        withRules(
            1 every 2.days,
            3 every 7.days,
        )
    }

    sharedLimit("NEWS", "MARKETING", "STATUS") {
        withRules(
            1 every 1.days,
            1 every 3.hours,
        )
    }

    unlimited("SECURITY")
}




