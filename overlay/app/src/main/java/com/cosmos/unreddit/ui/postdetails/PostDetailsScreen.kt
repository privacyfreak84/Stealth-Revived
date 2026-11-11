package com.cosmos.unreddit.ui.postdetails

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.Comment
import com.cosmos.unreddit.data.model.Comment.CommentEntity
import com.cosmos.unreddit.data.model.Comment.MoreEntity
import com.cosmos.unreddit.data.model.PostType
import com.cosmos.unreddit.data.model.Resource
import com.cosmos.unreddit.data.model.db.PostEntity
import com.cosmos.unreddit.ui.common.compose.BackButton
import com.cosmos.unreddit.ui.common.compose.CommentActionSheet
import com.cosmos.unreddit.ui.common.compose.ErrorState
import com.cosmos.unreddit.ui.common.compose.LoadingRow
import com.cosmos.unreddit.ui.common.compose.LoadingState
import com.cosmos.unreddit.ui.common.compose.LocalLinkActions
import com.cosmos.unreddit.ui.common.compose.PostActionSheet
import com.cosmos.unreddit.ui.common.compose.PostCard
import com.cosmos.unreddit.ui.common.compose.RichText
import com.cosmos.unreddit.ui.common.compose.SortContext
import com.cosmos.unreddit.ui.common.compose.SortSheet
import com.cosmos.unreddit.ui.common.compose.Tag
import com.cosmos.unreddit.ui.common.compose.listPadding
import com.cosmos.unreddit.ui.common.compose.relativeTime
import com.cosmos.unreddit.ui.common.compose.rememberPostCallbacks
import com.cosmos.unreddit.ui.common.compose.sortingLabel
import com.cosmos.unreddit.ui.postlist.DefaultContentPreferences

/** A post with its comment tree. */
@Composable
fun PostDetailsScreen(
    permalink: String,
    singleThread: Boolean,
    initialPost: PostEntity?,
    onBack: () -> Unit,
    viewModel: PostDetailsViewModel = hiltViewModel(),
) {
    val links = LocalLinkActions.current
    LaunchedEffect(permalink) {
        if (viewModel.permalink.value == null) {
            viewModel.setPermalink(permalink)
            viewModel.setSingleThread(singleThread)
        }
        viewModel.loadPost(false)
    }
    val postResource by viewModel.post.collectAsStateWithLifecycle(initialValue = Resource.Loading())
    val comments by viewModel.comments.collectAsStateWithLifecycle()
    val sorting by viewModel.sorting.collectAsStateWithLifecycle()
    val isSingleThread by viewModel.singleThread.collectAsStateWithLifecycle()
    val post = (postResource as? Resource.Success)?.data ?: initialPost
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var showSort by rememberSaveable { mutableStateOf(false) }
    var showPostMenu by remember { mutableStateOf(false) }
    var commentMenu by remember { mutableStateOf<CommentEntity?>(null) }
    val callbacks = rememberPostCallbacks(viewModel) { showPostMenu = true }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    Text(post?.subreddit.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                subtitle = {
                    Text(
                        "${sortingLabel(sorting)}${post?.let { " · " + stringResource(R.string.label_comments_count, it.commentsNumber) } ?: ""}",
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    IconButton(onClick = { showSort = true }, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.AutoMirrored.Rounded.Sort, stringResource(R.string.sort_title))
                    }
                    IconButton(onClick = { viewModel.loadPost(true) }, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.Rounded.Refresh, stringResource(R.string.menu_refresh))
                    }
                    post?.let { p ->
                        IconButton(
                            onClick = { links.share("https://www.reddit.com${p.permalink}", p.title) },
                            shapes = IconButtonDefaults.shapes(),
                        ) { Icon(Icons.Rounded.Share, stringResource(R.string.menu_share_post)) }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { padding ->
        if (post == null) {
            when (val resource = postResource) {
                is Resource.Error -> ErrorState(
                    Modifier.padding(padding).fillMaxSize(),
                    message = resource.message ?: stringResource(R.string.network_retry_message),
                    onRetry = { viewModel.loadPost(true) },
                )
                else -> LoadingState(Modifier.padding(padding))
            }
            return@Scaffold
        }
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(top = padding.calculateTopPadding()).fillMaxSize(),
            contentPadding = listPadding(),
        ) {
            item(key = "post") {
                Column {
                    PostCard(post, DefaultContentPreferences.copy(showNsfwPreview = true, showSpoilerPreview = true), callbacks, expanded = true)
                    if (post.type == PostType.TEXT && post.selfRedditText.isNotEmpty()) {
                        RichText(
                            post.selfRedditText,
                            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                        )
                    }
                    post.crosspost?.let { crosspost ->
                        OutlinedCard(
                            onClick = { links.openPost(crosspost.permalink) },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            shape = MaterialTheme.shapes.large,
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    "${stringResource(R.string.post_crosspost)} · ${crosspost.subreddit}",
                                    style = MaterialTheme.typography.labelLargeEmphasized,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(crosspost.title, style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }
                }
            }
            if (isSingleThread) {
                item(key = "thread") {
                    Button(
                        onClick = {
                            viewModel.setPermalink(post.permalink)
                            viewModel.setSingleThread(false)
                            viewModel.loadPost(false)
                        },
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    ) {
                        Icon(Icons.Rounded.Forum, null, Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                        Text(stringResource(R.string.post_load_discussion))
                    }
                }
            }
            item(key = "spacer") { Spacer(Modifier.height(12.dp)) }
            when (val resource = comments.resource) {
                is Resource.Loading -> item(key = "loading") { LoadingRow(Modifier.padding(top = 24.dp)) }
                is Resource.Error -> item(key = "error") {
                    ErrorState(onRetry = { viewModel.loadPost(true) })
                }
                is Resource.Success -> items(
                    resource.data,
                    key = { it.name + it.depth },
                    contentType = { if (it is MoreEntity) "more" else "comment" },
                ) { comment ->
                    when (comment) {
                        is CommentEntity -> CommentItem(
                            comment = comment,
                            expanded = comment.isExpanded,
                            saved = comment.saved,
                            isOp = comment.author == post.author,
                            revision = comments.revision,
                            onClick = { viewModel.toggleComment(comment) },
                            onLongClick = { commentMenu = comment },
                            modifier = Modifier.animateItem(),
                        )
                        is MoreEntity -> MoreItem(
                            more = comment,
                            loading = comment.isLoading,
                            error = comment.isError,
                            onClick = { viewModel.loadMore(comment, post) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }

    if (showSort) {
        SortSheet(sorting, SortContext.COMMENTS, onDismiss = { showSort = false }) {
            viewModel.setSorting(it)
            viewModel.loadPost(false)
        }
    }
    if (showPostMenu && post != null) {
        PostActionSheet(
            post,
            onToggleSave = { viewModel.toggleSavePost(post); post.saved = !post.saved },
            onDismiss = { showPostMenu = false },
        )
    }
    commentMenu?.let { comment ->
        CommentActionSheet(comment, onToggleSave = { viewModel.toggle(comment) }, onDismiss = { commentMenu = null })
    }
}

private fun PostDetailsViewModel.toggle(comment: CommentEntity) = toggleSave(comment)

@Composable
private fun threadColors(): List<Color> = listOf(
    MaterialTheme.colorScheme.primary,
    MaterialTheme.colorScheme.secondary,
    MaterialTheme.colorScheme.tertiary,
    MaterialTheme.colorScheme.error,
    MaterialTheme.colorScheme.outline,
)

/** Thread lines for nested replies, one colored rail per level. */
@Composable
private fun ThreadRails(depth: Int) {
    val colors = threadColors()
    for (level in 1..depth.coerceAtMost(8)) {
        Box(
            Modifier
                .padding(start = if (level == 1) 4.dp else 10.dp)
                .width(2.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(1.dp))
                .background(colors[(level - 1) % colors.size].copy(alpha = 0.6f))
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CommentItem(
    comment: CommentEntity,
    expanded: Boolean,
    saved: Boolean,
    isOp: Boolean,
    revision: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 6.dp),
    ) {
        ThreadRails(comment.depth)
        Column(Modifier.weight(1f).padding(start = if (comment.depth > 0) 10.dp else 4.dp, end = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "u/${comment.author}",
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    color = if (isOp || comment.isSubmitter) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (comment.isSubmitter) Tag(stringResource(R.string.comment_op))
                if (comment.stickied) {
                    Icon(Icons.Rounded.PushPin, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.tertiary)
                }
                Text(
                    buildString {
                        if (!comment.scoreHidden) append(comment.score).append(" · ")
                        append(relativeTime(comment.created))
                        if (comment.edited > -1) append("*")
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                if (saved) Tag(stringResource(R.string.save))
            }
            AnimatedVisibility(expanded || !comment.hasReplies) {
                RichText(
                    comment.body,
                    Modifier.fillMaxWidth().padding(top = 4.dp),
                    fontSizeSp = 14.5f,
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
            }
            if (!expanded && comment.hasReplies) {
                Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.ExpandMore, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(
                        stringResource(R.string.label_replies_hidden, comment.visibleReplyCount.coerceAtLeast(comment.replies.size)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
    // Revision forces recomposition when only in-place flags changed.
    if (revision < 0) Unit
}

@Composable
internal fun MoreItem(
    more: MoreEntity,
    loading: Boolean,
    error: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(vertical = 2.dp)) {
        ThreadRails(more.depth)
        Box(Modifier.padding(start = 8.dp)) {
            if (loading) {
                LoadingIndicator(Modifier.size(36.dp))
            } else {
                TextButton(onClick = onClick, shapes = ButtonDefaults.shapes()) {
                    Text(
                        if (error) stringResource(R.string.comment_more_error)
                        else stringResource(R.string.comment_more, more.count),
                    )
                }
            }
        }
    }
}
