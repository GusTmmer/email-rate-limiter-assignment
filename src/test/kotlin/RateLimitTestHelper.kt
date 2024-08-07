import com.timmermans.email.EmailSender
import com.timmermans.email.SendEmailRequest
import com.timmermans.email.rate_limiting.RateLimitedEmailSender
import com.timmermans.email.rate_limiting.RateLimitedException
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

object RateLimitTestHelper {
    fun assertNotRateLimited(userId: Long, topic: String, emailSender: RateLimitedEmailSender) {
        assertDoesNotThrow { sendMessageToUserWithTopic(userId, topic, emailSender) }
    }

    fun assertRateLimited(userId: Long, topic: String, emailSender: RateLimitedEmailSender) {
        assertThrows<RateLimitedException> { sendMessageToUserWithTopic(userId, topic, emailSender) }
    }

    private fun sendMessageToUserWithTopic(userId: Long, topic: String, emailSender: EmailSender) {
        emailSender.send(SendEmailRequest(toUserId = userId, topic = topic, contents = ""))
    }
}