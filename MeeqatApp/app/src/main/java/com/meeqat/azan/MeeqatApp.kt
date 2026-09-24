package com.meeqat.azan

import android.app.Application

class MeeqatApp : Application() {
    // Manual ServiceLocator init — offline-first, no Hilt
    override fun onCreate() {
        super.onCreate()
        com.meeqat.azan.di.ServiceLocator.init(this)
    }
}
