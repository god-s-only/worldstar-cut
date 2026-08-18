package com.worldstar.cut.features.video_editor.di

import com.worldstar.cut.features.video_editor.data.repository.ProjectRepositoryImpl
import com.worldstar.cut.features.video_editor.domain.repository.ProjectRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VideoEditorModule {

    @Binds
    @Singleton
    abstract fun bindProjectRepository(
        impl: ProjectRepositoryImpl
    ): ProjectRepository
}
