package com.rainbowt0506.appauth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues


class MainActivity : ComponentActivity() {

    private lateinit var authService: AuthorizationService
    private lateinit var authStateManager: AuthStateManager

    private val clientId = ""
    private val redirectUri = Uri.parse("")
    private val authEndpoint = Uri.parse("")
    private val tokenEndpoint = Uri.parse("")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authService = AuthorizationService(this)
        authStateManager = AuthStateManager.getInstance(this)

        setContent {
            var accessToken by remember { mutableStateOf<String?>(null) }
            var expiresIn by remember { mutableStateOf<Long?>(null) }

            // 初始化 token 與過期時間
            LaunchedEffect(Unit) {
                val state = authStateManager.current
                if (state.isAuthorized) {
                    accessToken = state.accessToken
                    val expiresAt = state.accessTokenExpirationTime
                    expiresAt?.let {
                        expiresIn = (it - System.currentTimeMillis()) / 1000
                    }
                    Log.d("Auth", "Loaded saved token: $accessToken")
                }
            }

            // 每秒倒數更新剩餘秒數
            LaunchedEffect(expiresIn) {
                while (expiresIn != null && expiresIn!! > 0) {
                    kotlinx.coroutines.delay(1000)
                    expiresIn = expiresIn?.minus(1)
                }
            }

            MainContent(
                accessToken = accessToken,
                expiresIn = expiresIn,
                onLoginClick = { startAuthFlow() },
                onRefreshClick = { refreshAccessToken() },
                onCheckExpiryClick = {
                    val isExpired = authStateManager.current.needsTokenRefresh
                    val msg = if (isExpired) "Access Token 已過期" else "Access Token 尚未過期"
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                }
            )

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
                        val token = response.accessToken

                        // 儲存 AuthState 到 DataStore
                        lifecycleScope.launch {
                            authStateManager.updateAfterAuthorization(resp, ex)
                            authStateManager.updateAfterTokenResponse(response, exception)

                            val savedToken = authStateManager.current.accessToken

                            // 顯示在畫面上
                            runOnUiThread {
                                setContent {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text("Access Token:\n$savedToken")
                                    }
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

    private fun refreshAccessToken() {
        val state = authStateManager.current

        if (!state.isAuthorized) {
            Log.w("Auth", "User is not authorized")
            return
        }

        // 使用 AppAuth 自帶的刷新機制
        state.performActionWithFreshTokens(authService) { accessToken, idToken, ex ->
            if (ex != null) {
                Log.e("Auth", "Token refresh failed", ex)
                return@performActionWithFreshTokens
            }

            if (accessToken != null) {
                Log.d("Auth", "Token refreshed: $accessToken")

                // 同步儲存最新的 AuthState
                lifecycleScope.launch {
                    authStateManager.replace(state)
                }
            }
        }
    }

}

@Composable
fun MainContent(
    accessToken: String?,
    expiresIn: Long?,
    onLoginClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onCheckExpiryClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = onLoginClick) {
            Text("Login with OAuth")
        }

        Spacer(modifier = Modifier.height(16.dp))

        accessToken?.let {
            Text("Access Token:\n$it")
        }

        Spacer(modifier = Modifier.height(16.dp))

        expiresIn?.let {
            Text("Token expires in: ${it}s")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onCheckExpiryClick) {
            Text("Check Token Expiry")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onRefreshClick) {
            Text("Refresh Access Token")
        }
    }
}



