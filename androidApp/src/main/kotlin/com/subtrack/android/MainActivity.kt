package com.subtrack.android

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.subtrack.shared.Subscription
import com.subtrack.shared.SubscriptionRepository
import com.subtrack.shared.WalletState
import com.subtrack.shared.observeWallet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class MainActivity : ComponentActivity() {
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = (application as SubTrackApp).repository

        // Android 13+ exige permissão em runtime pra notificar. Sem ela o app
        // ainda funciona como lista — por isso o pedido não bloqueia a tela, e
        // negar não impede o uso.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !canPostNotifications()) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

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

/**
 * Toda mutação passa por aqui pra que o widget nunca fique mostrando um total
 * que a lista já corrigiu.
 */
private fun CoroutineScope.mutateAndRefreshWidget(
    context: Context,
    block: suspend () -> Unit,
) {
    launch {
        block()
        SubTrackWidget.refresh(context)
    }
}

private sealed interface Screen {
    data object Wallet : Screen

    data object Adding : Screen

    data class Editing(val subscription: Subscription) : Screen
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubTrackRoot(
    repository: SubscriptionRepository,
    today: LocalDate,
) {
    var screen: Screen by remember { mutableStateOf(Screen.Wallet) }
    var showArchived by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val state by remember(today, showArchived) { repository.observeWallet(today, showArchived) }
        .collectAsState(initial = WalletState())

    when (val current = screen) {
        is Screen.Adding, is Screen.Editing -> {
            val editing = (current as? Screen.Editing)?.subscription
            SubscriptionFormScreen(
                today = today,
                editing = editing,
                onCancel = { screen = Screen.Wallet },
                onSave = { draft ->
                    scope.mutateAndRefreshWidget(context) {
                        if (editing == null) {
                            repository.add(draft)
                        } else {
                            // O histórico de aviso é preservado de propósito, e
                            // se acerta sozinho: como guardamos QUAL renovação
                            // foi avisada, mexer na data muda a próxima
                            // renovação e o aviso volta a valer; mexer só no
                            // valor mantém a data, e o usuário não é avisado
                            // duas vezes da mesma cobrança.
                            repository.update(
                                editing.copy(
                                    name = draft.name,
                                    amountCents = draft.amountCents,
                                    currency = draft.currency,
                                    cycle = draft.cycle,
                                    everyN = draft.everyN,
                                    anchor = draft.anchor,
                                    category = draft.category,
                                    notifyDaysBefore = draft.notifyDaysBefore,
                                ),
                            )
                        }
                    }
                    screen = Screen.Wallet
                },
                onArchiveToggle = { subscription ->
                    scope.mutateAndRefreshWidget(context) {
                        repository.setArchived(subscription.id, !subscription.archived)
                    }
                    screen = Screen.Wallet
                },
                onDelete = { subscription ->
                    scope.mutateAndRefreshWidget(context) { repository.delete(subscription.id) }
                    screen = Screen.Wallet
                },
            )
        }

        Screen.Wallet ->
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("SubTrack") },
                        actions = {
                            // Sem isto, arquivar seria porta de mão única: a
                            // arquivada some da lista e o botão de desarquivar
                            // fica inalcançável.
                            TextButton(onClick = { showArchived = !showArchived }) {
                                Text(if (showArchived) "Ocultar arquivadas" else "Ver arquivadas")
                            }
                        },
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(onClick = { screen = Screen.Adding }) {
                        Text("+")
                    }
                },
            ) { padding ->
                WalletScreen(
                    state = state,
                    today = today,
                    onSubscriptionClick = { screen = Screen.Editing(it) },
                    modifier = Modifier.fillMaxSize().padding(padding),
                )
            }
    }
}
