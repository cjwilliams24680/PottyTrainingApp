package com.cjwilliams.pottytraining.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.network.http.LoggingInterceptor
import com.cjwilliams.pottytraining.BuildConfig
import com.cjwilliams.pottytraining.data.auth.AuthInterceptor
import com.cjwilliams.pottytraining.data.auth.AuthRetryInterceptor
import com.cjwilliams.pottytraining.data.auth.TokenRefresher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * 10.0.2.2 is the host machine as seen from the Android emulator; the Gradle-side
     * introspection in app/build.gradle.kts talks to the same server on localhost.
     */
    private const val SERVER_URL = "http://10.0.2.2:3000/graphql"

    @Provides
    @Singleton
    fun provideTokenDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("auth_tokens")
        }

    @Provides
    @Singleton
    @UnauthenticatedApollo
    fun provideUnauthenticatedApolloClient(): ApolloClient =
        ApolloClient.Builder()
            .serverUrl(SERVER_URL)
            .withDebugLogging()
            .build()

    @Provides
    @Singleton
    fun provideApolloClient(
        authInterceptor: AuthInterceptor,
        tokenRefresher: TokenRefresher
    ): ApolloClient =
        ApolloClient.Builder()
            .serverUrl(SERVER_URL)
            .addHttpInterceptor(authInterceptor)
            .addInterceptor(AuthRetryInterceptor(tokenRefresher))
            .withDebugLogging()
            .build()

    private fun ApolloClient.Builder.withDebugLogging(): ApolloClient.Builder = apply {
        if (BuildConfig.DEBUG) {
            addHttpInterceptor(LoggingInterceptor(LoggingInterceptor.Level.BODY))
        }
    }
}
