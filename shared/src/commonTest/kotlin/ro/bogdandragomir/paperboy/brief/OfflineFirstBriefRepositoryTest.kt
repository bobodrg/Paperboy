package ro.bogdandragomir.paperboy.brief

import kotlinx.coroutines.test.runTest
import ro.bogdandragomir.paperboy.core.model.Brief
import ro.bogdandragomir.paperboy.data.local.CachedBrief
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OfflineFirstBriefRepositoryTest {
    private val oldBrief = CachedBrief(Brief("2026-10-27", listOf(story("old"))), fetchedAtEpochMillis = 1)

    @Test
    fun refreshAsksForTheFollowedTopicsAndStoresTheResult() =
        runTest {
            val api = FakeBriefApi { Brief("2026-10-28", listOf(story("new"))) }
            val local = FakeBriefLocalDataSource(oldBrief)
            val repository = OfflineFirstBriefRepository(api, local, { listOf("tech", "ai") }) { 42 }

            val result = repository.refresh()

            assertTrue(result.isSuccess)
            assertEquals(listOf(listOf("tech", "ai") to 10), api.requests)
            assertEquals(CachedBrief(Brief("2026-10-28", listOf(story("new"))), 42), local.stored.value)
        }

    @Test
    fun aFailedRefreshKeepsTheStoredBrief() =
        runTest {
            val api = FakeBriefApi { error("offline") }
            val local = FakeBriefLocalDataSource(oldBrief)
            val repository = OfflineFirstBriefRepository(api, local, { listOf("tech") }) { 42 }

            val result = repository.refresh()

            assertTrue(result.isFailure)
            assertEquals(oldBrief, local.stored.value)
        }
}
