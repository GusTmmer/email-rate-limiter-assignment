package com.timmermans.email.rate_limiting.definition.rate_limiter

import com.timmermans.email.SendEmailRequest
import com.timmermans.email.rate_limiting.definition.RateLimiter
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(ProhibitedRateLimiter::class.simpleName)

class ProhibitedRateLimiter : RateLimiter {
    override fun shouldRateLimit(emailRequest: SendEmailRequest): Boolean {
        logger.info("Blocking email with topic `${emailRequest.topic}` - configured as prohibited")
        return true
    }
}