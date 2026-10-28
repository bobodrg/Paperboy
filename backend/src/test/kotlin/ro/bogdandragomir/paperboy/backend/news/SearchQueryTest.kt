package ro.bogdandragomir.paperboy.backend.news

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SearchQueryTest {
    @Test
    fun everyWordMustMatchAndTheLastMayBeUnfinished() {
        assertEquals("apple & vision & p:*", SearchQuery.toTsQuery("Apple Vision P"))
    }

    @Test
    fun onlyLettersAndDigitsSurvive() {
        assertEquals("drop & table:*", SearchQuery.toTsQuery("'); DROP TABLE --"))
        assertEquals("românia:*", SearchQuery.toTsQuery("România!"))
    }

    @Test
    fun nothingToSearchForGivesNull() {
        assertNull(SearchQuery.toTsQuery("  !?  "))
    }

    @Test
    fun likeWildcardsAreEscaped() {
        assertEquals("100\\%\\_off", SearchQuery.escapeLike("100%_off"))
    }
}
