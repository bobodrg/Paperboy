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
        api: FakeBriefApi,
        local: FakeBriefLocalDataSource,
    ) = BriefViewModel(OfflineFirstBriefRepository(api, local, { listOf("tech") }) { 100 })

    @Test
    fun showsTheDownloadedBrief() =
        runTest {
            val vm = viewModel(FakeBriefApi { Brief("2026-10-28", listOf(story("1"))) }, FakeBriefLocalDataSource())

            vm.uiState.test {
                assertEquals(
                    BriefUiState.Ready(
                        listOf(story("1")),
                        fetchedAtEpochMillis = 100,
                        isRefreshing = false,
                        isStale = false,
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun offlineWithAStoredBriefShowsItAsStale() =
        runTest {
            val stored = CachedBrief(Brief("2026-10-27", listOf(story("old"))), fetchedAtEpochMillis = 7)
            val vm = viewModel(FakeBriefApi { error("offline") }, FakeBriefLocalDataSource(stored))

            vm.uiState.test {
                assertEquals(
                    BriefUiState.Ready(
                        listOf(story("old")),
                        fetchedAtEpochMillis = 7,
                        isRefreshing = false,
                        isStale = true,
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun offlineWithNothingStoredShowsTheErrorState() =
        runTest {
            val vm = viewModel(FakeBriefApi { error("offline") }, FakeBriefLocalDataSource())

            vm.uiState.test {
                assertEquals(BriefUiState.Error, awaitItem())
            }
        }

    @Test
    fun retryAfterAnErrorShowsTheBrief() =
        runTest {
            val api = FakeBriefApi { error("offline") }
            val vm = viewModel(api, FakeBriefLocalDataSource())

            vm.uiState.test {
                assertEquals(BriefUiState.Error, awaitItem())

                api.response = { Brief("2026-10-28", listOf(story("1"))) }
                vm.refresh()

                assertEquals(
                    BriefUiState.Ready(
                        listOf(story("1")),
                        fetchedAtEpochMillis = 100,
                        isRefreshing = false,
                        isStale = false,
                    ),
                    expectMostRecentItem(),
                )
            }
        }
}
