package ro.bogdandragomir.paperboy

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import ro.bogdandragomir.paperboy.brief.BriefRepository
import ro.bogdandragomir.paperboy.brief.BriefRoute
import ro.bogdandragomir.paperboy.brief.BriefViewModel
import ro.bogdandragomir.paperboy.brief.FollowedTopics
import ro.bogdandragomir.paperboy.brief.OfflineFirstBriefRepository
import ro.bogdandragomir.paperboy.data.local.BriefLocalDataSource
import ro.bogdandragomir.paperboy.data.local.PaperboyDatabase
import ro.bogdandragomir.paperboy.data.local.RoomBriefLocalDataSource
import ro.bogdandragomir.paperboy.data.remote.BriefApi
import ro.bogdandragomir.paperboy.data.remote.KtorBriefApi
import ro.bogdandragomir.paperboy.data.remote.createHttpClient
import ro.bogdandragomir.paperboy.designsystem.PaperboyTheme
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Provides the database and the API address for this platform. */
expect val platformModule: Module

// Until onboarding exists, everyone follows these.
private val defaultTopics = listOf("tech", "ai", "science", "world")

@OptIn(ExperimentalTime::class)
val appModule =
    module {
        single { createHttpClient() }
        single<BriefApi> { KtorBriefApi(get(), get()) }
        single { get<PaperboyDatabase>().briefDao() }
        single<BriefLocalDataSource> { RoomBriefLocalDataSource(get()) }
        single<FollowedTopics> { FollowedTopics { defaultTopics } }
        single<BriefRepository> {
            OfflineFirstBriefRepository(get(), get(), get()) { Clock.System.now().toEpochMilliseconds() }
        }
        viewModelOf(::BriefViewModel)
    }

/** Starts dependency injection. Called once from each platform's entry point. */
fun initKoin(config: KoinAppDeclaration? = null): KoinApplication =
    startKoin {
        config?.invoke(this)
        modules(platformModule, appModule)
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
            BriefRoute()
        }
    }
}
