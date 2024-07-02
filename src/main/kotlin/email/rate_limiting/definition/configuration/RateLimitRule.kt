package com.timmermans.email.rate_limiting.definition.configuration

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Serializable
data class RateLimitRule(
    val limit: Int,
    @Serializable(with = RateLimitRuleTimeWindowSerializer::class) val timeWindow: Duration,
)

private object RateLimitRuleTimeWindowSerializer : KSerializer<Duration> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("RateLimitRule.Window") {
        element<Long>("size")
        element<String>("timeUnit")
    }

    override fun serialize(encoder: Encoder, value: Duration) {
        val compositeEncoder = encoder.beginStructure(descriptor)
        val timeUnit = when {
            value.inWholeDays > 0 -> DurationUnit.DAYS
            value.inWholeHours > 0 -> DurationUnit.HOURS
            value.inWholeMinutes > 0 -> DurationUnit.MINUTES
            else -> DurationUnit.SECONDS
        }
        compositeEncoder.encodeLongElement(descriptor, 0, value.toLong(timeUnit))
        compositeEncoder.encodeStringElement(descriptor, 1, timeUnit.name)
        compositeEncoder.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): Duration {
        val dec = decoder.beginStructure(descriptor)
        var size: Long? = null
        var timeUnit: String? = null
        loop@ while (true) {
            when (val index = dec.decodeElementIndex(descriptor)) {
                0 -> size = dec.decodeLongElement(descriptor, 0)
                1 -> timeUnit = dec.decodeStringElement(descriptor, 1)
                CompositeDecoder.DECODE_DONE -> break@loop
                else -> throw SerializationException("Unknown index $index")
            }
        }
        dec.endStructure(descriptor)
        return size!!.toDuration(DurationUnit.valueOf(timeUnit!!))
    }
}
