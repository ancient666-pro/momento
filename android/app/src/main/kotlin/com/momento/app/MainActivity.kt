package com.momento.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.momento.core.designsystem.theme.MainBackground
import com.momento.core.designsystem.theme.MomentoTheme
import com.momento.core.designsystem.theme.PrimaryYellow
import com.momento.core.designsystem.theme.TextDark
import com.momento.domain.realtime.RealtimeSession
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var demoSession: DemoSessionBootstrap

    @Inject
    lateinit var realtimeSession: RealtimeSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MomentoTheme {
                var ready by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    withContext(Dispatchers.IO) { demoSession.ensureSession() }
                    ready = true
                    // Connect chat/calls after the session exists; failures (for
                    // example an unconfigured Nexconn app key) never block the UI.
                    val connected = realtimeSession.connect()
                    if (connected is com.momento.core.common.AppResult.Failure) {
                        android.util.Log.w("MomentoRealtime", connected.error.message)
                    }
                }
                if (ready) MainScreen() else SessionSplash()
            }
        }
    }
}

@Composable
private fun SessionSplash() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PrimaryYellow),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Momento",
            style = MaterialTheme.typography.displaySmall,
            color = TextDark
        )
        Text(
            text = "Point of view",
            style = MaterialTheme.typography.bodyMedium,
            color = TextDark.copy(alpha = 0.7f)
        )
    }
}
