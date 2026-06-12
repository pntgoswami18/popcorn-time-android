package com.popcorntime.android

import android.app.Application
import com.popcorntime.android.data.remote.RemoteControlServerController
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class PopcornApp : Application() {

    // Keeps the remote control server's running state in sync with the user's
    // persisted toggle for the whole process lifetime (server runs iff enabled).
    @Inject lateinit var remoteControlServerController: RemoteControlServerController

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        remoteControlServerController.start()
    }
}
