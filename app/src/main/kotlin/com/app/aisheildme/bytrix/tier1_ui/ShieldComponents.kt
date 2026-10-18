package com.app.aisheildme.bytrix.tier1_ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.aisheildme.bytrix.core.designsystem.AppColors
import com.app.aisheildme.bytrix.tier2_vault.model.TransactionState
import com.app.aisheildme.bytrix.tier3_negotiator.OrchestrationResult.Stage

@Composable
fun ShieldCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.LightSurface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
fun StatusChip(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
fun KeyValueRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = AppColors.Grey5, modifier = Modifier.width(132.dp))
        Spacer(Modifier.width(8.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, color = AppColors.BrandText, modifier = Modifier.weight(1f))
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = AppColors.Grey5)
}

data class StageStyle(val label: String, val color: Color)

fun Stage.style(): StageStyle = when (this) {
    Stage.ANSWERED -> StageStyle("Answered from your vault", AppColors.Info)
    Stage.NEEDS_INFORMATION -> StageStyle("More information needed", AppColors.Warning)
    Stage.AWAITING_APPROVAL -> StageStyle("Waiting for your approval", AppColors.Warning)
    Stage.BLOCKED -> StageStyle("Blocked by policy", AppColors.RedColor)
    Stage.PROCESSING -> StageStyle("Sending…", AppColors.Info)
    Stage.SUBMITTED -> StageStyle("Sent · awaiting confirmation", AppColors.Info)
    Stage.CONFIRMED -> StageStyle("Confirmed", AppColors.Success)
    Stage.MANUAL_ACTION_REQUIRED -> StageStyle("Your action needed", AppColors.Warning)
    Stage.QUEUED_OFFLINE -> StageStyle("Queued · not sent (offline)", AppColors.Grey5)
    Stage.FAILED -> StageStyle("Not completed", AppColors.RedColor)
    Stage.CANCELLED -> StageStyle("Cancelled · nothing shared", AppColors.Grey5)
}

fun TransactionState.style(): StageStyle = when (this) {
    TransactionState.DRAFT -> StageStyle("Draft", AppColors.Grey5)
    TransactionState.AWAITING_APPROVAL -> StageStyle("Awaiting approval", AppColors.Warning)
    TransactionState.APPROVED -> StageStyle("Approved · queued", AppColors.Info)
    TransactionState.PROCESSING -> StageStyle("Processing", AppColors.Info)
    TransactionState.SUBMITTED -> StageStyle("Submitted", AppColors.Info)
    TransactionState.CONFIRMED -> StageStyle("Confirmed", AppColors.Success)
    TransactionState.FAILED -> StageStyle("Failed", AppColors.RedColor)
    TransactionState.CANCELLED -> StageStyle("Cancelled", AppColors.Grey5)
    TransactionState.MANUAL_ACTION_REQUIRED -> StageStyle("Manual action needed", AppColors.Warning)
}

/** Progress through the request lifecycle shown under each assistant answer (spec §5.4). */
@Composable
fun StageProgress(stage: Stage) {
    val steps = listOf("Understood", "Planned", "Approved", "Sent", "Confirmed")
    val reached = when (stage) {
        Stage.ANSWERED, Stage.NEEDS_INFORMATION, Stage.BLOCKED -> 2
        Stage.AWAITING_APPROVAL, Stage.CANCELLED -> 2
        Stage.QUEUED_OFFLINE -> 3
        Stage.PROCESSING, Stage.SUBMITTED, Stage.MANUAL_ACTION_REQUIRED, Stage.FAILED -> 4
        Stage.CONFIRMED -> 5
    }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
        steps.forEachIndexed { index, label ->
            Column(modifier = Modifier.weight(1f)) {
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (index < reached) AppColors.BrandBlue else AppColors.LightBorder),
                )
                Spacer(Modifier.height(4.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = if (index < reached) AppColors.BrandText else AppColors.TextHint)
            }
        }
    }
}
