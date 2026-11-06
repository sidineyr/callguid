package com.sidineyr.callguide

/** No ad on startup, no ad per letter, and never wait for the network. */
class AdBreakPolicy(private val sessionStart: Long) {
    private var completed = 0
    private var lastBreak = sessionStart
    fun completedLesson() { completed++ }
    fun shouldOffer(now: Long): Boolean = completed >= 6 && now - lastBreak >= 90_000L
    fun offered(now: Long) { completed = 0; lastBreak = now }
}
