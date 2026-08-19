package com.worldstar.cut.core.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.worldstar.cut.features.video_editor.data.local.db.ProjectDao
import com.worldstar.cut.features.video_editor.data.local.db.ProjectEntity
import com.worldstar.cut.features.video_editor.data.local.db.TrackDao
import com.worldstar.cut.features.video_editor.data.local.db.TrackEntity
import com.worldstar.cut.features.video_editor.data.local.db.ClipDao
import com.worldstar.cut.features.video_editor.data.local.db.ClipEntity
import com.worldstar.cut.features.export.data.local.db.ExportHistoryDao
import com.worldstar.cut.features.export.data.local.db.ExportHistoryEntity

/**
 * Room database for Worldstar Cut.
 * Each feature registers its own entities and DAOs here.
 * Migrations are managed explicitly to preserve user projects.
 */
@Database(
    entities = [
        ProjectEntity::class,
        TrackEntity::class,
        ClipEntity::class,
        ExportHistoryEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(RoomTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun projectDao(): ProjectDao
    abstract fun trackDao(): TrackDao
    abstract fun clipDao(): ClipDao
    abstract fun exportHistoryDao(): ExportHistoryDao

    companion object {
        const val DATABASE_NAME = "worldstar_cut.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE clips ADD COLUMN mediaType TEXT NOT NULL DEFAULT 'video'")
            }
        }
    }
}
