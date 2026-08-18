package com.worldstar.cut.core.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "worldstar_cut_prefs"
)

/**
 * Manages persistent user preferences using Jetpack DataStore.
 * All keys are typed; callers never touch raw preference keys directly.
 */
@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    // ─── Keys ────────────────────────────────────────────────────────────────

    private object Keys {
        val IS_PREMIUM        = booleanPreferencesKey("is_premium")
        val PREMIUM_PLAN      = stringPreferencesKey("premium_plan")       // "monthly" | "yearly" | "lifetime" | ""
        val PREMIUM_EXPIRY_MS = longPreferencesKey("premium_expiry_ms")
        val DEFAULT_EXPORT_RES= stringPreferencesKey("default_export_res") // "720p" | "1080p" | "4K"
        val DEFAULT_FRAME_RATE= intPreferencesKey("default_frame_rate")    // 24 | 30 | 60
        val SHOW_WATERMARK    = booleanPreferencesKey("show_watermark")
        val FIRST_LAUNCH_DONE = booleanPreferencesKey("first_launch_done")
        val THEME_MODE        = stringPreferencesKey("theme_mode")         // "system" | "light" | "dark"
        val HAPTIC_FEEDBACK   = booleanPreferencesKey("haptic_feedback")
        val LAST_PROJECT_ID   = longPreferencesKey("last_project_id")
    }

    // ─── Flows ───────────────────────────────────────────────────────────────

    val isPremium: Flow<Boolean> = dataStore.data
        .catchIo()
        .map { it[Keys.IS_PREMIUM] ?: false }

    val premiumPlan: Flow<String> = dataStore.data
        .catchIo()
        .map { it[Keys.PREMIUM_PLAN] ?: "" }

    val premiumExpiryMs: Flow<Long> = dataStore.data
        .catchIo()
        .map { it[Keys.PREMIUM_EXPIRY_MS] ?: 0L }

    val defaultExportResolution: Flow<String> = dataStore.data
        .catchIo()
        .map { it[Keys.DEFAULT_EXPORT_RES] ?: "1080p" }

    val defaultFrameRate: Flow<Int> = dataStore.data
        .catchIo()
        .map { it[Keys.DEFAULT_FRAME_RATE] ?: 30 }

    val showWatermark: Flow<Boolean> = dataStore.data
        .catchIo()
        .map { it[Keys.SHOW_WATERMARK] ?: true }

    val isFirstLaunchDone: Flow<Boolean> = dataStore.data
        .catchIo()
        .map { it[Keys.FIRST_LAUNCH_DONE] ?: false }

    val themeMode: Flow<String> = dataStore.data
        .catchIo()
        .map { it[Keys.THEME_MODE] ?: "system" }

    val hapticFeedbackEnabled: Flow<Boolean> = dataStore.data
        .catchIo()
        .map { it[Keys.HAPTIC_FEEDBACK] ?: true }

    val lastProjectId: Flow<Long> = dataStore.data
        .catchIo()
        .map { it[Keys.LAST_PROJECT_ID] ?: -1L }

    // ─── Writes ───────────────────────────────────────────────────────────────

    suspend fun setPremium(isPremium: Boolean, plan: String, expiryMs: Long) {
        dataStore.edit { prefs ->
            prefs[Keys.IS_PREMIUM]        = isPremium
            prefs[Keys.PREMIUM_PLAN]      = plan
            prefs[Keys.PREMIUM_EXPIRY_MS] = expiryMs
            prefs[Keys.SHOW_WATERMARK]    = !isPremium
        }
    }

    suspend fun setDefaultExportResolution(resolution: String) {
        dataStore.edit { it[Keys.DEFAULT_EXPORT_RES] = resolution }
    }

    suspend fun setDefaultFrameRate(fps: Int) {
        dataStore.edit { it[Keys.DEFAULT_FRAME_RATE] = fps }
    }

    suspend fun setFirstLaunchDone() {
        dataStore.edit { it[Keys.FIRST_LAUNCH_DONE] = true }
    }

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun setHapticFeedback(enabled: Boolean) {
        dataStore.edit { it[Keys.HAPTIC_FEEDBACK] = enabled }
    }

    suspend fun setLastProjectId(id: Long) {
        dataStore.edit { it[Keys.LAST_PROJECT_ID] = id }
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private fun Flow<Preferences>.catchIo() = catch { e ->
        if (e is IOException) {
            Timber.e(e, "DataStore read error — emitting defaults")
            emit(emptyPreferences())
        } else {
            throw e
        }
    }
}
