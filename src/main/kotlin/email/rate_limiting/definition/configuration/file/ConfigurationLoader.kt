package com.timmermans.email.rate_limiting.definition.configuration.file

import kotlinx.serialization.json.Json
import java.io.File

fun defaultConfiguration(): List<RateLimitingConfig> {
    return loadRateLimitConfig("src/main/resources/defaultConfiguration.json")
}

fun loadRateLimitConfig(filepath: String): List<RateLimitingConfig> {
    val contents = File(filepath).readText()
    return Json.decodeFromString<List<RateLimitingConfig>>(contents)
}
