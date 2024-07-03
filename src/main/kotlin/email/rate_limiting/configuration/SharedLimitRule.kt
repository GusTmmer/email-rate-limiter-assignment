package com.timmermans.email.rate_limiting.configuration

import com.timmermans.email.EmailTopic

data class SharedLimitRule(
    val groupTopics: Set<EmailTopic>,
    val rules: Set<RateLimitRule>,
)
