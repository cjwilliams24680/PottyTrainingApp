package com.cjwilliams.pottytraining.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class Tokens(
    val accessToken: String,
    val refreshToken: String
)

@Singleton
class TokenStorage @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val tokens: Flow<Tokens?> = dataStore.data.map { prefs ->
        val access = prefs[ACCESS_TOKEN]
        val refresh = prefs[REFRESH_TOKEN]
        if (access != null && refresh != null) Tokens(access, refresh) else null
    }

    suspend fun current(): Tokens? = tokens.first()

    suspend fun update(tokens: Tokens) {
        dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN] = tokens.accessToken
            prefs[REFRESH_TOKEN] = tokens.refreshToken
        }
    }

    suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(ACCESS_TOKEN)
            prefs.remove(REFRESH_TOKEN)
        }
    }

    private companion object {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    }
}
