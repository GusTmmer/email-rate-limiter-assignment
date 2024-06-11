package com.timmermans

import com.timmermans.email.EmailTopic

data class SendEmailRequest(
    val toUserId: Long,
    val topic: EmailTopic,
    val contents: String,
)