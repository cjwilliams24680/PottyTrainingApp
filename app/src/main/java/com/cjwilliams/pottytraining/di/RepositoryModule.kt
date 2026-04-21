package com.cjwilliams.pottytraining.di

import com.cjwilliams.pottytraining.data.PottyRepositoryImpl
import com.cjwilliams.pottytraining.domain.PottyRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPottyRepository(
        pottyRepositoryImpl: PottyRepositoryImpl
    ): PottyRepository
}
