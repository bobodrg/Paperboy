package ro.bogdandragomir.paperboy.brief

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import ro.bogdandragomir.paperboy.core.model.Brief
import ro.bogdandragomir.paperboy.core.model.Source
import ro.bogdandragomir.paperboy.core.model.Story
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

    @Test
    fun emitsReadyWithTheRepositoryStories() =
        runTest {
            val story = story("1")
            val viewModel = BriefViewModel(FakeBriefRepository(flowOf(Brief("2026-10-28", listOf(story)))))

            viewModel.uiState.test {
                assertEquals(BriefUiState.Ready(listOf(story)), awaitItem())
            }
        }

    @Test
    fun emitsErrorWhenTheRepositoryFails() =
        runTest {
            val viewModel = BriefViewModel(FakeBriefRepository(flow { error("offline") }))

            viewModel.uiState.test {
                assertEquals(BriefUiState.Error, awaitItem())
            }
        }

    private class FakeBriefRepository(
        private val brief: Flow<Brief>,
    ) : BriefRepository {
        override fun observeTodayBrief(): Flow<Brief> = brief
    }

    private fun story(id: String) =
        Story(
            id = id,
            headline = "Headline $id",
            summary = "Summary",
            url = "https://example.com/$id",
            source = Source("src", "Example", "https://example.com"),
            topicIds = listOf("tech"),
            publishedAtEpochMillis = 0,
        )
}
