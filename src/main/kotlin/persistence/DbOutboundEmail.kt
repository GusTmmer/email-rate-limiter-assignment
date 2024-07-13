package com.timmermans.persistence

import java.time.Instant
import javax.persistence.*

@Entity
@Table(name = "outbound_emails")
data class DbOutboundEmail(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "user_id", nullable = false)
    val userId: Long = 0,

    @Column(name = "topic", nullable = false)
    val topic: String = "UNDEFINED",

    @Column(name = "contents", nullable = false)
    val contents: String = "",

    @Column(name = "sent_at", nullable = false)
    val sentAt: Instant
) {
    fun toPlainEntity(): OutboundEmail {
        return OutboundEmail(
            id = id,
            userId = userId,
            topic = topic,
            contents = contents,
            sentAt = sentAt,
        )
    }
}
