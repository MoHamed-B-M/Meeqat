package com.vibeprayer.app

import android.app.Application
import com.vibeprayer.app.data.datastore.UserPreferencesRepository
import com.vibeprayer.app.data.location.LocationTracker
import com.vibeprayer.app.data.remote.AlAdhanApi

class VibeApp : Application() {
    val api by lazy { AlAdhanApi() }
    val prefs by lazy { UserPreferencesRepository(this) }
    val locationTracker by lazy { LocationTracker(this) }
}
