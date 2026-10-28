package ro.bogdandragomir.paperboy.brief

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import ro.bogdandragomir.paperboy.core.model.Brief
import ro.bogdandragomir.paperboy.data.local.CachedBrief
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class BriefViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        api: FakePaperboyApi,
        local: FakeBriefLocalDataSource,
        interests: FakeInterestsRepository = FakeInterestsRepository(setOf("tech")),
    ) = BriefViewModel(OfflineFirstBriefRepository(api, local, interests) { 100 }, interests)

    private fun ready(
        vararg ids: String,
        fetchedAt: Long = 100,
        isStale: Boolean = false,
    ) = BriefUiState.Ready(ids.map(::story), fetchedAt, isRefreshing = false, isStale = isStale)

    private fun briefOf(vararg ids: String) = Brief("2026-10-28", ids.map(::story))

    @Test
    fun showsTheDownloadedBrief() =
        runTest {
            val vm = viewModel(FakePaperboyApi(response = { briefOf("1") }), FakeBriefLocalDataSource())

            vm.uiState.test { assertEquals(ready("1"), awaitItem()) }
        }

    @Test
    fun offlineWithAStoredBriefShowsItAsStale() =
        runTest {
            val stored = CachedBrief(Brief("2026-10-27", listOf(story("old"))), fetchedAtEpochMillis = 7)
            val vm = viewModel(FakePaperboyApi(response = { error("offline") }), FakeBriefLocalDataSource(stored))

            vm.uiState.test { assertEquals(ready("old", fetchedAt = 7, isStale = true), awaitItem()) }
        }

    @Test
    fun offlineWithNothingStoredShowsTheErrorState() =
        runTest {
            val vm = viewModel(FakePaperboyApi(response = { error("offline") }), FakeBriefLocalDataSource())

            vm.uiState.test { assertEquals(BriefUiState.Error, awaitItem()) }
        }

    @Test
    fun retryAfterAnErrorShowsTheBrief() =
        runTest {
            val api = FakePaperboyApi(response = { error("offline") })
            val vm = viewModel(api, FakeBriefLocalDataSource())

            vm.uiState.test {
                assertEquals(BriefUiState.Error, awaitItem())

                api.response = { briefOf("1") }
                vm.refresh()

                assertEquals(ready("1"), expectMostRecentItem())
            }
        }

    @Test
    fun changingTopicsDownloadsANewBrief() =
        runTest {
            val api = FakePaperboyApi(response = { briefOf("1") })
            val interests = FakeInterestsRepository(setOf("tech"))
            val vm = viewModel(api, FakeBriefLocalDataSource(), interests)

            vm.uiState.test {
                awaitItem()
                interests.setFollowedTopics(setOf("sport", "world", "tech"))
                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(listOf(listOf("tech"), listOf("sport", "tech", "world")), api.briefRequests.map { it.first })
        }
}
