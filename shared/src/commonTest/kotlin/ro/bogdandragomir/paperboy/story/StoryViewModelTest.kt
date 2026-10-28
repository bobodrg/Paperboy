package ro.bogdandragomir.paperboy.story

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import ro.bogdandragomir.paperboy.brief.FakeBriefLocalDataSource
import ro.bogdandragomir.paperboy.brief.FakeInterestsRepository
import ro.bogdandragomir.paperboy.brief.FakePaperboyApi
import ro.bogdandragomir.paperboy.brief.OfflineFirstBriefRepository
import ro.bogdandragomir.paperboy.brief.story
import ro.bogdandragomir.paperboy.core.model.Brief
import ro.bogdandragomir.paperboy.data.local.CachedBrief
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class StoryViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val local = FakeBriefLocalDataSource(CachedBrief(Brief("2026-10-28", listOf(story("1"), story("2"))), 0))
    private val repository = OfflineFirstBriefRepository(FakePaperboyApi(), local, FakeInterestsRepository()) { 0 }

    @Test
    fun showsTheStoryFromTheStoredBrief() =
        runTest {
            StoryViewModel("2", repository).uiState.test {
                assertEquals(StoryUiState.Ready(story("2")), awaitItem())
            }
        }

    @Test
    fun saysWhenTheStoryLeftTheBrief() =
        runTest {
            val vm = StoryViewModel("1", repository)

            vm.uiState.test {
                assertEquals(StoryUiState.Ready(story("1")), awaitItem())

                local.replace(Brief("2026-10-29", listOf(story("3"))), 1)

                assertEquals(StoryUiState.NotFound, awaitItem())
            }
        }
}
