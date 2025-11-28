package com.example.servyapp.data.local

import androidx.room.TypeConverter
import com.example.servyapp.domain.model.NutritionInfo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    // --- Convertidor para NutritionInfo ---
    @TypeConverter
    fun fromNutritionInfo(nutrition: NutritionInfo?): String? {
        return if (nutrition == null) null else gson.toJson(nutrition)
    }

    @TypeConverter
    fun toNutritionInfo(json: String?): NutritionInfo? {
        return if (json == null) null else gson.fromJson(json, NutritionInfo::class.java)
    }

    // --- Convertidor para List<String> (por si acaso lo necesitas para otros campos) ---
    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        return if (list == null) null else gson.toJson(list)
    }

    @TypeConverter
    fun toStringList(json: String?): List<String>? {
        if (json == null) return null
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type)
    }
}