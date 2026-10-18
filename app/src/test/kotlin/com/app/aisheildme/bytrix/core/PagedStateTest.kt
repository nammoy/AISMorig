package com.app.aisheildme.bytrix.core

import com.app.aisheildme.bytrix.core.common.PagedState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PagedStateTest {

    @Test
    fun `a full page is not the end`() {
        val state = PagedState<Int>().loadingFirst().appended((1..20).toList(), 20)
        assertFalse(state.endReached)
        assertEquals(2, state.page)
        assertTrue(state.canLoadMore)
    }

    @Test
    fun `a short page ends the list`() {
        val state = PagedState<Int>().loadingFirst().appended((1..7).toList(), 20)
        assertTrue(state.endReached)
        assertFalse(state.canLoadMore)
    }

    @Test
    fun `an empty first page is empty, not loading`() {
        val state = PagedState<Int>().loadingFirst().appended(emptyList(), 20)
        assertTrue(state.isEmpty)
        assertTrue(state.endReached)
    }

    @Test
    fun `the second page appends rather than replacing`() {
        val state = PagedState<Int>()
            .loadingFirst().appended((1..20).toList(), 20)
            .loadingNext().appended((21..30).toList(), 20)

        assertEquals(30, state.items.size)
        assertEquals(1, state.items.first())
        assertEquals(30, state.items.last())
        assertTrue(state.endReached)
    }

    @Test
    fun `reset then append replaces the list instead of duplicating it`() {
        val loaded = PagedState<Int>().loadingFirst().appended((1..20).toList(), 20)
        val refreshed = loaded.reset().refreshing().appended((100..104).toList(), 20)

        assertEquals(5, refreshed.items.size)
        assertEquals(100, refreshed.items.first())
    }

    @Test
    fun `a failure keeps the items already on screen`() {
        val state = PagedState<Int>()
            .loadingFirst().appended((1..20).toList(), 20)
            .loadingNext().failed("network down")

        assertEquals(20, state.items.size)
        assertEquals("network down", state.error)
        assertTrue(state.canLoadMore)
        assertFalse(state.isLoadingNextPage)
    }

    @Test
    fun `removing drops the item without a refetch`() {
        val state = PagedState<Int>().loadingFirst().appended((1..5).toList(), 20)
            .removing { it == 3 }

        assertEquals(listOf(1, 2, 4, 5), state.items)
    }

    @Test
    fun `canLoadMore is false while a page is in flight`() {
        val state = PagedState<Int>().loadingFirst().appended((1..20).toList(), 20).loadingNext()
        assertFalse(state.canLoadMore)
    }
}
