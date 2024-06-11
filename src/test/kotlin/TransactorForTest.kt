import com.timmermans.persistence.Transactor
import org.hibernate.Session

class TransactorForTest : Transactor() {

    companion object {
        private var session: Session? = null
    }

    fun startNewTransaction() {
        if (session == null) {
            session = sessionFactory.openSession().also { it.beginTransaction() }
        }
    }

    fun rollbackAndClose() {
        session?.run {
            transaction.rollback()
            close()
        }

        session = null
    }

    override fun <T> withSession(transaction: (Session) -> T): T {
        if (session == null) {
            startNewTransaction()
        }

        return try {
            transaction(session!!)
        } catch (e: Exception) {
            rollbackAndClose()
            throw e
        }
    }
}