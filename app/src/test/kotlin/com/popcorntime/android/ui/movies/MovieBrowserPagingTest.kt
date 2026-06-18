package com.popcorntime.android.ui.movies

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pagination decision logic: hasMore must follow the RAW page size, and pages
 * that client-side filters empty out trigger bounded auto-fetching.
 */
class MovieBrowserPagingTest {

    @Test
    fun `auto fetch continues while everything fetched so far is filtered out`() {
        assertTrue(shouldAutoFetchNextPage(filteredPageEmpty = true, rawPageCount = 50, attempts = 1))
    }

    @Test
    fun `auto fetch stops once filtered results exist`() {
        assertFalse(shouldAutoFetchNextPage(filteredPageEmpty = false, rawPageCount = 50, attempts = 1))
    }

    @Test
    fun `auto fetch stops at end of catalogue even when filtered empty`() {
        // Raw page empty = the server has no more pages — hasMore must be false
        // and no further fetches happen.
        assertFalse(shouldAutoFetchNextPage(filteredPageEmpty = true, rawPageCount = 0, attempts = 1))
    }

    @Test
    fun `auto fetch is bounded`() {
        assertTrue(
            shouldAutoFetchNextPage(
                filteredPageEmpty = true,
                rawPageCount = 50,
                attempts = MovieBrowserViewModel.MAX_AUTO_FETCH_PAGES - 1,
            )
        )
        assertFalse(
            shouldAutoFetchNextPage(
                filteredPageEmpty = true,
                rawPageCount = 50,
                attempts = MovieBrowserViewModel.MAX_AUTO_FETCH_PAGES,
            )
        )
    }
}
