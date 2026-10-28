package ro.bogdandragomir.paperboy

import android.app.Application
import org.koin.android.ext.koin.androidContext

class PaperboyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin { androidContext(this@PaperboyApplication) }
    }
}
