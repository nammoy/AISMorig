package com.app.aisheildme.bytrix.tier1_ui.consent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.aisheildme.bytrix.core.designsystem.AppColors
import com.app.aisheildme.bytrix.core.designsystem.component.CustomButton
import com.app.aisheildme.bytrix.tier1_ui.KeyValueRow
import com.app.aisheildme.bytrix.tier1_ui.SectionLabel
import com.app.aisheildme.bytrix.tier1_ui.StatusChip
import com.app.aisheildme.bytrix.tier3_negotiator.OrchestrationResult
import com.app.aisheildme.bytrix.tier3_negotiator.PolicyDecision

/**
 * Consent & approval (spec §5.8). Shows vendor identity, purpose, exactly what is shared, the
 * destination, the operation and consent status. Nothing is sent unless the user taps Approve.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsentApprovalSheet(
    result: OrchestrationResult,
    onApprove: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    val vendor = result.vendorDisplay ?: return
    val outbound = result.plan.outbound ?: return
    val policy = result.policy
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Review before sharing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppColors.BrandText)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (vendor.verified) Icons.Default.VerifiedUser else Icons.Default.GppBad,
                    contentDescription = null,
                    tint = if (vendor.verified) AppColors.Success else AppColors.RedColor,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(vendor.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text(vendor.verificationNote, style = MaterialTheme.typography.bodySmall, color = AppColors.Grey5)
                }
            }

            KeyValueRow("Purpose", outbound.purpose.label)
            KeyValueRow("Operation", outbound.operation)
            KeyValueRow("Destination", vendor.destination.ifBlank { "—" })
            KeyValueRow("Channel", vendor.channel.replace('_', ' ').lowercase())
            KeyValueRow(
                "Consent status",
                when (policy?.outcome) {
                    PolicyDecision.Outcome.ALLOW -> "Previously approved for this purpose"
                    PolicyDecision.Outcome.NEEDS_CONSENT -> "Needs your approval (valid 24 hours)"
                    PolicyDecision.Outcome.DENY -> "Blocked"
                    null -> "—"
                },
            )

            HorizontalDivider(color = AppColors.LightBorder)
            SectionLabel("Information that will be shared")
            if (vendor.dataPreview.isEmpty()) {
                Text("No personal information.", style = MaterialTheme.typography.bodySmall, color = AppColors.Grey5)
            }
            vendor.dataPreview.forEach { (label, value) -> KeyValueRow(label, value) }

            if (!policy?.blockedCategories.isNullOrEmpty()) {
                StatusChip("Withheld: " + policy!!.blockedCategories.joinToString { it.label }, AppColors.Grey5)
            }
            if (result.plan.missingInformation.isNotEmpty()) {
                StatusChip("Missing: " + result.plan.missingInformation.joinToString(), AppColors.Warning)
            }

            Spacer(Modifier.height(4.dp))
            CustomButton(title = "Approve & send", onClick = onApprove, enabled = vendor.verified)
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Don't send") }
            Spacer(Modifier.height(12.dp))
        }
    }
}
