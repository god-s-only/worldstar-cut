package com.worldstar.cut.features.trim_cut.di

import com.worldstar.cut.features.trim_cut.data.repository.TrimCutRepositoryImpl
import com.worldstar.cut.features.trim_cut.domain.repository.TrimCutRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TrimCutModule {

    @Binds
    @Singleton
    abstract fun bindTrimCutRepository(
        impl: TrimCutRepositoryImpl
    ): TrimCutRepository
}
