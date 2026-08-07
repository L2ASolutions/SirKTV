package com.sirktv.app.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PlayerSettingsDataStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class StartupPreferenceDataStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ParentalSettingsDataStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppearanceSettingsDataStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SubtitleAppearanceDataStore

/** Process-lifetime coroutine scope for best-effort background work (cache refreshes, etc). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
