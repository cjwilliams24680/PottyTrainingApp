package com.cjwilliams.pottytraining.di

import android.content.Context
import androidx.room.Room
import com.cjwilliams.pottytraining.data.PottyDao
import com.cjwilliams.pottytraining.data.PottyDatabase
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
    fun provideDatabase(@ApplicationContext context: Context): PottyDatabase {
        return Room.databaseBuilder(
            context,
            PottyDatabase::class.java,
            "potty_database"
        )
            // Logs are a local cache of server state, so a schema change can drop
            // and refetch rather than migrate.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    fun providePottyDao(database: PottyDatabase): PottyDao {
        return database.pottyDao()
    }
}
