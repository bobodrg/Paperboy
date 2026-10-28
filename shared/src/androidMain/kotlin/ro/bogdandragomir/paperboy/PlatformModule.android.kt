package ro.bogdandragomir.paperboy

import androidx.room.Room
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import ro.bogdandragomir.paperboy.data.local.PREFERENCES_FILE_NAME
import ro.bogdandragomir.paperboy.data.local.PaperboyDatabase
import ro.bogdandragomir.paperboy.data.local.buildPaperboyDatabase
import ro.bogdandragomir.paperboy.data.local.createPreferencesDataStore
import ro.bogdandragomir.paperboy.data.remote.ApiConfig

actual val platformModule: Module =
    module {
        single {
            val context = androidContext()
            Room
                .databaseBuilder<PaperboyDatabase>(context, context.getDatabasePath("paperboy.db").absolutePath)
                .buildPaperboyDatabase()
        }
        single { createPreferencesDataStore(androidContext().filesDir.resolve(PREFERENCES_FILE_NAME).absolutePath) }
        // Development: `adb reverse tcp:8080 tcp:8080` forwards the device's localhost:8080 to this computer.
        // (10.0.2.2 would need Android 17's local-network permission; loopback doesn't.)
        single { ApiConfig(baseUrl = "http://localhost:8080") }
    }
