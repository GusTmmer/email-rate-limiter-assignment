package com.timmermans.email.rate_limiting.rate_limiter

import com.timmermans.email.SendEmailRequest
import com.timmermans.email.rate_limiting.RateLimiter
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(UnlimitedRateLimiter::class.simpleName)

class UnlimitedRateLimiter : RateLimiter {
    override fun shouldRateLimit(emailRequest: SendEmailRequest): Boolean {
        logger.info("Skipping rate limit for email with topic `${emailRequest.topic}` - configured as unlimited")
        return false
    }
}