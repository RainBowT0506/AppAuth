package com.rainbowt0506.appauth

import android.content.Context

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rainbowt0506.appauth.data.local.AuthStateManager
import com.rainbowt0506.appauth.model.Resource
import kotlinx.coroutines.launch
import net.openid.appauth.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val clientId = ""
    private val redirectUri = Uri.parse("")
    private val authEndpoint = Uri.parse("")
    private val tokenEndpoint = Uri.parse("")

    private val context = getApplication<Application>().applicationContext
    private val authService = AuthorizationService(context)
    private val authStateManager = AuthStateManager.getInstance(context)

    private val _tokenState = MutableStateFlow<Resource<String>>(Resource.Loading())
    val tokenState: StateFlow<Resource<String>> = _tokenState

    private val _expiresIn = MutableStateFlow<Long?>(null)
    val expiresIn: StateFlow<Long?> = _expiresIn


    init {
        viewModelScope.launch { loadStoredState() }
    }

    fun createAuthIntent(): Intent {
        val serviceConfig = AuthorizationServiceConfiguration(authEndpoint, tokenEndpoint)

        val authRequest = AuthorizationRequest
            .Builder(serviceConfig, clientId, ResponseTypeValues.CODE, redirectUri)
            .setScopes("openid", "email", "profile", "https://www.googleapis.com/auth/youtube")
            .build()

        val intent = authService.getAuthorizationRequestIntent(authRequest)
        return intent
    }

    fun handleAuthIntent(data: Intent?) {
        viewModelScope.launch(Dispatchers.IO) {
            val resp = AuthorizationResponse.fromIntent(data!!)
            val ex = AuthorizationException.fromIntent(data)

            if (resp != null) {
                val tokenReq = resp.createTokenExchangeRequest()
                authService.performTokenRequest(tokenReq) { response, exception ->
                    viewModelScope.launch(Dispatchers.IO) {
                        if (response != null) {
                            authStateManager.updateAfterAuthorization(resp, ex)
                            authStateManager.updateAfterTokenResponse(response, exception)

                            _tokenState.value = Resource.Success(response.accessToken)
                            _expiresIn.value = response.accessTokenExpirationTime
                        } else {
                            _tokenState.value =
                                Resource.Error(
                                    exception?.errorDescription ?: "Token exchange failed"
                                )
                        }
                    }
                }
            } else {
                _tokenState.value = Resource.Error(ex?.errorDescription ?: "Authorization failed")
            }
        }
    }

    fun refreshAccessToken() {
        viewModelScope.launch(Dispatchers.IO) {
            val state = authStateManager.current
            if (state.isAuthorized) {
                state.performActionWithFreshTokens(authService) { accessToken, idToken, ex ->
                    viewModelScope.launch(Dispatchers.IO) {
                        if (ex != null) {
                            _tokenState.value =
                                Resource.Error(ex.errorDescription ?: "Token refresh failed")
                        }

                        if (accessToken != null) {
                            authStateManager.replace(state)
                            _tokenState.value = Resource.Success(accessToken)
                            _expiresIn.value = state.accessTokenExpirationTime
                        } else {
                            _tokenState.value = Resource.Error("Access token is null")
                        }
                    }
                }
            } else {
                _tokenState.value = Resource.Error("User is not authorized")
            }
        }
    }

    private fun loadStoredState() {
        val state = authStateManager.current
        if (state.isAuthorized) {
            _tokenState.value = Resource.Success(state.accessToken)
            _expiresIn.value = state.accessTokenExpirationTime
        }
    }
}