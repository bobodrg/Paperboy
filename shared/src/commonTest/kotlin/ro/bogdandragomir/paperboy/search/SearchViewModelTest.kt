package ro.bogdandragomir.paperboy.search

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import ro.bogdandragomir.paperboy.brief.FakePaperboyApi
import ro.bogdandragomir.paperboy.brief.FakeSavedRepository
import ro.bogdandragomir.paperboy.brief.story
import ro.bogdandragomir.paperboy.data.local.RecentSearchesRepository
import ro.bogdandragomir.paperboy.story.RecentStories
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private class FakeRecentSearches : RecentSearchesRepository {
    override val recent = MutableStateFlow<List<String>>(emptyList())

    override suspend fun add(query: String) = recent.update { listOf(query) + it.filterNot { q -> q == query } }

    override suspend fun clear() {
        recent.value = emptyList()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun waitsForAPauseInTypingThenSearchesOnce() =
        runTest(dispatcher) {
            val api = FakePaperboyApi(searchResults = { listOf(story("hit")) })
            val recentStories = RecentStories()
            val vm = SearchViewModel(api, FakeSavedRepository(), FakeRecentSearches(), recentStories)

            vm.uiState.test {
                awaitItem()
                vm.onQueryChange("a")
                vm.onQueryChange("ap")
                vm.onQueryChange("apple")
                advanceTimeBy(400)

                val found = expectMostRecentItem().results
                assertIs<SearchResults.Found>(found)
                assertEquals(listOf("hit"), found.stories.map { it.id })
                assertEquals(listOf("apple"), api.searches)
                // Results can be opened from the story screen.
                assertEquals("hit", recentStories.observe("hit").first()?.id)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun offlineSearchesSavedStories() =
        runTest(dispatcher) {
            val saved = FakeSavedRepository().apply { save(story("1")) }
            val api = FakePaperboyApi(searchResults = { error("offline") })
            val vm = SearchViewModel(api, saved, FakeRecentSearches(), RecentStories())

            vm.uiState.test {
                awaitItem()
                vm.onQueryChange("headline 1")
                advanceTimeBy(400)

                val found = expectMostRecentItem().results
                assertIs<SearchResults.Found>(found)
                assertTrue(found.offline)
                assertEquals(listOf("1"), found.stories.map { it.id })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun remembersRecentSearches() =
        runTest(dispatcher) {
            val recent = FakeRecentSearches()
            val vm = SearchViewModel(FakePaperboyApi(), FakeSavedRepository(), recent, RecentStories())

            vm.onQueryChange("  climate  ")
            vm.rememberQuery()
            advanceTimeBy(10)

            assertEquals(listOf("climate"), recent.recent.value)
        }
}
