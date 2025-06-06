package com.rainbowt0506.appauth

import android.annotation.SuppressLint
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.TokenResponse

/**
 * An example persistence mechanism for an [AuthState] instance. This stores the instance in a
 * shared preferences file, and provides thread-safe access and mutation.
 */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "AUTH_STATE")

class AuthStateManager private constructor(private val context: Context) {
    private val key by lazy { stringPreferencesKey("STATE") }
    val current: AuthState
        get() {
            val serializedJson =
                runBlocking { context.dataStore.data.first()[key] } ?: return AuthState()
            return AuthState.jsonDeserialize(serializedJson)
        }

    suspend fun replace(state: AuthState) {
        context.dataStore.edit { pref -> pref[key] = state.jsonSerializeString() }
    }

    suspend fun updateAfterAuthorization(
        response: AuthorizationResponse?,
        ex: AuthorizationException?,
    ) {
        val current = current
        current.update(response, ex)
        replace(current)
    }

    suspend fun updateAfterTokenResponse(
        response: TokenResponse?,
        ex: AuthorizationException?,
    ) {
        val current = current
        current.update(response, ex)
        replace(current)
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var INSTANCE: AuthStateManager? = null

        @Synchronized
        fun getInstance(context: Context): AuthStateManager {
            if (INSTANCE == null) {
                INSTANCE = AuthStateManager(context.applicationContext)
            }
            return INSTANCE as AuthStateManager
        }
    }
}
