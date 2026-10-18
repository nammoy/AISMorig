package com.app.aisheildme.bytrix.tier1_ui.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.aisheildme.bytrix.core.designsystem.AppColors
import com.app.aisheildme.bytrix.core.designsystem.component.SimpleAppBar
import com.app.aisheildme.bytrix.tier1_ui.KeyValueRow
import com.app.aisheildme.bytrix.tier1_ui.ShieldCard
import com.app.aisheildme.bytrix.tier1_ui.StatusChip
import com.app.aisheildme.bytrix.tier1_ui.style
import com.app.aisheildme.bytrix.tier2_vault.model.DataCategory
import com.app.aisheildme.bytrix.tier2_vault.model.Purpose
import com.app.aisheildme.bytrix.tier2_vault.model.TransactionState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val STAMP = DateTimeFormatter.ofPattern("MMM d · h:mm a", Locale.US)
private fun Long.stamp() = STAMP.format(Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()))

/** Vendor interactions, consents and activity history in one place. [startTab]: 0 requests, 1 consents, 2 history. */
@Composable
fun ActivityScreen(
    onBack: () -> Unit,
    startTab: Int = 0,
    viewModel: ActivityViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(startTab) }

    Scaffold(
        topBar = { SimpleAppBar(title = if (tab == 1) "Consent Management" else "Activity & Interactions", onBack = onBack) },
        containerColor = AppColors.LightBackground,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab, containerColor = AppColors.LightBackground) {
                listOf("Requests", "Consents", "History").forEachIndexed { i, label ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(label) })
                }
            }
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (tab) {
                    0 -> {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(ui.resumeMessage ?: "Requests to outside organizations", style = MaterialTheme.typography.bodySmall, color = AppColors.Grey5, modifier = Modifier.weight(1f))
                                TextButton(onClick = { viewModel.resumePending() }) { Text("Resume pending") }
                            }
                        }
                        if (ui.requests.isEmpty()) item { Text("No vendor interactions yet.", color = AppColors.Grey5) }
                        items(ui.requests, key = { it.id }) { r ->
                            val state = runCatching { TransactionState.valueOf(r.status) }.getOrNull()
                            ShieldCard {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(ui.vendorNames[r.vendorId] ?: "Organization", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                    state?.style()?.let { StatusChip(it.label, it.color) }
                                }
                                KeyValueRow("Purpose", runCatching { Purpose.valueOf(r.purpose).label }.getOrDefault(r.purpose))
                                KeyValueRow("Channel", r.channel.replace('_', ' ').lowercase())
                                KeyValueRow("Updated", r.updatedAt.stamp())
                                KeyValueRow("Request ID", r.requestUuid.take(8))
                            }
                        }
                    }
                    1 -> {
                        if (ui.consents.isEmpty()) item { Text("You haven't shared anything with an organization yet.", color = AppColors.Grey5) }
                        items(ui.consents, key = { it.id }) { c ->
                            ShieldCard {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(ui.vendorNames[c.vendorId] ?: "Organization", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                    StatusChip(c.status.lowercase().replaceFirstChar { it.uppercase() }, if (c.status == "GRANTED") AppColors.Success else AppColors.Grey5)
                                }
                                KeyValueRow("Purpose", runCatching { Purpose.valueOf(c.purpose).label }.getOrDefault(c.purpose))
                                KeyValueRow(
                                    "Data",
                                    c.dataCategories.split(",").mapNotNull { n -> DataCategory.entries.firstOrNull { it.name == n }?.label }.joinToString(),
                                )
                                KeyValueRow("Granted", c.grantedAt.stamp())
                                c.expiresAt?.let { KeyValueRow("Expires", it.stamp()) }
                                if (c.status == "GRANTED") TextButton(onClick = { viewModel.revoke(c.id) }) { Text("Revoke") }
                            }
                        }
                    }
                    else -> {
                        if (ui.audit.isEmpty()) item { Text("No activity yet.", color = AppColors.Grey5) }
                        items(ui.audit, key = { it.id }) { e ->
                            ShieldCard {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(e.action.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                    StatusChip(e.outcome.lowercase(), if (e.outcome == "SUCCESS") AppColors.Success else AppColors.Grey5)
                                }
                                KeyValueRow("When", e.occurredAt.stamp())
                                e.domain?.let { KeyValueRow("Domain", it.lowercase()) }
                                e.detail?.let { KeyValueRow("Detail", it) }
                            }
                        }
                    }
                }
            }
        }
    }
}
