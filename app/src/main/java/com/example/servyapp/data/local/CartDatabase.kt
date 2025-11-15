package com.example.servyapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CartItemEntity::class],
    version = 1,
    exportSchema = false // Por simplicidad, se recomienda 'true' en producción
)
abstract class CartDatabase : RoomDatabase() {
    abstract fun cartDao(): CartDao
}