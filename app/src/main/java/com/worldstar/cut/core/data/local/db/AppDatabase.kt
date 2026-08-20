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
    version = 9,
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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE clips ADD COLUMN transitionType TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE clips ADD COLUMN transitionDurationMs INTEGER NOT NULL DEFAULT 500")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE clips ADD COLUMN cropX REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE clips ADD COLUMN cropY REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE clips ADD COLUMN cropW REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE clips ADD COLUMN cropH REAL NOT NULL DEFAULT 1.0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE clips ADD COLUMN textPosX REAL NOT NULL DEFAULT 0.5")
                db.execSQL("ALTER TABLE clips ADD COLUMN textPosY REAL NOT NULL DEFAULT 0.5")
                db.execSQL("ALTER TABLE clips ADD COLUMN textSizeSp REAL NOT NULL DEFAULT 24.0")
                db.execSQL("ALTER TABLE clips ADD COLUMN textRotation REAL NOT NULL DEFAULT 0.0")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE clips ADD COLUMN fontFamily TEXT NOT NULL DEFAULT 'default'")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE clips ADD COLUMN textOverlays TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE clips ADD COLUMN motionTrack TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE clips ADD COLUMN imageOverlays TEXT DEFAULT NULL")
            }
        }
    }
}
