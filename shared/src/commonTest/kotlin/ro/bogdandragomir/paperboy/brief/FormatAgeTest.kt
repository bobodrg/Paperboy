package ro.bogdandragomir.paperboy.brief

import kotlin.test.Test
import kotlin.test.assertEquals

class FormatAgeTest {
    private val minute = 60_000L

    @Test
    fun formatsMinutesHoursAndDays() {
        assertEquals("Just now", formatAge(0, 30_000))
        assertEquals("25m ago", formatAge(0, 25 * minute))
        assertEquals("3h ago", formatAge(0, 3 * 60 * minute + 59 * minute))
        assertEquals("2d ago", formatAge(0, 2 * 24 * 60 * minute))
    }

    @Test
    fun futureTimesCountAsJustNow() {
        assertEquals("Just now", formatAge(10 * minute, 0))
    }
}
