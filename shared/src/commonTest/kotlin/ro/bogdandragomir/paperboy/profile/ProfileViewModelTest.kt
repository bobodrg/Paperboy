package ro.bogdandragomir.paperboy.profile

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import ro.bogdandragomir.paperboy.brief.FakeInterestsRepository
import ro.bogdandragomir.paperboy.brief.FakePaperboyApi
import ro.bogdandragomir.paperboy.brief.FakeSavedRepository
import ro.bogdandragomir.paperboy.brief.story
import ro.bogdandragomir.paperboy.core.model.Topic
import ro.bogdandragomir.paperboy.data.local.Settings
import ro.bogdandragomir.paperboy.data.local.SettingsRepository
import ro.bogdandragomir.paperboy.data.local.ThemeMode
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

private class FakeSettingsRepository : SettingsRepository {
    override val settings = MutableStateFlow(Settings())

    override suspend fun setThemeMode(mode: ThemeMode) = settings.update { it.copy(themeMode = mode) }

    override suspend fun setMorningHour(hour: Int) = settings.update { it.copy(morningHour = hour) }

    override suspend fun setMorningNotification(enabled: Boolean) =
        settings.update {
            it.copy(morningNotification = enabled)
        }

    override suspend fun setEmailDigest(enabled: Boolean) = settings.update { it.copy(emailDigest = enabled) }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun showsTopicNamesSavedCountAndSettings() =
        runTest {
            val saved = FakeSavedRepository().apply { save(story("1")) }
            val settings = FakeSettingsRepository()
            val vm =
                ProfileViewModel(
                    interests = FakeInterestsRepository(setOf("ai", "tech")),
                    saved = saved,
                    settingsRepository = settings,
                    api = FakePaperboyApi(topics = { listOf(Topic("tech", "Tech"), Topic("ai", "AI", "tech")) }),
                )

            vm.uiState.test {
                val state = expectMostRecentItem()
                assertEquals(listOf("AI", "Tech"), state.topicNames)
                assertEquals(1, state.savedCount)

                vm.setThemeMode(ThemeMode.Dark)
                vm.setMorningHour(6)
                val updated = expectMostRecentItem().settings
                assertEquals(ThemeMode.Dark, updated.themeMode)
                assertEquals(6, updated.morningHour)
            }
        }

    @Test
    fun offlineShowsReadableTopicIds() =
        runTest {
            val vm =
                ProfileViewModel(
                    FakeInterestsRepository(setOf("sport")),
                    FakeSavedRepository(),
                    FakeSettingsRepository(),
                    FakePaperboyApi(topics = { error("offline") }),
                )

            vm.uiState.test { assertEquals(listOf("Sport"), expectMostRecentItem().topicNames) }
        }
}
