package com.app.aisheildme.bytrix.feature.folders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.aisheildme.bytrix.core.common.UiMessage
import com.app.aisheildme.bytrix.core.designsystem.AppColors
import com.app.aisheildme.bytrix.core.designsystem.CategoryIcons
import com.app.aisheildme.bytrix.core.designsystem.component.AppSnackbarHost
import com.app.aisheildme.bytrix.core.designsystem.component.CustomButton
import com.app.aisheildme.bytrix.core.designsystem.component.CustomTextField
import com.app.aisheildme.bytrix.core.designsystem.component.EmptyState
import com.app.aisheildme.bytrix.core.designsystem.component.ErrorState
import com.app.aisheildme.bytrix.core.designsystem.component.FullScreenLoader
import com.app.aisheildme.bytrix.core.designsystem.component.MessageEffect
import com.app.aisheildme.bytrix.core.designsystem.component.NextPageError
import com.app.aisheildme.bytrix.core.designsystem.component.NextPageSpinner
import com.app.aisheildme.bytrix.core.designsystem.component.SimpleAppBar
import com.app.aisheildme.bytrix.domain.model.DocumentCategory
import com.app.aisheildme.bytrix.domain.model.FolderCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(
    onBack: () -> Unit,
    onOpenFolder: (DocumentCategory) -> Unit,
    viewModel: FoldersViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val gridState = rememberLazyGridState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    MessageEffect(state.message, snackbarHostState, viewModel::onMessageShown)

    val nearEnd by remember {
        derivedStateOf {
            val info = gridState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            last >= info.totalItemsCount - 4
        }
    }
    LaunchedEffect(nearEnd) {
        if (nearEnd) viewModel.loadNextPage()
    }

    if (state.sheetVisible) {
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissSheet,
            sheetState = sheetState,
            containerColor = AppColors.LightSurface,
        ) {
            AddFolderSheet(
                name = state.name,
                selected = state.selectedCategory,
                isSaving = state.isSaving,
                canSave = state.canSave,
                onNameChange = viewModel::onNameChange,
                onCategorySelect = viewModel::onCategorySelect,
                onSave = viewModel::saveFolder,
            )
        }
    }

    Scaffold(
        topBar = { SimpleAppBar(title = "Folders", onBack = onBack) },
        snackbarHost = {
            AppSnackbarHost(snackbarHostState, state.message?.type != UiMessage.Type.Success)
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
                    text = "No folders yet",
                    actionText = "Create one",
                    onAction = viewModel::openSheet,
                )

                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    state = gridState,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(paging.items, key = { it.id }) { category ->
                        FolderCard(category, onOpenFolder)
                    }
                    if (paging.isLoadingNextPage) {
                        item(span = { GridItemSpan(maxLineSpan) }) { NextPageSpinner() }
                    } else if (paging.error != null) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            NextPageError(paging.error!!, viewModel::loadNextPage)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderCard(
    category: DocumentCategory,
    onOpen: (DocumentCategory) -> Unit,
) {
    val tint = CategoryIcons.parseColor(category.color)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.LightSurface)
            .clickable { onOpen(category) }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = CategoryIcons.forKey(category.icon.ifEmpty { category.slug }),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = category.name,
            style = MaterialTheme.typography.labelMedium,
            color = AppColors.BrandText,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun AddFolderSheet(
    name: String,
    selected: FolderCategory,
    isSaving: Boolean,
    canSave: Boolean,
    onNameChange: (String) -> Unit,
    onCategorySelect: (FolderCategory) -> Unit,
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
            text = "Create Folder",
            style = MaterialTheme.typography.titleLarge,
            color = AppColors.BrandText,
        )
        Spacer(Modifier.height(20.dp))

        CustomTextField(
            label = "Folder Name",
            placeholder = "Enter folder name",
            value = name,
            onValueChange = onNameChange,
        )
        Spacer(Modifier.height(20.dp))

        Text(
            text = "Category",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.BlackText,
        )
        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FolderCategory.entries.forEach { category ->
                CategoryChip(
                    category = category,
                    selected = category == selected,
                    onClick = { onCategorySelect(category) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(24.dp))

        CustomButton(
            title = "Create",
            onClick = onSave,
            enabled = canSave,
            isLoading = isSaving,
            height = 44.dp,
        )
    }
}

@Composable
private fun CategoryChip(
    category: FolderCategory,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = CategoryIcons.parseColor(category.colorHex)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) tint.copy(alpha = 0.12f) else AppColors.LightGray)
            .border(
                width = if (selected) 1.5.dp else 0.dp,
                color = if (selected) tint else androidx.compose.ui.graphics.Color.Transparent,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = CategoryIcons.forFolderCategory(category),
            contentDescription = null,
            tint = if (selected) tint else AppColors.TextHint,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = category.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) tint else AppColors.TextHint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
