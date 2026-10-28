package ro.bogdandragomir.paperboy

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import ro.bogdandragomir.paperboy.brief.BriefRepository
import ro.bogdandragomir.paperboy.brief.BriefRoute
import ro.bogdandragomir.paperboy.brief.BriefViewModel
import ro.bogdandragomir.paperboy.brief.OfflineFirstBriefRepository
import ro.bogdandragomir.paperboy.data.local.BriefLocalDataSource
import ro.bogdandragomir.paperboy.data.local.DataStoreInterestsRepository
import ro.bogdandragomir.paperboy.data.local.InterestsRepository
import ro.bogdandragomir.paperboy.data.local.PaperboyDatabase
import ro.bogdandragomir.paperboy.data.local.RoomBriefLocalDataSource
import ro.bogdandragomir.paperboy.data.remote.KtorPaperboyApi
import ro.bogdandragomir.paperboy.data.remote.PaperboyApi
import ro.bogdandragomir.paperboy.data.remote.createHttpClient
import ro.bogdandragomir.paperboy.designsystem.PaperboyTheme
import ro.bogdandragomir.paperboy.onboarding.OnboardingRoute
import ro.bogdandragomir.paperboy.onboarding.OnboardingViewModel
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Provides the database, the preferences store and the API address for this platform. */
expect val platformModule: Module

@OptIn(ExperimentalTime::class)
val appModule =
    module {
        single { createHttpClient() }
        single<PaperboyApi> { KtorPaperboyApi(get(), get()) }
        single { get<PaperboyDatabase>().briefDao() }
        single<BriefLocalDataSource> { RoomBriefLocalDataSource(get()) }
        single<InterestsRepository> { DataStoreInterestsRepository(get()) }
        single<BriefRepository> {
            OfflineFirstBriefRepository(get(), get(), get()) { Clock.System.now().toEpochMilliseconds() }
        }
        viewModelOf(::RootViewModel)
        viewModelOf(::BriefViewModel)
        viewModelOf(::OnboardingViewModel)
    }

/** Starts dependency injection. Called once from each platform's entry point. */
fun initKoin(config: KoinAppDeclaration? = null): KoinApplication =
    startKoin {
        config?.invoke(this)
        modules(platformModule, appModule)
    }

enum class RootState { Loading, NeedsOnboarding, Ready }

class RootViewModel(
    interests: InterestsRepository,
) : ViewModel() {
    val state: StateFlow<RootState> =
        interests.followedTopicIds
            .map { if (it.isNullOrEmpty()) RootState.NeedsOnboarding else RootState.Ready }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RootState.Loading)
}

@Composable
fun App() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader
            .Builder(context)
            .components { add(KtorNetworkFetcherFactory()) }
            .build()
    }
    PaperboyTheme {
        Surface {
            Root()
        }
    }
}

@Composable
private fun Root(viewModel: RootViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editingTopics by rememberSaveable { mutableStateOf(false) }

    when {
        // Reading DataStore takes a few milliseconds; show nothing rather than flash the wrong screen.
        state == RootState.Loading -> Unit
        state == RootState.NeedsOnboarding -> OnboardingRoute(isEditing = false, onDone = {})
        editingTopics -> OnboardingRoute(isEditing = true, onDone = { editingTopics = false })
        else -> BriefRoute(onEditTopics = { editingTopics = true })
    }
}
