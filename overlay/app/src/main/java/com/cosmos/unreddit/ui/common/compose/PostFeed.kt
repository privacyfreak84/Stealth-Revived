package com.cosmos.unreddit.ui.common.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MediumFloatingActionButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.db.PostEntity
import com.cosmos.unreddit.data.model.preferences.ContentPreferences
import com.cosmos.unreddit.ui.base.BaseViewModel
import kotlinx.coroutines.launch

/** Standard post callbacks: open details, media, links, and the post action sheet. */
@Composable
fun rememberPostCallbacks(
    viewModel: BaseViewModel,
    onMenu: (PostEntity) -> Unit,
): PostCallbacks {
    val links = LocalLinkActions.current
    return remember(viewModel, links) {
        object : PostCallbacks {
            override fun onOpen(post: PostEntity) {
                viewModel.insertPostInHistory(post.id)
                post.seen = true
                com.cosmos.unreddit.ui.navigation.NavHandoff.putPost(post)
                links.openPost(post.permalink)
            }

            override fun onMedia(post: PostEntity) {
                viewModel.insertPostInHistory(post.id)
                if (post.gallery.isNotEmpty()) links.openGallery(post.gallery)
                else links.openMedia(post.mediaUrl, post.mediaType)
            }

            override fun onLink(post: PostEntity) {
                viewModel.insertPostInHistory(post.id)
                links.handleLink(post.url)
            }

            override fun onSubreddit(post: PostEntity) = links.openSubreddit(post.subreddit)
            override fun onUser(post: PostEntity) = links.openUser(post.author)
            override fun onToggleSave(post: PostEntity) = viewModel.toggleSavePost(post)
            override fun onMenu(post: PostEntity) = onMenu(post)
        }
    }
}

/**
 * Paged post list with Expressive pull-to-refresh, loading and error states, and a
 * scroll-to-top button.
 */
@Composable
fun PostFeed(
    posts: LazyPagingItems<PostEntity>,
    preferences: ContentPreferences,
    viewModel: BaseViewModel,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = listPadding(),
    header: (LazyListScope.() -> Unit)? = null,
) {
    var menuPost by remember { mutableStateOf<PostEntity?>(null) }
    val callbacks = rememberPostCallbacks(viewModel) { menuPost = it }
    val refreshState = rememberPullToRefreshState()
    val refreshing = posts.loadState.refresh is LoadState.Loading && posts.itemCount > 0
    val scope = rememberCoroutineScope()

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = { posts.refresh() },
        state = refreshState,
        modifier = modifier.fillMaxSize(),
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                state = refreshState,
                isRefreshing = refreshing,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        },
    ) {
        LazyColumn(
            state = listState,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            header?.invoke(this)
            when (val refresh = posts.loadState.refresh) {
                is LoadState.Loading -> if (posts.itemCount == 0) item { LoadingRow(Modifier.padding(top = 96.dp)) }
                is LoadState.Error -> if (posts.itemCount == 0) item {
                    ErrorState(
                        message = refresh.error.message ?: stringResource(R.string.network_retry_message),
                        onRetry = { posts.retry() },
                    )
                }
                is LoadState.NotLoading -> if (posts.itemCount == 0 && refresh.endOfPaginationReached.not()) {
                    // Waiting for the first page.
                } else if (posts.itemCount == 0) item { EmptyState() }
            }
            items(
                count = posts.itemCount,
                key = { index -> posts.peek(index)?.let { "${it.id}-$index" } ?: index },
                contentType = { "post" },
            ) { index ->
                posts[index]?.let { post ->
                    PostCard(post, preferences, callbacks, Modifier.animateItem())
                }
            }
            when (posts.loadState.append) {
                is LoadState.Loading -> item { LoadingRow() }
                is LoadState.Error -> item { ErrorState(onRetry = { posts.retry() }) }
                else -> Unit
            }
        }

        val showTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 4 } }
        AnimatedVisibility(
            visible = showTop,
            enter = scaleIn(),
            exit = scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = contentPadding.calculateBottomPadding()),
        ) {
            MediumFloatingActionButton(onClick = { scope.launch { listState.animateScrollToItem(0) } }) {
                Icon(Icons.Rounded.KeyboardArrowUp, stringResource(R.string.action_scroll_to_top))
            }
        }
    }

    menuPost?.let { post ->
        PostActionSheet(
            post = post,
            onToggleSave = {
                viewModel.toggleSavePost(post)
                post.saved = !post.saved
            },
            onDismiss = { menuPost = null },
        )
    }
}
