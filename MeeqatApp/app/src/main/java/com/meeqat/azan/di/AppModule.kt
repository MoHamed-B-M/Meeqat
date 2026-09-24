package com.meeqat.azan.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.meeqat.azan.data.local.AppDatabase
import com.meeqat.azan.data.local.DailyPrayerDao
import com.meeqat.azan.data.local.ManualOffsetDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "meeqat_prefs")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "meeqat.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDailyPrayerDao(db: AppDatabase): DailyPrayerDao = db.dailyPrayerDao()

    @Provides
    fun provideManualOffsetDao(db: AppDatabase): ManualOffsetDao = db.manualOffsetDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.dataStore
}
