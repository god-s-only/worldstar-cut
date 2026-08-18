package com.worldstar.cut.core.data.local.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Room TypeConverters for types that Room cannot persist natively.
 * Registered at the database level so they apply to all DAOs.
 */
class RoomTypeConverters {

    private val gson = Gson()

    // ─── List<String> ───────────────────────────────────────────────────────

    @TypeConverter
    fun fromStringList(list: List<String>?): String? =
        list?.let { gson.toJson(it) }

    @TypeConverter
    fun toStringList(json: String?): List<String>? =
        json?.let {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(it, type)
        }

    // ─── List<Long> ─────────────────────────────────────────────────────────

    @TypeConverter
    fun fromLongList(list: List<Long>?): String? =
        list?.let { gson.toJson(it) }

    @TypeConverter
    fun toLongList(json: String?): List<Long>? =
        json?.let {
            val type = object : TypeToken<List<Long>>() {}.type
            gson.fromJson(it, type)
        }

    // ─── Map<String, String> ────────────────────────────────────────────────

    @TypeConverter
    fun fromStringMap(map: Map<String, String>?): String? =
        map?.let { gson.toJson(it) }

    @TypeConverter
    fun toStringMap(json: String?): Map<String, String>? =
        json?.let {
            val type = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson(it, type)
        }
}
