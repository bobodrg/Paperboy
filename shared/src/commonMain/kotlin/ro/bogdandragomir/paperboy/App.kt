package ro.bogdandragomir.paperboy

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import ro.bogdandragomir.paperboy.brief.BriefRepository
import ro.bogdandragomir.paperboy.brief.BriefRoute
import ro.bogdandragomir.paperboy.brief.BriefViewModel
import ro.bogdandragomir.paperboy.brief.SampleBriefRepository
import ro.bogdandragomir.paperboy.designsystem.PaperboyTheme

val appModule =
    module {
        single<BriefRepository> { SampleBriefRepository() }
        viewModelOf(::BriefViewModel)
    }

/** Starts dependency injection. Called once from each platform's entry point. */
fun initKoin(config: KoinAppDeclaration? = null): KoinApplication =
    startKoin {
        config?.invoke(this)
        modules(appModule)
    }

@Composable
fun App() {
    PaperboyTheme {
        Surface {
            BriefRoute()
        }
    }
}
