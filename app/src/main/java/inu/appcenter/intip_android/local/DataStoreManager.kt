package inu.appcenter.intip_android.local

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreManager(private val context: Context) {

    companion object {
        private val Context.dataStore by preferencesDataStore("user_prefs")
        val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token_key")
        val ACCESS_TOKEN_EXPIRED_TIME_KEY = stringPreferencesKey("access_token_expired_time_key")
        val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token_key")
        val REFRESH_TOKEN_EXPIRED_TIME_KEY = stringPreferencesKey("refresh_token_expired_time_key")
    }

    val accessToken: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[ACCESS_TOKEN_KEY]
        }

    val accessTokenExpiredTime: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[ACCESS_TOKEN_EXPIRED_TIME_KEY]
        }

    val refreshToken: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[REFRESH_TOKEN_KEY]
        }

    val refreshTokenExpiredTime: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[REFRESH_TOKEN_EXPIRED_TIME_KEY]
        }

    suspend fun saveAccessToken(token: String, expiredTime: String) {
        context.dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = token
            preferences[ACCESS_TOKEN_EXPIRED_TIME_KEY] = expiredTime
        }
        Log.d("DataStoreManager", "Access token saved: $token, expires at: $expiredTime")
    }

    suspend fun saveRefreshToken(token: String, expiredTime: String) {
        context.dataStore.edit { preferences ->
            preferences[REFRESH_TOKEN_KEY] = token
            preferences[REFRESH_TOKEN_EXPIRED_TIME_KEY] = expiredTime
        }
        Log.d("DataStoreManager", "Refresh token saved: $token, expires at: $expiredTime")
    }

    suspend fun clearTokens() {
        context.dataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN_KEY)
            preferences.remove(ACCESS_TOKEN_EXPIRED_TIME_KEY)
            preferences.remove(REFRESH_TOKEN_KEY)
            preferences.remove(REFRESH_TOKEN_EXPIRED_TIME_KEY)
        }
    }
}
