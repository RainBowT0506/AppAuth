package com.rainbowt0506.appauth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun GoogleLoginScreen(
    authService: AuthorizationService,
    authStateManager: AuthStateManager,
    intent: Intent
) {
    val context = LocalContext.current
    var accessToken by remember { mutableStateOf<String?>(null) }
    var userInfo by remember { mutableStateOf<Pair<String, String>?>(null) }

    LaunchedEffect(intent) {
        if (intent.action == Intent.ACTION_VIEW) {
            try {
                val response = AuthorizationResponse.fromIntent(intent)
                val ex = AuthorizationException.fromIntent(intent)

                if (response != null) {
                    val tokenRequest = response.createTokenExchangeRequest()
                    authService.performTokenRequest(tokenRequest) { tokenResponse, tokenEx ->
                        try {
                            if (tokenResponse != null) {
                                val authState = AuthState(response, tokenResponse, tokenEx)
                                authStateManager.replace(authState)
                                accessToken = tokenResponse.accessToken
                                fetchUserInfo(tokenResponse.accessToken!!) {
                                    userInfo = it
                                }
                            } else {
                                Log.e("OAuth", "Token exchange failed", tokenEx)
                            }
                        } catch (e: Exception) {
                            Log.e("OAuth", "Exception during token request", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("OAuth", "Exception in LaunchedEffect", e)
            }
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = {
            startAuthFlow(context)
        }) {
            Text("Login with Google")
        }

        Spacer(modifier = Modifier.height(20.dp))

        userInfo?.let {
            Text("Name: ${it.first}")
            Text("Email: ${it.second}")
        }
        Log.d("OAuth", "Access Token: $accessToken")
    }
}

fun startAuthFlow(context: Context) {
    val discoveryUrl = Uri.parse("https://accounts.google.com/.well-known/openid-configuration")
    val clientId = "526518801458-lc9bn6dt4u3i1rjjfddu9qe77ka9b0ps.apps.googleusercontent.com"
    val redirectUri =
        Uri.parse("com.googleusercontent.apps.526518801458-lc9bn6dt4u3i1rjjfddu9qe77ka9b0ps:/oauthredirect")

    AuthorizationServiceConfiguration.fetchFromUrl(discoveryUrl) { config, ex ->
        if (ex != null || config == null) return@fetchFromUrl

        val request = AuthorizationRequest.Builder(
            config,
            clientId,
            ResponseTypeValues.CODE,
            redirectUri
        )
            .setScopes("openid", "email", "profile", "https://www.googleapis.com/auth/youtube")
            .build()

        val authService = AuthorizationService(context)
        val intent = authService.getAuthorizationRequestIntent(request)
        context.startActivity(intent)
    }
}


fun fetchUserInfo(accessToken: String, onResult: (Pair<String, String>?) -> Unit) {
    Thread {
        try {
            val url = URL("https://www.googleapis.com/oauth2/v3/userinfo")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("Authorization", "Bearer $accessToken")
            val result = conn.inputStream.bufferedReader().use { it.readText() }

            val json = JSONObject(result)
            val name = json.getString("name")
            val email = json.getString("email")

            onResult(name to email)
        } catch (e: Exception) {
            Log.e("OAuth", "Error fetching user info", e)
            onResult(null)
        }
    }.start()
}
