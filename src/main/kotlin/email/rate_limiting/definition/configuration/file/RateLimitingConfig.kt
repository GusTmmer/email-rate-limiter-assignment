package com.timmermans.email.rate_limiting.definition.configuration.file

import com.timmermans.email.EmailTopic
import com.timmermans.email.rate_limiting.definition.configuration.RateLimitRule
import kotlinx.serialization.Serializable


@Serializable
enum class Type { REGULAR, SHARED, UNLIMITED, PROHIBITED }


@Serializable
data class RateLimitingConfig(
    val type: Type,
    val topics: List<EmailTopic>,
    val rules: List<RateLimitRule>? = null,
)
