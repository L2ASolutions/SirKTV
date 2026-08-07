package com.sirktv.app.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PlayerSettingsDataStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class StartupPreferenceDataStore

/** Process-lifetime coroutine scope for best-effort background work (cache refreshes, etc). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
