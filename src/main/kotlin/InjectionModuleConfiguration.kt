package com.timmermans

import com.timmermans.email.EmailGateway
import com.timmermans.email.EmailSender
import com.timmermans.email.rate_limiting.RateLimitedEmailSender
import com.timmermans.email.rate_limiting.definition.RateLimiterProvider
import com.timmermans.email.rate_limiting.definition.configuration.dsl.rateLimiterProvider
import com.timmermans.persistence.OutboundEmailsRepository
import com.timmermans.persistence.Transactor
import org.koin.dsl.module
import java.time.Clock

val mainModule = module {
    single { Transactor() }
    single { OutboundEmailsRepository() }

    single<RateLimitedEmailSender> { (p: RateLimiterProvider) -> RateLimitedEmailSender(EmailGateway(), p) }
    single<EmailSender> { RateLimitedEmailSender(EmailGateway(), rateLimiterProvider) }

    single<Clock> { Clock.systemUTC() }
}