package com.timmermans.email.rate_limiting.definition

import com.timmermans.SendEmailRequest

interface RateLimiter {
    fun shouldRateLimit(emailRequest: SendEmailRequest): Boolean
}
