package com.sirktv.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    @PlayerSettingsDataStore
    fun providePlayerSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("player_settings") }

    @Provides
    @Singleton
    @StartupPreferenceDataStore
    fun provideStartupPreferenceDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("startup_preference") }

    @Provides
    @Singleton
    @ParentalSettingsDataStore
    fun provideParentalSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("parental_settings") }

    @Provides
    @Singleton
    @AppearanceSettingsDataStore
    fun provideAppearanceSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("appearance_settings") }

    @Provides
    @Singleton
    @SubtitleAppearanceDataStore
    fun provideSubtitleAppearanceDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("subtitle_appearance") }

    @Provides
    @Singleton
    @DisplayNameDataStore
    fun provideDisplayNameDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("display_name") }
}
