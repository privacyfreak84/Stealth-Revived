package com.cosmos.unreddit.data.remote.datasource

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.cosmos.unreddit.data.model.Sorting
import com.cosmos.unreddit.data.remote.api.reddit.model.Child
import com.cosmos.unreddit.data.remote.api.reddit.model.Listing
import com.cosmos.unreddit.data.remote.api.reddit.source.CurrentSource
import kotlinx.coroutines.CancellationException

/**
 * Paging source for single post lists, including global and subreddit search.
 *
 * Transport adapters can fail with runtime exceptions as Reddit changes its response
 * shape. Those failures must become a Paging error state, not escape the load coroutine
 * and crash the search screen.
 */
open class PostListDataSource(
    private val source: CurrentSource,
    private val query: String,
    private val sorting: Sorting
) : PagingSource<String, Child>() {

    override val keyReuseSupported: Boolean = true

    override suspend fun load(params: LoadParams<String>): LoadResult<String, Child> {
        return try {
            val response = getResponse(query, sorting, params.key)
            val data = response.data
            LoadResult.Page(data.children, null, data.after)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Log.e("PostListDataSource", "Unable to load post/search results", exception)
            LoadResult.Error(exception)
        }
    }

    open suspend fun getResponse(
        query: String,
        sorting: Sorting,
        after: String?
    ): Listing {
        return source.getSubreddit(query, sorting.generalSorting, sorting.timeSorting, after)
    }

    override fun getRefreshKey(state: PagingState<String, Child>): String? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey
        }
    }
}
