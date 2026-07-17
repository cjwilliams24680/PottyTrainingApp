package com.cjwilliams.pottytraining.di

import com.cjwilliams.pottytraining.data.PottyRepositoryImpl
import com.cjwilliams.pottytraining.data.auth.AuthRepositoryImpl
import com.cjwilliams.pottytraining.data.auth.TokenManager
import com.cjwilliams.pottytraining.data.auth.TokenRefresher
import com.cjwilliams.pottytraining.domain.AuthRepository
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

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindTokenRefresher(
        tokenManager: TokenManager
    ): TokenRefresher
}
