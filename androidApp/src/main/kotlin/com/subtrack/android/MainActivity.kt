package com.subtrack.android

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.subtrack.shared.SortOrder
import com.subtrack.shared.Subscription
import com.subtrack.shared.SubscriptionDraft
import com.subtrack.shared.SubscriptionRepository
import com.subtrack.shared.WalletState
import com.subtrack.shared.csvFileName
import com.subtrack.shared.observeWallet
import com.subtrack.shared.toCsv
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import java.io.BufferedReader
import java.io.InputStreamReader
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

        val smartName = intent.getStringExtra("smart_name")
        val smartAmount = intent.getLongExtra("smart_amount", -1L)

        setContent {
            // A data de hoje é lida uma vez por composição da tela. Se o app
            // ficar aberto atravessando a meia-noite, os rótulos só atualizam
            // na próxima abertura — aceitável agora, revisar se incomodar.
            val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }

            MaterialTheme {
                SubTrackRoot(
                    repository = repository,
                    premiumRepository = (application as SubTrackApp).premiumRepository,
                    today = today,
                    initialSmartName = smartName,
                    initialSmartAmount = smartAmount,
                )
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

/**
 * Grava o CSV no arquivo que o usuário escolheu.
 *
 * Via Storage Access Framework: o usuário aponta onde salvar e o app recebe
 * acesso só àquele arquivo. Não exige permissão de armazenamento, e o dado não
 * passa por lugar nenhum além do destino escolhido.
 */
private suspend fun writeCsv(
    context: Context,
    repository: SubscriptionRepository,
    today: LocalDate,
    destination: Uri,
): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        // observeAll: o export inclui arquivadas — backup que descarta
        // histórico não é backup.
        val csv = repository.observeAll(today).first().toCsv()
        context.contentResolver.openOutputStream(destination)?.use { output ->
            output.write(csv.toByteArray(Charsets.UTF_8))
        } ?: error("Não foi possível abrir $destination para escrita")
    }.isSuccess
}

private sealed interface Screen {
    data object Wallet : Screen

    data class Adding(val draft: SubscriptionDraft? = null) : Screen

    data class Editing(val subscription: Subscription) : Screen

    data class Importing(val rows: List<List<String>>) : Screen

    data object Paywall : Screen
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubTrackRoot(
    repository: SubscriptionRepository,
    premiumRepository: com.subtrack.shared.PremiumRepository,
    today: LocalDate,
    initialSmartName: String?,
    initialSmartAmount: Long,
) {
    // Se o app for aberto pela notificação, inicializa direto na tela Adding
    // com um draft preenchido.
    val initialScreen = if (initialSmartName != null && initialSmartAmount > 0) {
        val draft = com.subtrack.shared.SubscriptionDraft(
            name = initialSmartName,
            amountCents = initialSmartAmount,
            currency = "BRL",
            cycle = com.subtrack.shared.BillingCycle.MONTHLY,
            everyN = 1,
            anchor = today,
            category = null,
            notifyDaysBefore = 3,
        )
        Screen.Adding(draft)
    } else {
        Screen.Wallet
    }

    var screen: Screen by remember { mutableStateOf(initialScreen) }
    var showArchived by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf(SortOrder.RENEWAL) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val isPremium by premiumRepository.observeIsPremium().collectAsState(initial = false)

    val exportCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { destination ->
        // destination é null quando o usuário cancela o seletor.
        if (destination == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = writeCsv(context, repository, today, destination)
            snackbarHostState.showSnackbar(
                if (ok) "Assinaturas exportadas" else "Não foi possível exportar",
            )
        }
    }

    val importCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { source ->
        if (source == null) return@rememberLauncherForActivityResult
        scope.launch {
            val content = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(source)?.use { input ->
                        BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
                    }
                }.getOrNull()
            }
            if (content == null) {
                snackbarHostState.showSnackbar("Não foi possível ler o arquivo")
                return@launch
            }
            val rows = com.subtrack.shared.parseCsvRows(content)
            val header = rows.firstOrNull().orEmpty()
            if (com.subtrack.shared.detectSubTrackFormat(header)) {
                val result = com.subtrack.shared.importSubTrackCsv(rows)
                scope.mutateAndRefreshWidget(context) {
                    result.imported.forEach { repository.add(it) }
                }
                snackbarHostState.showSnackbar("Importadas: ${result.imported.size}, Ignoradas: ${result.skipped}")
            } else {
                screen = Screen.Importing(rows)
            }
        }
    }

    val state by remember(today, showArchived, sortOrder) { repository.observeWallet(today, showArchived, sortOrder) }
        .collectAsState(initial = WalletState())

    when (val current = screen) {
        is Screen.Adding, is Screen.Editing -> {
            val editing = (current as? Screen.Editing)?.subscription
            val addingDraft = (current as? Screen.Adding)?.draft

            // Para reaproveitar a SubscriptionFormScreen sem refatorar o componente,
            // podemos converter o draft numa Subscription falsa só pra preencher os campos.
            val initialData = editing ?: addingDraft?.let { draft ->
                Subscription(
                    id = 0,
                    name = draft.name,
                    amountCents = draft.amountCents,
                    currency = draft.currency,
                    cycle = draft.cycle,
                    everyN = draft.everyN,
                    anchor = draft.anchor,
                    category = draft.category,
                    notifyDaysBefore = draft.notifyDaysBefore,
                    archived = false,
                    lastNotifiedRenewal = null,
                )
            }

            SubscriptionFormScreen(
                today = today,
                editing = initialData,
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

        is Screen.Importing -> {
            ImportScreen(
                rows = current.rows,
                onImport = { mapping ->
                    val result = com.subtrack.shared.importGenericCsv(current.rows, mapping, today)
                    scope.mutateAndRefreshWidget(context) {
                        result.imported.forEach { repository.add(it) }
                    }
                    scope.launch {
                        snackbarHostState.showSnackbar("Importadas: ${result.imported.size}, Ignoradas: ${result.skipped}")
                    }
                    screen = Screen.Wallet
                },
                onCancel = { screen = Screen.Wallet },
            )
        }

        Screen.Paywall -> {
            PaywallScreen(
                onBuyClick = {
                    scope.launch {
                        premiumRepository.purchasePremium()
                        snackbarHostState.showSnackbar("Bem-vindo ao SubTrack Pro!")
                        screen = Screen.Wallet
                    }
                },
                onCancel = { screen = Screen.Wallet },
            )
        }

        Screen.Wallet ->
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("SubTrack") },
                        actions = {
                            TextButton(onClick = {
                                if (isPremium) {
                                    importCsv.launch(arrayOf("text/comma-separated-values", "text/csv"))
                                } else {
                                    screen = Screen.Paywall
                                }
                            }) {
                                Text("Importar")
                            }
                            TextButton(onClick = {
                                if (isPremium) {
                                    exportCsv.launch(csvFileName(today))
                                } else {
                                    screen = Screen.Paywall
                                }
                            }) {
                                Text("Exportar")
                            }
                            // Sem isto, arquivar seria porta de mão única: a
                            // arquivada some da lista e o botão de desarquivar
                            // fica inalcançável.
                            TextButton(onClick = { showArchived = !showArchived }) {
                                Text(if (showArchived) "Ocultar" else "Arquivadas")
                            }
                        },
                    )
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
                floatingActionButton = {
                    FloatingActionButton(onClick = {
                        val activeCount = state.groupedByCategory.sumOf { group -> group.items.count { !it.subscription.archived } }
                        if (!isPremium && activeCount >= 5) {
                            screen = Screen.Paywall
                        } else {
                            screen = Screen.Adding()
                        }
                    }) {
                        Text("+")
                    }
                },
            ) { padding ->
                WalletScreen(
                    state = state,
                    today = today,
                    onSubscriptionClick = { screen = Screen.Editing(it) },
                    onSortOrderChange = { sortOrder = it },
                    modifier = Modifier.fillMaxSize().padding(padding),
                )
            }
    }
}
