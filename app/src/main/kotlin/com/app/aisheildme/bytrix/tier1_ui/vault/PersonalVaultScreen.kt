package com.app.aisheildme.bytrix.tier1_ui.vault

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.aisheildme.bytrix.core.designsystem.AppColors
import com.app.aisheildme.bytrix.core.designsystem.component.SimpleAppBar
import com.app.aisheildme.bytrix.tier1_ui.KeyValueRow
import com.app.aisheildme.bytrix.tier1_ui.SectionLabel
import com.app.aisheildme.bytrix.tier1_ui.ShieldCard
import com.app.aisheildme.bytrix.tier1_ui.StatusChip
import com.app.aisheildme.bytrix.tier2_vault.repository.AuthorizedDataAccess
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_TIME = DateTimeFormatter.ofPattern("EEE, MMM d · h:mm a", Locale.US)
private val DATE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)
private fun Long.dateTime() = DATE_TIME.format(Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()))
private fun Long.date() = DATE.format(Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()))

@Composable
fun PersonalVaultScreen(
    onBack: () -> Unit,
    onImportDocument: () -> Unit,
    viewModel: PersonalVaultViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it); viewModel.onMessageShown() } }

    Scaffold(
        topBar = {
            SimpleAppBar(title = "Personal Vault", onBack = onBack) {
                TextButton(onClick = onImportDocument) { Text("Import") }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = AppColors.LightBackground,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab, containerColor = AppColors.LightBackground) {
                listOf("Healthcare", "Retail", "Banking").forEachIndexed { i, label ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(label) })
                }
            }
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    ShieldCard {
                        Text("Stored only on this device, encrypted.", style = MaterialTheme.typography.bodySmall, color = AppColors.Grey5)
                        TextButton(onClick = viewModel::loadSampleData, enabled = !loading) {
                            Text(if (loading) "Loading…" else "Load synthetic sample data")
                        }
                    }
                }
                when (tab) {
                    0 -> {
                        val h = ui.healthcare
                        item {
                            ShieldCard {
                                SectionLabel("Patient")
                                if (h.patient == null) Text("No patient profile yet.", color = AppColors.Grey5)
                                h.patient?.let {
                                    KeyValueRow("Name", "${it.firstName} ${it.lastName}")
                                    KeyValueRow("Date of birth", it.dateOfBirth ?: "—")
                                    KeyValueRow("Allergies", it.allergies ?: "—")
                                }
                            }
                        }
                        item {
                            ShieldCard {
                                SectionLabel("Appointments")
                                if (h.appointments.isEmpty()) Text("None on file.", color = AppColors.Grey5)
                                h.appointments.forEach { a ->
                                    KeyValueRow(a.startsAt.dateTime(), "${a.providerName ?: "Appointment"} · ${a.organizationName.orEmpty()}")
                                    if (a.checkInStatus != "NOT_STARTED") StatusChip("Check-in: ${a.checkInStatus.lowercase().replace('_', ' ')}", AppColors.Success)
                                }
                            }
                        }
                        item {
                            ShieldCard {
                                SectionLabel("Medications")
                                if (h.medications.isEmpty()) Text("None on file.", color = AppColors.Grey5)
                                h.medications.forEach { KeyValueRow(it.name, listOfNotNull(it.dosage.ifBlank { null }, it.frequency).joinToString(" · ")) }
                            }
                        }
                        item {
                            ShieldCard {
                                SectionLabel("Insurance")
                                if (h.insurance.isEmpty()) Text("None on file.", color = AppColors.Grey5)
                                h.insurance.forEach { KeyValueRow(it.payerName, "Member ••${it.memberId.takeLast(4)}${it.groupNumber?.let { g -> " · Group $g" } ?: ""}") }
                            }
                        }
                    }
                    1 -> {
                        if (ui.purchases.isEmpty()) item { Text("No purchases yet. Import a receipt to add one.", color = AppColors.Grey5) }
                        items(ui.purchases, key = { it.id }) { p ->
                            ShieldCard {
                                Text(p.merchantName, fontWeight = FontWeight.SemiBold)
                                KeyValueRow("Order", p.orderNumber)
                                KeyValueRow("Date", p.purchasedAt.date())
                                KeyValueRow("Total", AuthorizedDataAccess.money(p.totalMinor, p.currency))
                                if (p.hasReceipt) StatusChip("Receipt on file", AppColors.Success)
                            }
                        }
                    }
                    else -> {
                        item {
                            ShieldCard {
                                SectionLabel("Accounts")
                                if (ui.accounts.isEmpty()) Text("No accounts yet. Import a statement to add one.", color = AppColors.Grey5)
                                ui.accounts.forEach { KeyValueRow(it.nickname, "${it.accountType.lowercase()} ••${it.last4}") }
                            }
                        }
                        items(ui.transactions, key = { it.id }) { t ->
                            ShieldCard {
                                KeyValueRow(t.postedAt.date(), "${t.merchantName ?: t.description} · ${AuthorizedDataAccess.money(t.amountMinor, t.currency)}")
                            }
                        }
                    }
                }
            }
        }
    }
}
