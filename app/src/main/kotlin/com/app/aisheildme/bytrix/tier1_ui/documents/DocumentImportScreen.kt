package com.app.aisheildme.bytrix.tier1_ui.documents

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.aisheildme.bytrix.core.designsystem.AppColors
import com.app.aisheildme.bytrix.core.designsystem.component.CustomButton
import com.app.aisheildme.bytrix.core.designsystem.component.SimpleAppBar
import com.app.aisheildme.bytrix.tier1_ui.SectionLabel
import com.app.aisheildme.bytrix.tier1_ui.ShieldCard
import com.app.aisheildme.bytrix.tier1_ui.StatusChip
import com.app.aisheildme.bytrix.tier4_vendor.ocr.DocumentType
import com.app.aisheildme.bytrix.tier4_vendor.ocr.FormMapper
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

@Composable
fun DocumentImportScreen(
    onBack: () -> Unit,
    viewModel: DocumentImportViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val scanner = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val pages = GmsDocumentScanningResult.fromActivityResultIntent(result.data)?.pages?.map { it.imageUri }.orEmpty()
        viewModel.onPages(pages, fromScanner = true)
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.onPages(listOf(uri), fromScanner = false)
    }

    Scaffold(
        topBar = { SimpleAppBar(title = "Import Document", onBack = onBack) },
        containerColor = AppColors.LightBackground,
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (val state = ui) {
                ImportUi.Idle, is ImportUi.Failed -> {
                    ShieldCard {
                        Text("Add a document to your vault", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Insurance cards, medical forms, receipts and bank statements are read on this device with Google ML Kit. You review every field before anything is saved.",
                            style = MaterialTheme.typography.bodySmall, color = AppColors.Grey5,
                        )
                        if (state is ImportUi.Failed) Text(state.message, color = AppColors.RedColor, style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedButton(
                        onClick = {
                            val options = GmsDocumentScannerOptions.Builder()
                                .setGalleryImportAllowed(true)
                                .setPageLimit(5)
                                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                                .build()
                            GmsDocumentScanning.getClient(options)
                                .getStartScanIntent(context as Activity)
                                .addOnSuccessListener { scanner.launch(IntentSenderRequest.Builder(it).build()) }
                                .addOnFailureListener { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.DocumentScanner, contentDescription = null)
                        Text("  Scan with camera")
                    }
                    OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Text("  Choose a photo")
                    }
                }

                ImportUi.Reading -> ShieldCard {
                    CircularProgressIndicator()
                    Text("Reading the document on this device…", style = MaterialTheme.typography.bodySmall)
                }

                is ImportUi.Review -> {
                    ShieldCard {
                        SectionLabel("Document type")
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            DocumentType.entries.filter { it != DocumentType.UNKNOWN && it != DocumentType.VENDOR_LETTER }.forEach { t ->
                                FilterChip(selected = state.type == t, onClick = { viewModel.onTypeChange(t) }, label = { Text(t.label) })
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatusChip("${(state.analysis.confidence * 100).toInt()}% match", AppColors.Info)
                            if (state.analysis.aiAssisted) StatusChip("Gemini Nano assisted", AppColors.BrandBlue)
                        }
                        state.analysis.summary?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = AppColors.Grey7) }
                    }
                    ShieldCard {
                        SectionLabel("Review the fields")
                        state.fields.forEach { field ->
                            val issue = state.issues.firstOrNull { it.key == field.key }
                            OutlinedTextField(
                                value = field.value,
                                onValueChange = { viewModel.onFieldChange(field.key, it) },
                                label = { Text(FormMapper.label(field.key)) },
                                isError = issue?.blocking == true,
                                supportingText = if (issue != null) { { Text(issue.message) } } else null,
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = field.key != "transactions",
                            )
                        }
                    }
                    ShieldCard {
                        SectionLabel("Proposed changes to your vault")
                        if (state.changes.isEmpty()) Text("Nothing to save yet — complete the required fields.", color = AppColors.Grey5, style = MaterialTheme.typography.bodySmall)
                        state.changes.forEach { Text("• ${it.description}", style = MaterialTheme.typography.bodySmall) }
                    }
                    state.error?.let { Text(it, color = AppColors.RedColor, style = MaterialTheme.typography.bodySmall) }
                    CustomButton(title = "Approve & save to vault", onClick = viewModel::approveAndSave, enabled = state.canSave, isLoading = state.saving)
                    OutlinedButton(onClick = viewModel::reset, modifier = Modifier.fillMaxWidth()) { Text("Discard") }
                }

                is ImportUi.Done -> {
                    ShieldCard {
                        Text(if (state.outcome.duplicate) "Already in your vault" else "Saved to your vault", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        state.outcome.saved.forEach { Text("✓ $it", style = MaterialTheme.typography.bodySmall) }
                        state.outcome.skipped.forEach { Text("– $it", style = MaterialTheme.typography.bodySmall, color = AppColors.Grey5) }
                    }
                    CustomButton(title = "Import another", onClick = viewModel::reset)
                    OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Done") }
                }
            }
        }
    }
}
