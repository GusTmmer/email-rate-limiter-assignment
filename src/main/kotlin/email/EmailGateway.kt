package com.timmermans.email

import com.timmermans.persistence.DbOutboundEmail
import com.timmermans.persistence.Transactor
import org.hibernate.Session
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.LoggerFactory
import java.time.Clock

private val logger = LoggerFactory.getLogger(EmailGateway::class.simpleName)

class EmailGateway : EmailSender, KoinComponent {

    private val transactor: Transactor by inject()
    private val clock: Clock by inject()

    override fun send(emailRequest: SendEmailRequest) {
        transactor.withSession { session: Session ->
            val dbOutboundEmail = emailRequest.toDbOutboundEmail(clock)
            session.save(dbOutboundEmail)
        }

        logger.info("Sent `${emailRequest.topic}` email to user `${emailRequest.toUserId}`")
    }
}

fun SendEmailRequest.toDbOutboundEmail(clock: Clock) = DbOutboundEmail(
    userId = toUserId,
    topic = topic,
    contents = contents,
    sentAt = clock.instant(),
)