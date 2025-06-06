package com.rainbowt0506.appauth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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

class MainActivity : ComponentActivity() {

    private lateinit var authService: AuthorizationService
    private var authState: AuthState? = null

    private val clientId =
        ""
    private val redirectUri =
        Uri.parse("")
    private val authEndpoint = Uri.parse("")
    private val tokenEndpoint = Uri.parse("")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authService = AuthorizationService(this)

        setContent {
            var accessToken by remember { mutableStateOf<String?>(null) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Button(onClick = { startAuthFlow() }) {
                    Text("Login with OAuth")
                }

                Spacer(modifier = Modifier.height(16.dp))

                accessToken?.let {
                    Text("Access Token:\n$it")
                }
            }
        }
    }

    private fun startAuthFlow() {
        val serviceConfig = AuthorizationServiceConfiguration(authEndpoint, tokenEndpoint)

        val authRequest = AuthorizationRequest.Builder(
            serviceConfig,
            clientId,
            ResponseTypeValues.CODE,
            redirectUri
        ).setScopes("openid", "email", "profile", "https://www.googleapis.com/auth/youtube")

            .build()

        val intent = authService.getAuthorizationRequestIntent(authRequest)
        startActivityForResult(intent, 1234)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1234) {
            val resp = AuthorizationResponse.fromIntent(data!!)
            val ex = AuthorizationException.fromIntent(data)

            if (resp != null) {
                val tokenRequest = resp.createTokenExchangeRequest()
                authService.performTokenRequest(tokenRequest) { response, exception ->
                    if (response != null) {
                        authState = AuthState(resp, exception)
                        authState?.update(response, exception)

                        val token = response.accessToken
                        runOnUiThread {
                            setContent {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text("Access Token:\n$token")
                                }
                            }
                        }
                    } else {
                        Log.e("Auth", "Token exchange failed", exception)
                    }
                }
            } else {
                Log.e("Auth", "Authorization failed", ex)
            }
        }
    }
}


