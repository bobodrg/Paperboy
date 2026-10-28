package ro.bogdandragomir.paperboy

import androidx.room.Room
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask
import ro.bogdandragomir.paperboy.data.local.PaperboyDatabase
import ro.bogdandragomir.paperboy.data.local.buildPaperboyDatabase
import ro.bogdandragomir.paperboy.data.remote.ApiConfig

actual val platformModule: Module =
    module {
        single {
            Room
                .databaseBuilder<PaperboyDatabase>(name = "${documentDirectory()}/paperboy.db")
                .buildPaperboyDatabase()
        }
        // The iOS simulator shares the Mac's network, so localhost is the Mac.
        single { ApiConfig(baseUrl = "http://localhost:8080") }
    }

@OptIn(ExperimentalForeignApi::class)
private fun documentDirectory(): String {
    val url =
        NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
    return requireNotNull(url?.path) { "No documents directory" }
}
