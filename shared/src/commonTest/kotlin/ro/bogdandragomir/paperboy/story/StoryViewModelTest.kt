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
import ro.bogdandragomir.paperboy.brief.FakeSavedRepository
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
    private val brief = OfflineFirstBriefRepository(FakePaperboyApi(), local, FakeInterestsRepository()) { 0 }
    private val saved = FakeSavedRepository()
    private val recent = RecentStories()
    private val lookup = StoryLookup(brief, saved, recent)

    @Test
    fun showsTheStoryFromTheStoredBrief() =
        runTest {
            StoryViewModel("2", lookup, saved).uiState.test {
                assertEquals(StoryUiState.Ready(story("2"), isSaved = false), awaitItem())
            }
        }

    @Test
    fun findsSavedStoriesAfterTheBriefMovedOn() =
        runTest {
            saved.save(story("1"))
            local.replace(Brief("2026-10-29", listOf(story("3"))), 1)

            StoryViewModel("1", lookup, saved).uiState.test {
                assertEquals(StoryUiState.Ready(story("1"), isSaved = true), awaitItem())
            }
        }

    @Test
    fun findsStoriesSeenInSearchResults() =
        runTest {
            recent.remember(listOf(story("search-hit")))

            StoryViewModel("search-hit", lookup, saved).uiState.test {
                assertEquals(StoryUiState.Ready(story("search-hit"), isSaved = false), awaitItem())
            }
        }

    @Test
    fun saysWhenTheStoryIsGoneEverywhere() =
        runTest {
            StoryViewModel("missing", lookup, saved).uiState.test {
                assertEquals(StoryUiState.NotFound, awaitItem())
            }
        }

    @Test
    fun toggleSaveSavesAndUnsaves() =
        runTest {
            val vm = StoryViewModel("2", lookup, saved)
            vm.uiState.test {
                assertEquals(false, (awaitItem() as StoryUiState.Ready).isSaved)
                vm.toggleSave()
                assertEquals(true, (awaitItem() as StoryUiState.Ready).isSaved)
            }
        }
}
