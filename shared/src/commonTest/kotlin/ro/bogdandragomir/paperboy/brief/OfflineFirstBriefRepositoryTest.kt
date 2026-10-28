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
            val api = FakePaperboyApi(response = { Brief("2026-10-28", listOf(story("new"))) })
            val local = FakeBriefLocalDataSource(oldBrief)
            val repository =
                OfflineFirstBriefRepository(api, local, FakeInterestsRepository(setOf("tech", "ai"))) { 42 }

            val result = repository.refresh()

            assertTrue(result.isSuccess)
            assertEquals(listOf(listOf("ai", "tech") to 10), api.briefRequests)
            assertEquals(CachedBrief(Brief("2026-10-28", listOf(story("new"))), 42), local.stored.value)
        }

    @Test
    fun aFailedRefreshKeepsTheStoredBrief() =
        runTest {
            val api = FakePaperboyApi(response = { error("offline") })
            val local = FakeBriefLocalDataSource(oldBrief)
            val repository = OfflineFirstBriefRepository(api, local, FakeInterestsRepository(setOf("tech"))) { 42 }

            val result = repository.refresh()

            assertTrue(result.isFailure)
            assertEquals(oldBrief, local.stored.value)
        }

    @Test
    fun doesNotCallTheApiBeforeOnboarding() =
        runTest {
            val api = FakePaperboyApi()
            val repository =
                OfflineFirstBriefRepository(api, FakeBriefLocalDataSource(), FakeInterestsRepository()) { 42 }

            assertTrue(repository.refresh().isFailure)
            assertTrue(api.briefRequests.isEmpty())
        }
}
