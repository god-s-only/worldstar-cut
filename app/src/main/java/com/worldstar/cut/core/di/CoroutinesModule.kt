package com.worldstar.cut.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

// ─── Qualifier annotations ────────────────────────────────────────────────────

/** CPU-bound work — sorting, mapping, JSON parsing */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

/** I/O bound work — disk, network, MediaStore queries */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/** Main/UI thread */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

/** Main/UI thread but immediate (won't reschedule if already on main) */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainImmediateDispatcher

// ─── Module ───────────────────────────────────────────────────────────────────

@Module
@InstallIn(SingletonComponent::class)
object CoroutinesModule {

    @DefaultDispatcher
    @Provides
    @Singleton
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @IoDispatcher
    @Provides
    @Singleton
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @MainDispatcher
    @Provides
    @Singleton
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    @MainImmediateDispatcher
    @Provides
    @Singleton
    fun provideMainImmediateDispatcher(): CoroutineDispatcher = Dispatchers.Main.immediate
}
