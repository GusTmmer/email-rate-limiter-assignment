import com.timmermans.mainModule
import com.timmermans.persistence.Transactor
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.module
import org.koin.test.KoinTest
import java.time.Clock

class TestInjectionExtension : BeforeEachCallback, AfterEachCallback, KoinTest {

    override fun beforeEach(p0: ExtensionContext?) {
        startKoin()
    }

    override fun afterEach(p0: ExtensionContext?) {
        stopKoin()
    }

    private fun startKoin() {
        startKoin {
            modules(mainModule, module {
                single { MutableClock() } withOptions { bind<Clock>() }
                single { TransactorForTest() } withOptions { bind<Transactor>() }
            })
        }
    }

}