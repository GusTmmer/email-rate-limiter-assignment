import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.koin.core.component.inject
import org.koin.test.KoinTest

class TransactionalExtension() : BeforeEachCallback, AfterEachCallback, KoinTest {

    private val transactor: TransactorForTest by inject()

    override fun beforeEach(p0: ExtensionContext?) {
        transactor.startNewTransaction()
    }

    override fun afterEach(p0: ExtensionContext?) {
        transactor.rollbackAndClose()
    }
}