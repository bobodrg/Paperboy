package ro.bogdandragomir.paperboy.saved

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import ro.bogdandragomir.paperboy.brief.FakeSavedRepository
import ro.bogdandragomir.paperboy.brief.story
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class SavedViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun removeCanBeUndone() =
        runTest {
            val saved = FakeSavedRepository()
            saved.save(story("1"))
            saved.save(story("2"))
            val vm = SavedViewModel(saved)

            vm.remove(story("1"))
            assertEquals(listOf("2"), saved.saved.value.map { it.id })
            assertEquals("1", vm.lastRemoved.value?.id)

            vm.undoRemove()
            assertEquals(
                setOf("1", "2"),
                saved.saved.value
                    .map { it.id }
                    .toSet(),
            )
            assertNull(vm.lastRemoved.value)
        }
}
