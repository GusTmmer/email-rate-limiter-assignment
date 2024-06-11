package com.timmermans.persistence

import com.timmermans.email.EmailTopic
import java.time.Instant

data class OutboundEmail(
    val id: Long,
    val userId: Long,
    val topic: EmailTopic,
    val contents: String,
    val sentAt: Instant,
)