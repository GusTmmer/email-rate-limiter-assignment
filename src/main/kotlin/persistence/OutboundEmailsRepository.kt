package com.timmermans.persistence

import org.hibernate.Session
import org.hibernate.criterion.Order
import org.hibernate.criterion.Restrictions
import java.time.Instant

class OutboundEmailsRepository {

    fun getOutboundEmailsWithTopicsForUser(
        session: Session,
        userId: Long,
        topics: Set<String>,
        after: Instant,
    ): List<DbOutboundEmail> {
        return session.createCriteria<DbOutboundEmail>()
            .add(Restrictions.eq("userId", userId))
            .add(Restrictions.`in`("topic", topics))
            .add(Restrictions.ge("sentAt", after))
            .addOrder(Order.desc("sentAt"))
            .collectAsList<DbOutboundEmail>()
    }
}