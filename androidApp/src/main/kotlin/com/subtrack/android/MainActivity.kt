package com.subtrack.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.subtrack.shared.SubscriptionRepository
import com.subtrack.shared.WalletState
import com.subtrack.shared.observeWallet
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
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

            MaterialTheme {
                SubTrackRoot(repository = repository, today = today)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubTrackRoot(
    repository: SubscriptionRepository,
    today: LocalDate,
) {
    var addingSubscription by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val state by remember(today) { repository.observeWallet(today) }
        .collectAsState(initial = WalletState())

    if (addingSubscription) {
        AddSubscriptionScreen(
            today = today,
            onCancel = { addingSubscription = false },
            onSave = { draft ->
                scope.launch { repository.add(draft) }
                addingSubscription = false
            },
        )
        return
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("SubTrack") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { addingSubscription = true }) {
                Text("+")
            }
        },
    ) { padding ->
        WalletScreen(
            state = state,
            today = today,
            modifier = Modifier.fillMaxSize().padding(padding),
        )
    }
}
