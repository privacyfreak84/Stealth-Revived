package com.cosmos.unreddit.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.User
import com.cosmos.unreddit.data.model.db.SubredditEntity
import com.cosmos.unreddit.ui.common.compose.BackButton
import com.cosmos.unreddit.ui.common.compose.EmptyState
import com.cosmos.unreddit.ui.common.compose.ErrorState
import com.cosmos.unreddit.ui.common.compose.LoadingRow
import com.cosmos.unreddit.ui.common.compose.LocalLinkActions
import com.cosmos.unreddit.ui.common.compose.PostFeed
import com.cosmos.unreddit.ui.common.compose.ShapedAvatar
import com.cosmos.unreddit.ui.common.compose.SortContext
import com.cosmos.unreddit.ui.common.compose.SortSheet
import com.cosmos.unreddit.ui.common.compose.listPadding
import com.cosmos.unreddit.ui.postlist.DefaultContentPreferences

/** Pill-shaped search field used by the search screens. */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    autoFocus: Boolean = false,
) {
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(autoFocus) { if (autoFocus) focus.requestFocus() }
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { Icon(Icons.Rounded.Search, null) },
        trailingIcon = if (value.isNotEmpty()) {
            { IconButton(onClick = { onValueChange("") }) { Icon(Icons.Rounded.Clear, null) } }
        } else null,
        singleLine = true,
        shape = MaterialTheme.shapes.extraLargeIncreased,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); onSearch() }),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        modifier = modifier.focusRequester(focus),
    )
}

private enum class SearchTab(val label: Int) {
    POSTS(R.string.tab_search_post), SUBREDDITS(R.string.tab_search_subreddit), USERS(R.string.tab_search_user)
}

@Composable
fun SearchScreen(initialQuery: String, onBack: () -> Unit, viewModel: SearchViewModel = hiltViewModel()) {
    var text by rememberSaveable { mutableStateOf(initialQuery) }
    val query by viewModel.query.collectAsStateWithLifecycle()
    val sorting by viewModel.sorting.collectAsStateWithLifecycle()
    val preferences by viewModel.contentPreferences.collectAsStateWithLifecycle(DefaultContentPreferences)
    var tab by rememberSaveable { mutableStateOf(SearchTab.POSTS) }
    var showSort by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank() && viewModel.query.value.isBlank()) viewModel.setQuery(initialQuery)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Column(Modifier.statusBarsPadding().padding(bottom = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                    BackButton(onBack)
                    SearchField(
                        value = text,
                        onValueChange = { text = it },
                        onSearch = { if (text.isNotBlank()) viewModel.setQuery(text.trim()) },
                        placeholder = stringResource(R.string.search_hint),
                        autoFocus = initialQuery.isBlank(),
                        modifier = Modifier.weight(1f),
                    )
                    if (tab == SearchTab.POSTS) {
                        IconButton(onClick = { showSort = true }, shapes = IconButtonDefaults.shapes()) {
                            Icon(Icons.AutoMirrored.Rounded.Sort, stringResource(R.string.sort_title))
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                ) {
                    val tabs = SearchTab.entries
                    tabs.forEachIndexed { index, item ->
                        ToggleButton(
                            checked = tab == item,
                            onCheckedChange = { tab = item },
                            shapes = when (index) {
                                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                tabs.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            },
                            modifier = Modifier.weight(1f),
                        ) { Text(stringResource(item.label), maxLines = 1) }
                    }
                }
            }
        },
    ) { padding ->
        val modifier = Modifier.padding(top = padding.calculateTopPadding()).fillMaxSize()
        if (query.isBlank()) {
            EmptyState(modifier.padding(top = 64.dp), message = stringResource(R.string.search_empty_hint), icon = Icons.Rounded.Search)
            return@Scaffold
        }
        when (tab) {
            SearchTab.POSTS -> PostFeed(
                posts = viewModel.postDataFlow.collectAsLazyPagingItems(),
                preferences = preferences,
                viewModel = viewModel,
                modifier = modifier,
            )
            SearchTab.SUBREDDITS -> {
                val items = viewModel.subredditDataFlow.collectAsLazyPagingItems()
                PagedList(items, modifier) { index, subreddit -> SubredditRow(subreddit, index, items.itemCount) }
            }
            SearchTab.USERS -> {
                val items = viewModel.userDataFlow.collectAsLazyPagingItems()
                PagedList(items, modifier) { index, user -> UserRow(user, index, items.itemCount) }
            }
        }
    }

    if (showSort) SortSheet(sorting, SortContext.SEARCH, onDismiss = { showSort = false }, onSort = viewModel::setSorting)
}

@Composable
private fun <T : Any> PagedList(
    items: LazyPagingItems<T>,
    modifier: Modifier,
    row: @Composable (Int, T) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = listPadding(),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        when (val refresh = items.loadState.refresh) {
            is LoadState.Loading -> item { LoadingRow(Modifier.padding(top = 64.dp)) }
            is LoadState.Error -> item {
                ErrorState(message = refresh.error.message ?: stringResource(R.string.network_retry_message), onRetry = items::retry)
            }
            is LoadState.NotLoading -> if (items.itemCount == 0) item { EmptyState() }
        }
        items(items.itemCount) { index -> items[index]?.let { row(index, it) } }
        if (items.loadState.append is LoadState.Loading) item { LoadingRow() }
    }
}

@Composable
fun SubredditRow(subreddit: SubredditEntity, index: Int, count: Int) {
    val links = LocalLinkActions.current
    SegmentedListItem(
        onClick = { links.openSubreddit(subreddit.displayName) },
        shapes = ListItemDefaults.segmentedShapes(index, count),
        leadingContent = { ShapedAvatar(subreddit.displayName, subreddit.icon) },
        supportingContent = {
            Text(
                listOf(subreddit.getSubscribersCount(), subreddit.title).filter { it.isNotBlank() }.joinToString(" · "),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
    ) { Text("r/${subreddit.displayName}", style = MaterialTheme.typography.titleMedium) }
}

@Composable
fun UserRow(user: User, index: Int, count: Int) {
    val links = LocalLinkActions.current
    SegmentedListItem(
        onClick = { links.openUser(user.displayName) },
        shapes = ListItemDefaults.segmentedShapes(index, count),
        leadingContent = { ShapedAvatar(user.displayName, user.icon) },
        supportingContent = {
            Text(stringResource(R.string.user_karma, user.getPostKarmaCount()), maxLines = 1)
        },
    ) { Text("u/${user.displayName}", style = MaterialTheme.typography.titleMedium) }
}
