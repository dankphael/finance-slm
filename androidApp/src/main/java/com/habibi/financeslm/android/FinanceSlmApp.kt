package com.habibi.financeslm.android

import android.app.Application
import com.habibi.financeslm.di.appModule
import com.habibi.financeslm.di.sdkModule
import com.habibi.financeslm.platform.PlatformContext
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class FinanceSlmApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize PlatformContext
        PlatformContext.init(this)

        // Initialize Koin DI
        startKoin {
            androidContext(this@FinanceSlmApp)
            modules(sdkModule, appModule)
        }
    }
}