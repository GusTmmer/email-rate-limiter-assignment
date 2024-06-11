package com.timmermans.email.rate_limiting

import com.timmermans.SendEmailRequest
import com.timmermans.email.EmailSender
import com.timmermans.email.rate_limiting.definition.RateLimiterProvider
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(RateLimitedEmailSender::class.simpleName)

class RateLimitedException(message: String) : Exception(message)

class RateLimitedEmailSender(
    private val delegate: EmailSender,
    private val rateLimiterProvider: RateLimiterProvider,
) : EmailSender {

    override fun send(emailRequest: SendEmailRequest) {
        if (shouldRateLimit(emailRequest)) {
            val error = "Skipping ${emailRequest.topic} email due to rate limit"
            logger.warn(error)
            throw RateLimitedException(error)
        }

        delegate.send(emailRequest)
    }

    private fun shouldRateLimit(emailRequest: SendEmailRequest): Boolean {
        return rateLimiterProvider.forTopic(emailRequest.topic)
            .shouldRateLimit(emailRequest)
    }
}