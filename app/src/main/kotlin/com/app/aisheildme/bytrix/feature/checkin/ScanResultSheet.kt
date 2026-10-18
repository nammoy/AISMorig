package com.app.aisheildme.bytrix.feature.checkin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.app.aisheildme.bytrix.core.designsystem.AppColors
import com.app.aisheildme.bytrix.core.designsystem.component.CustomButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultSheet(
    state: CheckInFormState,
    onRetry: () -> Unit,
    onToggleRawText: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scan = state.lastScan ?: return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.LightSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = "Scan result",
                style = MaterialTheme.typography.titleLarge,
                color = AppColors.BrandText,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${scan.pageCount} page${if (scan.pageCount == 1) "" else "s"} captured",
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.TextHint,
            )
            Spacer(Modifier.height(16.dp))

            if (scan.pages.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    scan.pages.take(3).forEach { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = 84.dp, height = 110.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppColors.LightGray),
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            when {
                state.isReadingScan -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = AppColors.BrandBlue,
                    )
                    Spacer(Modifier.size(10.dp))
                    Text(
                        text = state.aiDownloadLabel ?: "Reading the form…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.BlackText,
                    )
                }

                state.aiError != null -> Column {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = AppColors.Warning,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.size(10.dp))
                        Text(
                            text = state.aiError,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.BlackText,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onRetry) { Text("Try again") }
                }

                state.scannedFieldCount > 0 -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = AppColors.Success,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.size(10.dp))
                    Text(
                        text = "Applied ${state.scannedFieldCount} field" +
                            "${if (state.scannedFieldCount == 1) "" else "s"} from your scan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.BlackText,
                    )
                }

                else -> Text(
                    text = "Nothing could be read from that scan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextHint,
                )
            }

            if (scan.hasText) {
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onToggleRawText) {
                    Text(
                        text = if (state.showRawScanText) "Hide scanned text" else "Show scanned text",
                        color = AppColors.BrandBlue,
                    )
                }
                if (state.showRawScanText) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppColors.LightGray)
                            .padding(12.dp),
                    ) {
                        Text(
                            text = scan.rawText,
                            style = MaterialTheme.typography.labelMedium,
                            color = AppColors.BlackText,
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            CustomButton(title = "Done", onClick = onDismiss, height = 44.dp)
        }
    }
}
