package com.rainbowt0506.appauth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.rainbowt0506.appauth.model.Resource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues


class MainActivity : ComponentActivity() {

    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[AuthViewModel::class.java]

        val authLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                viewModel.handleAuthIntent(result.data)
            }

        setContent {
            val tokenState by viewModel.tokenState.collectAsStateWithLifecycle()
            val expiresAt by viewModel.expiresIn.collectAsStateWithLifecycle()


            LaunchedEffect(tokenState) {
                when(tokenState){
                    is Resource.Loading ->{

                    }
                    is Resource.Success -> {
                        Log.d("MainActivity", "Access Token: ${(tokenState as Resource.Success).data}")
                    }
                    is Resource.Error -> {
                        Log.e("MainActivity", "Error: ${(tokenState as Resource.Error).message}")
                        Toast.makeText(this@MainActivity, "Error: ${(tokenState as Resource.Error).message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            var remainingSeconds by remember { mutableStateOf<Long?>(null) }
            LaunchedEffect(expiresAt) {
                remainingSeconds = expiresAt?.let { (it - System.currentTimeMillis()) / 1000 }
                while (remainingSeconds != null && remainingSeconds!! > 0) {
                    delay(1_000)
                    remainingSeconds = remainingSeconds?.minus(1)
                }
            }

            MainContent(
                accessToken = tokenState.data,
                expiresIn = remainingSeconds,
                onLoginClick  = { authLauncher.launch(viewModel.createAuthIntent()) },
                onRefreshClick = { viewModel.refreshAccessToken() },
                onCheckExpiryClick = {
                    val msg = if (remainingSeconds != null && remainingSeconds!! > 0)
                        "Access Token 尚未過期" else "Access Token 已過期"
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                }
            )
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



