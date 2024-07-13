package com.timmermans.persistence

import java.time.Instant

data class OutboundEmail(
    val id: Long,
    val userId: Long,
    val topic: String,
    val contents: String,
    val sentAt: Instant,
)