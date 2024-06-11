package com.timmermans

import org.koin.core.context.GlobalContext.startKoin
import java.util.*

fun main() {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

    startKoin {
        modules(mainModule)
    }

    // Add listener / queue consumer of email requests

}