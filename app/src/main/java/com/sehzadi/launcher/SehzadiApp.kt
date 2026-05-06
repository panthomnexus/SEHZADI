package com.sehzadi.launcher

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SehzadiApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
