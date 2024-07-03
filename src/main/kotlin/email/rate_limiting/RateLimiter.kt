package com.timmermans.email.rate_limiting

import com.timmermans.email.SendEmailRequest

interface RateLimiter {
    fun shouldRateLimit(emailRequest: SendEmailRequest): Boolean
}
