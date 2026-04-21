package com.cjwilliams.pottytraining.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [PottyEntity::class], version = 1, exportSchema = false)
abstract class PottyDatabase : RoomDatabase() {
    abstract fun pottyDao(): PottyDao
}
