package com.app.aisheildme.bytrix.feature.documents

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.aisheildme.bytrix.core.common.UiMessage
import com.app.aisheildme.bytrix.core.designsystem.AppColors
import com.app.aisheildme.bytrix.core.designsystem.component.AppSnackbarHost
import com.app.aisheildme.bytrix.core.designsystem.component.ConfirmDialog
import com.app.aisheildme.bytrix.core.designsystem.component.CustomButton
import com.app.aisheildme.bytrix.core.designsystem.component.CustomTextField
import com.app.aisheildme.bytrix.core.designsystem.component.EmptyState
import com.app.aisheildme.bytrix.core.designsystem.component.ErrorState
import com.app.aisheildme.bytrix.core.designsystem.component.FullScreenLoader
import com.app.aisheildme.bytrix.core.designsystem.component.MessageEffect
import com.app.aisheildme.bytrix.core.designsystem.component.NextPageError
import com.app.aisheildme.bytrix.core.designsystem.component.NextPageSpinner
import com.app.aisheildme.bytrix.core.designsystem.component.SimpleAppBar
import com.app.aisheildme.bytrix.core.designsystem.component.isNearEnd
import com.app.aisheildme.bytrix.core.util.TimeFormat
import com.app.aisheildme.bytrix.domain.model.DocumentItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    onBack: () -> Unit,
    onShare: (DocumentItem) -> Unit,
    viewModel: DocumentsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val pickFile = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val name = context.contentResolver
            .query(uri, null, null, null, null)
            ?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
        viewModel.onFilePicked(uri, name)
    }

    MessageEffect(state.message, snackbarHostState, viewModel::onMessageShown)

    val nearEnd = listState.isNearEnd()
    LaunchedEffect(nearEnd) {
        if (nearEnd) viewModel.loadNextPage()
    }

    state.pendingDelete?.let { document ->
        ConfirmDialog(
            title = "Delete Document",
            message = "Are you sure you want to delete \"${document.title}\"? " +
                "This action cannot be undone.",
            confirmText = "Delete",
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::dismissDelete,
        )
    }

    if (state.sheetVisible) {
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissSheet,
            sheetState = sheetState,
            containerColor = AppColors.LightSurface,
        ) {
            AddDocumentSheet(
                title = state.title,
                fileName = state.pickedFileName,
                isSaving = state.isSaving,
                canSave = state.canSave,
                onTitleChange = viewModel::onTitleChange,
                onPickFile = { pickFile.launch(arrayOf("*/*")) },
                onSave = viewModel::saveDocument,
            )
        }
    }

    Scaffold(
        topBar = { SimpleAppBar(title = state.folderName, onBack = onBack) },
        snackbarHost = {
            AppSnackbarHost(snackbarHostState, state.message?.type != UiMessage.Type.Success)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::openSheet,
                containerColor = AppColors.BrandBlue,
                contentColor = Color.White,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add document")
            }
        },
        containerColor = AppColors.LightBackground,
    ) { padding ->
        val paging = state.paging
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                paging.isLoadingFirstPage && paging.items.isEmpty() -> FullScreenLoader()

                paging.error != null && paging.items.isEmpty() ->
                    ErrorState(paging.error!!, viewModel::loadFirstPage)

                paging.isEmpty -> EmptyState(
                    text = "No documents in this folder yet",
                    actionText = "Upload one",
                    onAction = viewModel::openSheet,
                )

                else -> LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(
                        start = 24.dp, end = 24.dp, top = 8.dp, bottom = 96.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(paging.items, key = { it.id }) { document ->
                        DocumentRow(
                            document = document,
                            onOpen = { viewModel.openDocument(context, document) },
                            onShare = { onShare(document) },
                            onDelete = { viewModel.askDelete(document) },
                        )
                    }
                    if (paging.isLoadingNextPage) {
                        item { NextPageSpinner() }
                    } else if (paging.error != null) {
                        item { NextPageError(paging.error!!, viewModel::loadNextPage) }
                    }
                }
            }

            if (state.isOpening) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    FullScreenLoader()
                }
            }
        }
    }
}

@Composable
private fun DocumentRow(
    document: DocumentItem,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.LightSurface)
            .clickable(onClick = onOpen)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AppColors.BrandBlue.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = when {
                    document.isPdf -> Icons.Default.PictureAsPdf
                    document.isImage -> Icons.Default.Image
                    else -> Icons.Default.Description
                },
                contentDescription = null,
                tint = AppColors.BrandBlue,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = document.title,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.BrandText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = listOf(
                    TimeFormat.shortDate(document.createdAtEpochMs),
                    TimeFormat.shortTime(document.createdAtEpochMs),
                ).filter { it.isNotEmpty() }.joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.TextHint,
            )
        }
        Icon(
            imageVector = Icons.Default.Share,
            contentDescription = "Share",
            tint = AppColors.TextHint,
            modifier = Modifier
                .size(20.dp)
                .clickable(onClick = onShare),
        )
        Spacer(Modifier.size(16.dp))
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Delete",
            tint = AppColors.Error,
            modifier = Modifier
                .size(20.dp)
                .clickable(onClick = onDelete),
        )
    }
}

@Composable
fun AddDocumentSheet(
    title: String,
    fileName: String?,
    isSaving: Boolean,
    canSave: Boolean,
    onTitleChange: (String) -> Unit,
    onPickFile: () -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
    ) {
        Text(
            text = "Add Document",
            style = MaterialTheme.typography.titleLarge,
            color = AppColors.BrandText,
        )
        Spacer(Modifier.height(20.dp))

        CustomTextField(
            label = "Title",
            placeholder = "Enter document title",
            value = title,
            onValueChange = onTitleChange,
        )
        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(AppColors.LightGray)
                .clickable(onClick = onPickFile)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = AppColors.BrandBlue,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.size(12.dp))
            Text(
                text = fileName ?: "Choose a file",
                style = MaterialTheme.typography.bodyMedium,
                color = if (fileName != null) AppColors.BrandText else AppColors.TextHint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(24.dp))

        CustomButton(
            title = "Upload",
            onClick = onSave,
            enabled = canSave,
            isLoading = isSaving,
            height = 44.dp,
        )
    }
}
