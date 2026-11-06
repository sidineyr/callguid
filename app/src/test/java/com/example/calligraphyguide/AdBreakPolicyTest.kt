package com.sidineyr.callguide

import org.junit.Assert.*
import org.junit.Test

class AdBreakPolicyTest {
    @Test fun firstLetterAndFastRepetitionsDoNotInterruptLearning() {
        val policy = AdBreakPolicy(10_000)
        assertFalse(policy.shouldOffer(120_000))
        repeat(6) { policy.completedLesson() }
        assertFalse(policy.shouldOffer(99_999))
        assertTrue(policy.shouldOffer(100_000))
    }
    @Test fun aBreakRequiresBothNewLessonsAndNewElapsedTime() {
        val policy = AdBreakPolicy(0)
        repeat(6) { policy.completedLesson() }
        policy.offered(90_000)
        assertFalse(policy.shouldOffer(300_000))
        repeat(6) { policy.completedLesson() }
        assertFalse(policy.shouldOffer(179_999))
        assertTrue(policy.shouldOffer(180_000))
    }
}
