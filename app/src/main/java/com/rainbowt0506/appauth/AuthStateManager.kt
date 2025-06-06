package com.rainbowt0506.appauth

import android.content.Context
import net.openid.appauth.AuthState


class AuthStateManager(context: Context) {
    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)

    fun replace(state: AuthState) {
        prefs.edit().putString("state", state.jsonSerializeString()).apply()
    }

    fun get(): AuthState? {
        return prefs.getString("state", null)?.let {
            try { AuthState.jsonDeserialize(it) } catch (e: Exception) { null }
        }
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
