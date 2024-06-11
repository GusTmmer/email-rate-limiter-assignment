import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.*
import kotlin.time.Duration
import kotlin.time.toJavaDuration

class MutableClock(
    private var instant: Instant = Instant.parse("2024-01-01T00:00:00Z"),
    private val zone: ZoneId = ZoneId.of("UTC"),
) : Clock() {

    override fun withZone(zone: ZoneId): Clock {
        return MutableClock(instant, zone)
    }

    override fun getZone(): ZoneId {
        return zone
    }

    override fun instant(): Instant {
        return instant
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is Clock) return false
        return instant == other.instant() && zone == other.zone
    }

    override fun hashCode(): Int {
        return Objects.hash(super.hashCode(), instant, zone)
    }

    override fun toString(): String {
        return String.format("MutableClock[%s,%s]", instant, zone)
    }

    fun advanceBy(duration: Duration) {
        instant = instant.plus(duration.toJavaDuration())
    }

    fun set(newInstant: Instant) {
        instant = newInstant
    }
}