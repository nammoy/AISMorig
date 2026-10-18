package com.app.aisheildme.bytrix.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.aisheildme.bytrix.core.common.PagedState

@Composable
fun <T : Any> PagedList(
    state: PagedState<T>,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    emptyText: String = "Nothing here yet",
    contentPadding: PaddingValues = PaddingValues(
        start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp,
    ),
    itemContent: @Composable (T) -> Unit,
) {
    val listState = rememberLazyListState()
    val nearEnd = listState.isNearEnd()

    LaunchedEffect(nearEnd) {
        if (nearEnd) onLoadMore()
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoadingFirstPage && state.items.isEmpty() -> FullScreenLoader()

            state.error != null && state.items.isEmpty() -> ErrorState(state.error, onRetry)

            state.isEmpty -> EmptyState(text = emptyText)

            else -> LazyColumn(
                state = listState,
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.items, key = key) { item -> itemContent(item) }
                if (state.isLoadingNextPage) {
                    item { NextPageSpinner() }
                } else if (state.error != null) {
                    item { NextPageError(state.error, onLoadMore) }
                }
            }
        }
    }
}
