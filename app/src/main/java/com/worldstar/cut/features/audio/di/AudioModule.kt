package com.worldstar.cut.features.audio.di

import com.worldstar.cut.features.audio.data.repository.AudioRepositoryImpl
import com.worldstar.cut.features.audio.domain.repository.AudioRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AudioModule {

    @Binds
    @Singleton
    abstract fun bindAudioRepository(
        impl: AudioRepositoryImpl
    ): AudioRepository
}
