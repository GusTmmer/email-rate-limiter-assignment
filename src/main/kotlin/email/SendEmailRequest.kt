package com.timmermans.email

data class SendEmailRequest(
    val toUserId: Long,
    val topic: String,
    val contents: String,
)