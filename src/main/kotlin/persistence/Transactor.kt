package com.timmermans.persistence

import org.hibernate.Session
import org.hibernate.SessionFactory
import org.hibernate.cfg.Configuration

open class Transactor {

    protected companion object {
        val sessionFactory: SessionFactory = Configuration().configure().buildSessionFactory()
    }

    open fun <T> withSession(transaction: (Session) -> T): T {
        val session = sessionFactory.openSession()

        return try {
            session.beginTransaction()
            transaction(session).also { session.transaction.commit() }
        } catch (e: Exception) {
            session.transaction.rollback()
            throw e
        } finally {
            session.close()
        }
    }
}