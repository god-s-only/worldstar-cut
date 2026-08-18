package com.worldstar.cut.features.media_picker.di

import android.content.ContentResolver
import android.content.Context
import com.worldstar.cut.core.di.IoDispatcher
import com.worldstar.cut.features.media_picker.data.repository.MediaPickerRepositoryImpl
import com.worldstar.cut.features.media_picker.data.source.MediaPickerLocalDataSource
import com.worldstar.cut.features.media_picker.domain.repository.MediaPickerRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MediaPickerModule {

    /**
     * Binds the concrete [MediaPickerRepositoryImpl] to its domain interface.
     * Any class that injects [MediaPickerRepository] gets this impl.
     */
    @Binds
    @Singleton
    abstract fun bindMediaPickerRepository(
        impl: MediaPickerRepositoryImpl
    ): MediaPickerRepository

    companion object {

        @Provides
        @Singleton
        fun provideContentResolver(
            @ApplicationContext context: Context
        ): ContentResolver = context.contentResolver

        @Provides
        @Singleton
        fun provideMediaPickerLocalDataSource(
            contentResolver: ContentResolver,
            @IoDispatcher ioDispatcher: CoroutineDispatcher
        ): MediaPickerLocalDataSource =
            MediaPickerLocalDataSource(contentResolver, ioDispatcher)
    }
}
