package com.timmermans.email

data class SendEmailRequest(
    val toUserId: Long,
    val topic: EmailTopic,
    val contents: String,
)