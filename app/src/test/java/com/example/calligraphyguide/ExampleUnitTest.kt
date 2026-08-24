package com.sidineyr.callguide

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun exerciseSequence_wrapsToFirstItem() {
        val exercises = listOf("a", "m", "s", "g", "Brasil")
        val nextIndex = (exercises.lastIndex + 1) % exercises.size
        assertEquals("a", exercises[nextIndex])
    }
}
