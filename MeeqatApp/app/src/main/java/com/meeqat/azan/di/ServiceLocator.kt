package com.meeqat.azan.di

import android.content.Context
import androidx.room.Room
import com.meeqat.azan.data.local.AppDatabase
import com.meeqat.azan.data.repo.CalculationRepository
import com.meeqat.azan.data.repo.LocationRepository
import com.meeqat.azan.data.repo.NominatimRepository
import com.meeqat.azan.data.repo.PrayerRepository
import com.meeqat.azan.data.repo.SettingsRepository
import com.meeqat.azan.data.repo.SoundRepository
import com.meeqat.azan.domain.icon.AppIconManager
import com.meeqat.azan.domain.scheduler.AthanScheduler

object ServiceLocator {
    @Volatile private var appContext: Context? = null

    fun init(context: Context) {
        if (appContext == null) appContext = context.applicationContext
    }

    private fun requireContext(): Context = appContext ?: throw IllegalStateException("ServiceLocator not initialized — call init() in Application.onCreate()")

    val database: AppDatabase by lazy {
        Room.databaseBuilder(requireContext(), AppDatabase::class.java, "meeqat.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(requireContext()) }
    val calculationRepository: CalculationRepository by lazy { CalculationRepository(database, settingsRepository) }
    val locationRepository: LocationRepository by lazy { LocationRepository(requireContext(), settingsRepository) }
    val soundRepository: SoundRepository by lazy { SoundRepository(settingsRepository) }
    val nominatimRepository: NominatimRepository by lazy { NominatimRepository(requireContext()) }
    val prayerRepository: PrayerRepository by lazy {
        PrayerRepository(requireContext(), database, settingsRepository, calculationRepository)
    }
    val athanScheduler: AthanScheduler by lazy { AthanScheduler(requireContext(), settingsRepository) }
    val appIconManager: AppIconManager by lazy { AppIconManager(requireContext()) }
}
