import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase

class ErrorLogAppender : AppenderBase<ILoggingEvent>() {
    private val _errorMessages = mutableListOf<String>()
    val errorMessages: List<String>
        get() = _errorMessages

    var assertedHasLogErrors = false
        private set

    override fun append(eventObject: ILoggingEvent) {
        if (eventObject.level.isGreaterOrEqual(ch.qos.logback.classic.Level.ERROR)) {
            _errorMessages.add(eventObject.formattedMessage)
        }
    }

    fun assertHasErrorLogs() {
        assertedHasLogErrors = true
    }
}