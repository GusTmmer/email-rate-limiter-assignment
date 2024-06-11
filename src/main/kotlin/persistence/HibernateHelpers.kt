package com.timmermans.persistence

import org.hibernate.Criteria
import org.hibernate.Session

inline fun <reified T> Session.createCriteria(): Criteria {
    return createCriteria(T::class.java)
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T> Criteria.collectAsList(): List<T> {
    return list() as List<T>
}
