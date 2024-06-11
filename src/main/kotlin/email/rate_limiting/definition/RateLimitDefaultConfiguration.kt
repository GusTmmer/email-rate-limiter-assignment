package com.timmermans.email.rate_limiting.definition

import com.timmermans.email.EmailTopic
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours


val rateLimiterProvider: RateLimiterProvider = rateLimited {
    prohibited(EmailTopic.UNDEFINED)

    limit(EmailTopic.NEWS) {
        withRules(
            1 every 2.days,
            3 every 7.days,
        )
    }

    sharedLimit(EmailTopic.NEWS, EmailTopic.MARKETING, EmailTopic.STATUS) {
        withRules(
            1 every 1.days,
            1 every 3.hours,
        )
    }

    unlimited(EmailTopic.SECURITY)
}




