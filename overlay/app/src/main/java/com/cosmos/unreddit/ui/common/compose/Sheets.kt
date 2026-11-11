package com.cosmos.unreddit.ui.common.compose

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.Comment
import com.cosmos.unreddit.data.model.Sort
import com.cosmos.unreddit.data.model.Sorting
import com.cosmos.unreddit.data.model.TimeSorting
import com.cosmos.unreddit.data.model.db.PostEntity
import kotlinx.coroutines.launch

/** Which sort options a feed offers. */
enum class SortContext(val sorts: List<Sort>, val timed: Set<Sort>) {
    FEED(
        listOf(Sort.HOT, Sort.NEW, Sort.TOP, Sort.RISING, Sort.CONTROVERSIAL),
        setOf(Sort.TOP, Sort.CONTROVERSIAL)
    ),
    SEARCH(
        listOf(Sort.RELEVANCE, Sort.HOT, Sort.TOP, Sort.NEW, Sort.COMMENTS),
        setOf(Sort.RELEVANCE, Sort.TOP, Sort.COMMENTS)
    ),
    USER(
        listOf(Sort.HOT, Sort.NEW, Sort.TOP, Sort.CONTROVERSIAL),
        setOf(Sort.TOP, Sort.CONTROVERSIAL)
    ),
    COMMENTS(
        listOf(Sort.BEST, Sort.TOP, Sort.NEW, Sort.CONTROVERSIAL, Sort.OLD, Sort.QA),
        emptySet()
    ),
}

@StringRes
fun Sort.label(): Int = when (this) {
    Sort.HOT -> R.string.sort_hot
    Sort.NEW -> R.string.sort_new
    Sort.TOP -> R.string.sort_top
    Sort.RISING -> R.string.sort_rising
    Sort.CONTROVERSIAL -> R.string.sort_controversial
    Sort.RELEVANCE -> R.string.sort_relevance
    Sort.COMMENTS -> R.string.sort_comments
    Sort.BEST -> R.string.sort_best
    Sort.OLD -> R.string.sort_old
    Sort.QA -> R.string.sort_qa
}

@StringRes
fun TimeSorting.label(): Int = when (this) {
    TimeSorting.HOUR -> R.string.sort_time_hour_short
    TimeSorting.DAY -> R.string.sort_time_day_short
    TimeSorting.WEEK -> R.string.sort_time_week_short
    TimeSorting.MONTH -> R.string.sort_time_month_short
    TimeSorting.YEAR -> R.string.sort_time_year_short
    TimeSorting.ALL -> R.string.sort_time_all_short
}

@Composable
fun sortingLabel(sorting: Sorting): String {
    val sort = stringResource(sorting.generalSorting.label())
    return sorting.timeSorting?.let { "$sort · ${stringResource(it.label())}" } ?: sort
}

/** Sort picker: shape-morphing toggle buttons, then a connected time range group. */
@Composable
fun SortSheet(
    current: Sorting,
    context: SortContext,
    onDismiss: () -> Unit,
    onSort: (Sorting) -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf(current.generalSorting) }
    fun apply(sorting: Sorting) {
        onSort(sorting)
        scope.launch { state.hide() }.invokeOnCompletion { onDismiss() }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp).navigationBarsPadding()) {
            Text(
                stringResource(R.string.sort_title),
                style = MaterialTheme.typography.headlineSmallEmphasized,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                context.sorts.forEach { sort ->
                    ToggleButton(
                        checked = pending == sort,
                        onCheckedChange = {
                            pending = sort
                            if (sort !in context.timed) apply(Sorting(sort))
                        },
                    ) { Text(stringResource(sort.label())) }
                }
            }
            AnimatedVisibility(pending in context.timed) {
                Column {
                    Text(
                        stringResource(R.string.sort_time_title),
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        modifier = Modifier.padding(top = 24.dp, bottom = 12.dp),
                    )
                    val times = TimeSorting.values()
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                    ) {
                        times.forEachIndexed { index, time ->
                            val selected = current.generalSorting == pending && current.timeSorting == time
                            ToggleButton(
                                checked = selected,
                                onCheckedChange = { apply(Sorting(pending, time)) },
                                shapes = when (index) {
                                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                    times.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                },
                            ) { Text(stringResource(time.label())) }
                        }
                    }
                }
            }
        }
    }
}

/** One row of an action sheet. */
data class SheetAction(val icon: ImageVector, val label: String, val onClick: () -> Unit)

/** Bottom sheet listing actions as an Expressive segmented list. */
@Composable
fun ActionSheet(
    title: String?,
    actions: List<SheetAction>,
    onDismiss: () -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state) {
        Column(
            Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            if (title != null) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 12.dp),
                )
            }
            actions.forEachIndexed { index, action ->
                SegmentedListItem(
                    onClick = {
                        scope.launch { state.hide() }.invokeOnCompletion {
                            onDismiss()
                            action.onClick()
                        }
                    },
                    shapes = ListItemDefaults.segmentedShapes(index, actions.size),
                    leadingContent = { Icon(action.icon, null) },
                ) { Text(action.label) }
            }
        }
    }
}

@Composable
fun PostActionSheet(post: PostEntity, onToggleSave: () -> Unit, onDismiss: () -> Unit) {
    val links = LocalLinkActions.current
    val postUrl = "https://www.reddit.com${post.permalink}"
    ActionSheet(
        title = post.title,
        actions = listOf(
            SheetAction(Icons.Rounded.Forum, stringResource(R.string.post_open_subreddit)) {
                links.openSubreddit(post.subreddit)
            },
            SheetAction(Icons.Rounded.AccountCircle, stringResource(R.string.post_open_user)) {
                links.openUser(post.author)
            },
            SheetAction(
                if (post.saved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                stringResource(if (post.saved) R.string.unsave else R.string.save),
                onToggleSave,
            ),
            SheetAction(Icons.AutoMirrored.Rounded.OpenInNew, stringResource(R.string.menu_open_link)) {
                links.openInOtherApp(post.url)
            },
            SheetAction(Icons.Rounded.Link, stringResource(R.string.menu_share_link)) {
                links.share(post.url)
            },
            SheetAction(Icons.Rounded.Share, stringResource(R.string.menu_share_post)) {
                links.share(postUrl, post.title)
            },
            SheetAction(Icons.Rounded.ContentCopy, stringResource(R.string.menu_copy_link)) {
                links.copy(postUrl)
            },
        ),
        onDismiss = onDismiss,
    )
}

@Composable
fun CommentActionSheet(
    comment: Comment.CommentEntity,
    onToggleSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val links = LocalLinkActions.current
    val url = "https://www.reddit.com${comment.permalink}"
    ActionSheet(
        title = "u/${comment.author}",
        actions = listOf(
            SheetAction(Icons.Rounded.AccountCircle, stringResource(R.string.post_open_user)) {
                links.openUser(comment.author)
            },
            SheetAction(Icons.Rounded.Forum, stringResource(R.string.post_open_subreddit)) {
                links.openSubreddit(comment.subreddit)
            },
            SheetAction(
                if (comment.saved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                stringResource(if (comment.saved) R.string.unsave else R.string.save),
                onToggleSave,
            ),
            SheetAction(Icons.AutoMirrored.Rounded.OpenInNew, stringResource(R.string.menu_open_link)) {
                links.openInOtherApp(url)
            },
            SheetAction(Icons.Rounded.Share, stringResource(R.string.menu_share_link)) {
                links.share(url)
            },
            SheetAction(Icons.Rounded.ContentCopy, stringResource(R.string.menu_copy_link)) {
                links.copy(url)
            },
        ),
        onDismiss = onDismiss,
    )
}
