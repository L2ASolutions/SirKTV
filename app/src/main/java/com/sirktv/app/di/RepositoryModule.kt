package com.sirktv.app.di

import com.sirktv.app.data.repository.AuthRepositoryImpl
import com.sirktv.app.data.repository.ChannelRepositoryImpl
import com.sirktv.app.data.repository.PlayerSettingsRepositoryImpl
import com.sirktv.app.data.repository.StartupPreferenceRepositoryImpl
import com.sirktv.app.domain.repository.AuthRepository
import com.sirktv.app.domain.repository.ChannelRepository
import com.sirktv.app.domain.repository.PlayerSettingsRepository
import com.sirktv.app.domain.repository.StartupPreferenceRepository
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
}
