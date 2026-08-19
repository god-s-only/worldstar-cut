package com.worldstar.cut.core.di

import android.content.Context
import androidx.room.Room
import com.worldstar.cut.core.data.local.db.AppDatabase
import com.worldstar.cut.features.export.data.local.db.ExportHistoryDao
import com.worldstar.cut.features.video_editor.data.local.db.ClipDao
import com.worldstar.cut.features.video_editor.data.local.db.ProjectDao
import com.worldstar.cut.features.video_editor.data.local.db.TrackDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()

    @Provides
    @Singleton
    fun provideProjectDao(db: AppDatabase): ProjectDao = db.projectDao()

    @Provides
    @Singleton
    fun provideTrackDao(db: AppDatabase): TrackDao = db.trackDao()

    @Provides
    @Singleton
    fun provideClipDao(db: AppDatabase): ClipDao = db.clipDao()

    @Provides
    @Singleton
    fun provideExportHistoryDao(db: AppDatabase): ExportHistoryDao = db.exportHistoryDao()
}
