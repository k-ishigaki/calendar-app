package io.github.kishigaki.calendar

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
