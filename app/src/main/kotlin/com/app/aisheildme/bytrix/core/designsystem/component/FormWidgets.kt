package com.app.aisheildme.bytrix.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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

@Composable
fun FormCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.LightSurface)
            .padding(16.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = AppColors.BrandText,
        )
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
fun ReadOnlyField(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = AppColors.TextHint,
            modifier = Modifier.weight(0.45f),
        )
        Text(
            text = value.ifBlank { "—" },
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.BrandText,
            modifier = Modifier.weight(0.55f),
        )
    }
}

@Composable
fun OptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) AppColors.BrandBlue.copy(alpha = 0.10f) else AppColors.LightGray,
            )
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) AppColors.BrandBlue else AppColors.LightBorder,
                shape = RoundedCornerShape(20.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) AppColors.BrandBlue else AppColors.BlackText,
        )
    }
}

@Composable
fun PainScale(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            (0..10).forEach { level ->
                val selected = level == value
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(CircleShape)
                        .background(if (selected) painColor(level) else AppColors.LightGray)
                        .clickable { onValueChange(level) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = level.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) Color.White else AppColors.TextHint,
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("No pain", style = MaterialTheme.typography.labelMedium, color = AppColors.TextHint)
            Text("Worst", style = MaterialTheme.typography.labelMedium, color = AppColors.TextHint)
        }
    }
}

private fun painColor(level: Int): Color = when {
    level <= 3 -> Color(0xFF4CAF50)
    level <= 6 -> Color(0xFFFFA726)
    else -> Color(0xFFE53935)
}

@Composable
fun AiSummaryCard(
    summary: String,
    deviceLabel: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.BrandBlue.copy(alpha = 0.05f))
            .border(1.dp, AppColors.BrandBlue.copy(alpha = 0.20f), RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Text(
            text = "AI Summary",
            style = MaterialTheme.typography.titleMedium,
            color = AppColors.BrandBlue,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.BlackText,
        )
        if (deviceLabel != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Processed on-device on $deviceLabel",
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.TextHint,
            )
        }
    }
}
