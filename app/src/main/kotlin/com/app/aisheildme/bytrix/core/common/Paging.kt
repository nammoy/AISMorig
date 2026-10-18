package com.app.aisheildme.bytrix.core.common

data class PagedState<T>(
    val items: List<T> = emptyList(),
    val page: Int = 1,
    val isLoadingFirstPage: Boolean = true,
    val isLoadingNextPage: Boolean = false,
    val isRefreshing: Boolean = false,
    val endReached: Boolean = false,
    val error: String? = null,
) {
    val isEmpty: Boolean get() = items.isEmpty() && !isLoadingFirstPage && error == null

    val canLoadMore: Boolean
        get() = !endReached && !isLoadingNextPage && !isLoadingFirstPage

    fun loadingFirst() = copy(isLoadingFirstPage = true, error = null)

    fun loadingNext() = copy(isLoadingNextPage = true, error = null)

    fun refreshing() = copy(isRefreshing = true, error = null)

    fun appended(newItems: List<T>, pageSize: Int) = copy(
        items = if (page == FIRST_PAGE) newItems else items + newItems,
        page = page + 1,
        isLoadingFirstPage = false,
        isLoadingNextPage = false,
        isRefreshing = false,
        endReached = newItems.size < pageSize,
        error = null,
    )

    fun failed(message: String) = copy(
        isLoadingFirstPage = false,
        isLoadingNextPage = false,
        isRefreshing = false,
        error = message,
    )

    fun reset() = copy(page = FIRST_PAGE, endReached = false, error = null)

    fun removing(predicate: (T) -> Boolean) = copy(items = items.filterNot(predicate))

    companion object {
        const val FIRST_PAGE = 1
        const val PAGE_SIZE = 20
    }
}
