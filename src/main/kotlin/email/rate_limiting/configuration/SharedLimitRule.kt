package com.timmermans.email.rate_limiting.configuration

data class SharedLimitRule(
    val groupTopics: Set<String>,
    val rules: Set<RateLimitRule>,
)
