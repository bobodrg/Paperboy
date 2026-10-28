package ro.bogdandragomir.paperboy.onboarding

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import ro.bogdandragomir.paperboy.brief.FakeInterestsRepository
import ro.bogdandragomir.paperboy.brief.FakePaperboyApi
import ro.bogdandragomir.paperboy.core.model.Topic
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val topics =
        listOf(
            Topic("tech", "Tech"),
            Topic("ai", "AI", parentId = "tech"),
            Topic("science", "Science"),
            Topic("world", "World"),
        )

    @Test
    fun subTopicsAppearOnlyUnderASelectedParent() {
        val vm = OnboardingViewModel(FakePaperboyApi(topics = { topics }), FakeInterestsRepository())
        assertEquals(
            listOf("tech", "science", "world"),
            vm.uiState.value.visibleTopics
                .map { it.id },
        )

        vm.toggle("tech")

        assertEquals(
            listOf("tech", "ai", "science", "world"),
            vm.uiState.value.visibleTopics
                .map { it.id },
        )
    }

    @Test
    fun unselectingAParentAlsoUnselectsItsSubTopics() {
        val vm = OnboardingViewModel(FakePaperboyApi(topics = { topics }), FakeInterestsRepository())
        vm.toggle("tech")
        vm.toggle("ai")

        vm.toggle("tech")

        assertEquals(emptySet(), vm.uiState.value.selected)
    }

    @Test
    fun savesOnlyWithAtLeastThreeTopics() =
        runTest {
            val interests = FakeInterestsRepository()
            val vm = OnboardingViewModel(FakePaperboyApi(topics = { topics }), interests)
            var saved = false

            vm.toggle("tech")
            vm.toggle("ai")
            assertFalse(vm.uiState.value.canSave)
            vm.save { saved = true }
            assertNull(interests.followedTopicIds.value)

            vm.toggle("world")
            vm.save { saved = true }

            assertTrue(saved)
            assertEquals(setOf("tech", "ai", "world"), interests.followedTopicIds.value)
        }

    @Test
    fun editingStartsFromTheCurrentTopics() {
        val vm =
            OnboardingViewModel(
                FakePaperboyApi(topics = { topics }),
                FakeInterestsRepository(setOf("science", "world", "tech")),
            )

        assertEquals(setOf("science", "world", "tech"), vm.uiState.value.selected)
    }

    @Test
    fun aFailedLoadCanBeRetried() {
        val api = FakePaperboyApi(topics = { error("offline") })
        val vm = OnboardingViewModel(api, FakeInterestsRepository())
        assertTrue(vm.uiState.value.loadFailed)

        api.topics = { topics }
        vm.load()

        assertFalse(vm.uiState.value.loadFailed)
        assertEquals(4, vm.uiState.value.topics.size)
    }
}
