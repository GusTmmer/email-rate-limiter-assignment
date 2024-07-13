import ch.qos.logback.classic.Logger
import org.junit.jupiter.api.extension.*
import org.slf4j.LoggerFactory

class LogErrorVerifierExtension : BeforeEachCallback, AfterEachCallback, ParameterResolver {

    companion object {
        const val APPENDER_KEY = "errorLogAppender"
    }

    override fun beforeEach(context: ExtensionContext?) {
        val logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger

        val errorLogAppender = ErrorLogAppender()
        errorLogAppender.context = logger.loggerContext
        errorLogAppender.start()

        logger.addAppender(errorLogAppender)
        getStore(context).put(APPENDER_KEY, errorLogAppender)
    }

    override fun afterEach(context: ExtensionContext?) {
        val errorLogAppender = getStore(context).get(APPENDER_KEY, ErrorLogAppender::class.java)
        if (
            (errorLogAppender.errorMessages.isNotEmpty() && !errorLogAppender.assertedHasLogErrors)
            || (errorLogAppender.errorMessages.isEmpty() && errorLogAppender.assertedHasLogErrors)
        ) {
            throw AssertionError("Error logs detected:\n${errorLogAppender.errorMessages.joinToString("\n")}")
        }

        val logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        logger.detachAppender(errorLogAppender)

        errorLogAppender.stop()
    }

    override fun supportsParameter(parameterContext: ParameterContext?, extensionContext: ExtensionContext?): Boolean {
        return parameterContext?.parameter?.type == ErrorLogAppender::class.java
    }

    override fun resolveParameter(p0: ParameterContext?, extensionContext: ExtensionContext?): Any {
        return getStore(extensionContext).get(APPENDER_KEY, ErrorLogAppender::class.java)
    }

    private fun getStore(context: ExtensionContext?): ExtensionContext.Store {
        return context!!.getStore(ExtensionContext.Namespace.create(LogErrorVerifierExtension::class.java))
    }
}
