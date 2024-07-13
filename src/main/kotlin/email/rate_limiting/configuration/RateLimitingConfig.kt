package com.timmermans.email.rate_limiting.configuration

import kotlinx.serialization.Serializable


@Serializable
enum class Type { REGULAR, SHARED, UNLIMITED, PROHIBITED }

@Serializable
data class RateLimitingConfig(
    val type: Type,
    val topics: List<String>,
    val rules: List<RateLimitRule>? = null,
)
