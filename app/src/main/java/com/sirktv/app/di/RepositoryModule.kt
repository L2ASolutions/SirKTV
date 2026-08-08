package com.sirktv.app.di

import com.sirktv.app.data.repository.AppearanceSettingsRepositoryImpl
import com.sirktv.app.data.repository.AuthRepositoryImpl
import com.sirktv.app.data.repository.ChannelRepositoryImpl
import com.sirktv.app.data.repository.DisplayNameRepositoryImpl
import com.sirktv.app.data.repository.FavoritesRepositoryImpl
import com.sirktv.app.data.repository.MovieRepositoryImpl
import com.sirktv.app.data.repository.ParentalSettingsRepositoryImpl
import com.sirktv.app.data.repository.PlayerSettingsRepositoryImpl
import com.sirktv.app.data.repository.SearchHistoryRepositoryImpl
import com.sirktv.app.data.repository.SeriesRepositoryImpl
import com.sirktv.app.data.repository.StartupPreferenceRepositoryImpl
import com.sirktv.app.data.repository.SubtitleAppearanceRepositoryImpl
import com.sirktv.app.data.repository.WatchProgressRepositoryImpl
import com.sirktv.app.domain.repository.AppearanceSettingsRepository
import com.sirktv.app.domain.repository.AuthRepository
import com.sirktv.app.domain.repository.ChannelRepository
import com.sirktv.app.domain.repository.DisplayNameRepository
import com.sirktv.app.domain.repository.FavoritesRepository
import com.sirktv.app.domain.repository.MovieRepository
import com.sirktv.app.domain.repository.ParentalSettingsRepository
import com.sirktv.app.domain.repository.PlayerSettingsRepository
import com.sirktv.app.domain.repository.SearchHistoryRepository
import com.sirktv.app.domain.repository.SeriesRepository
import com.sirktv.app.domain.repository.StartupPreferenceRepository
import com.sirktv.app.domain.repository.SubtitleAppearanceRepository
import com.sirktv.app.domain.repository.WatchProgressRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindChannelRepository(impl: ChannelRepositoryImpl): ChannelRepository

    @Binds
    @Singleton
    abstract fun bindPlayerSettingsRepository(impl: PlayerSettingsRepositoryImpl): PlayerSettingsRepository

    @Binds
    @Singleton
    abstract fun bindStartupPreferenceRepository(impl: StartupPreferenceRepositoryImpl): StartupPreferenceRepository

    @Binds
    @Singleton
    abstract fun bindMovieRepository(impl: MovieRepositoryImpl): MovieRepository

    @Binds
    @Singleton
    abstract fun bindSeriesRepository(impl: SeriesRepositoryImpl): SeriesRepository

    @Binds
    @Singleton
    abstract fun bindWatchProgressRepository(impl: WatchProgressRepositoryImpl): WatchProgressRepository

    @Binds
    @Singleton
    abstract fun bindFavoritesRepository(impl: FavoritesRepositoryImpl): FavoritesRepository

    @Binds
    @Singleton
    abstract fun bindSearchHistoryRepository(impl: SearchHistoryRepositoryImpl): SearchHistoryRepository

    @Binds
    @Singleton
    abstract fun bindParentalSettingsRepository(impl: ParentalSettingsRepositoryImpl): ParentalSettingsRepository

    @Binds
    @Singleton
    abstract fun bindAppearanceSettingsRepository(impl: AppearanceSettingsRepositoryImpl): AppearanceSettingsRepository

    @Binds
    @Singleton
    abstract fun bindSubtitleAppearanceRepository(impl: SubtitleAppearanceRepositoryImpl): SubtitleAppearanceRepository

    @Binds
    @Singleton
    abstract fun bindDisplayNameRepository(impl: DisplayNameRepositoryImpl): DisplayNameRepository
}
