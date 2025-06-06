package com.rainbowt0506.appauth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.rainbowt0506.appauth.ui.theme.AppAuthTheme
import net.openid.appauth.AuthorizationService

class MainActivity : ComponentActivity() {

    private lateinit var latestIntent: Intent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val authStateManager = AuthStateManager(this)
        val authService = AuthorizationService(this)

        // 初始 intent 設定
        latestIntent = intent

        setContent {
            GoogleLoginScreen(
                authService = authService,
                authStateManager = authStateManager,
                intent = latestIntent
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("OAuth", "onNewIntent called: $intent")

        // 儲存新的 intent 給 Composable 使用
        latestIntent = intent

        val authStateManager = AuthStateManager(this)
        val authService = AuthorizationService(this)

        setContent {
            GoogleLoginScreen(
                authService = authService,
                authStateManager = authStateManager,
                intent = latestIntent
            )
        }
    }
}


