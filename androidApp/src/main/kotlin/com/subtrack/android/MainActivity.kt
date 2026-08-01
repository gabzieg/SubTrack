package com.subtrack.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.subtrack.shared.WalletState
import com.subtrack.shared.observeWallet
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = (application as SubTrackApp).repository

        setContent {
            // A data de hoje é lida uma vez por composição da tela. Se o app
            // ficar aberto atravessando a meia-noite, os rótulos só atualizam
            // na próxima abertura — aceitável agora, revisar se incomodar.
            val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
            val state by remember(today) { repository.observeWallet(today) }
                .collectAsState(initial = WalletState())

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WalletScreen(state = state, today = today)
                }
            }
        }
    }
}
