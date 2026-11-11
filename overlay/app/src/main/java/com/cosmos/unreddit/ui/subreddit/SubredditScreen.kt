package com.cosmos.unreddit.ui.subreddit

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.Resource
import com.cosmos.unreddit.data.model.db.SubredditEntity
import com.cosmos.unreddit.ui.common.compose.BackButton
import com.cosmos.unreddit.ui.common.compose.ErrorState
import com.cosmos.unreddit.ui.common.compose.LocalLinkActions
import com.cosmos.unreddit.ui.common.compose.PostFeed
import com.cosmos.unreddit.ui.common.compose.RichText
import com.cosmos.unreddit.ui.common.compose.ShapedAvatar
import com.cosmos.unreddit.ui.common.compose.SortContext
import com.cosmos.unreddit.ui.common.compose.SortSheet
import com.cosmos.unreddit.ui.common.compose.Tag
import com.cosmos.unreddit.ui.common.compose.sortingLabel
import com.cosmos.unreddit.ui.navigation.Routes
import com.cosmos.unreddit.ui.postlist.DefaultContentPreferences

@Composable
fun SubredditScreen(name: String, onBack: () -> Unit, viewModel: SubredditViewModel = hiltViewModel()) {
    val links = LocalLinkActions.current
    LaunchedEffect(name) { viewModel.setSubreddit(name.removeSuffix("/")) }
    val subreddit by viewModel.subreddit.collectAsStateWithLifecycle()
    LaunchedEffect(subreddit) { if (subreddit.isNotBlank()) viewModel.loadSubredditInfo(false) }

    val about by viewModel.about.collectAsStateWithLifecycle()
    val subscribed by viewModel.isSubscribed.collectAsStateWithLifecycle()
    val sorting by viewModel.sorting.collectAsStateWithLifecycle()
    val preferences by viewModel.contentPreferences.collectAsStateWithLifecycle(DefaultContentPreferences)
    val posts = viewModel.postDataFlow.collectAsLazyPagingItems()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showSort by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    val info = (about as? Resource.Success)?.data

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("r/$name", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                subtitle = {
                    Text(
                        info?.let {
                            "${it.getSubscribersCount()} ${stringResource(R.string.subreddit_subscribers).lowercase()} · " +
                                "${it.getActiveUsers()} ${stringResource(R.string.subreddit_active_users).lowercase()}"
                        } ?: sortingLabel(sorting)
                    )
                },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    IconButton(onClick = { links.navigate(Routes.subredditSearch(name)) }, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.Rounded.Search, stringResource(R.string.search_hint_subreddit, name))
                    }
                    IconButton(onClick = { showSort = true }, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.AutoMirrored.Rounded.Sort, stringResource(R.string.sort_title))
                    }
                    IconButton(onClick = { showAbout = true }, enabled = info != null, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.Rounded.Info, stringResource(R.string.action_show_description))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        val error = about as? Resource.Error
        if (error != null && (error.code == 403 || error.code == 404)) {
            ErrorState(
                Modifier.padding(padding),
                message = stringResource(
                    if (error.code == 404) R.string.dialog_subreddit_not_found_body
                    else R.string.dialog_subreddit_unauthorized_body
                ),
            )
            return@Scaffold
        }
        PostFeed(
            posts = posts,
            preferences = preferences,
            viewModel = viewModel,
            modifier = Modifier.padding(top = padding.calculateTopPadding()),
            header = {
                item(key = "header") {
                    SubredditHeader(name, info, subscribed, onToggle = viewModel::toggleSubscription)
                }
            },
        )
    }

    if (showSort) SortSheet(sorting, SortContext.FEED, onDismiss = { showSort = false }, onSort = viewModel::setSorting)
    if (showAbout && info != null) {
        ModalBottomSheet(onDismissRequest = { showAbout = false }, sheetState = rememberModalBottomSheetState(false)) {
            Column(
                Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(info.title, style = MaterialTheme.typography.headlineSmallEmphasized)
                Spacer(Modifier.height(12.dp))
                RichText(info.description, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun SubredditHeader(
    name: String,
    info: SubredditEntity?,
    subscribed: Boolean,
    onToggle: () -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
    ) {
        Box {
            if (!info?.header.isNullOrBlank()) {
                AsyncImage(
                    model = info?.header,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(96.dp).clip(MaterialTheme.shapes.extraLarge),
                )
            }
            Column(Modifier.padding(16.dp).padding(top = if (info?.header.isNullOrBlank()) 0.dp else 56.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ShapedAvatar(name, info?.icon, size = 64.dp)
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        Text(info?.title ?: "r/$name", style = MaterialTheme.typography.titleMediumEmphasized, maxLines = 2)
                        if (info?.over18 == true) {
                            Tag(
                                stringResource(R.string.flair_nsfw),
                                container = MaterialTheme.colorScheme.errorContainer,
                                content = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                    ToggleButton(
                        checked = subscribed,
                        onCheckedChange = { onToggle() },
                        colors = ToggleButtonDefaults.toggleButtonColors(),
                    ) {
                        AnimatedContent(subscribed, label = "subscribe") { isSubscribed ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (isSubscribed) Icons.Rounded.Check else Icons.Rounded.Add, null,
                                    Modifier.size(ButtonDefaults.IconSize),
                                )
                                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                                Text(
                                    stringResource(
                                        if (isSubscribed) R.string.subreddit_button_unsubscribe
                                        else R.string.subreddit_button_subscribe
                                    )
                                )
                            }
                        }
                    }
                }
                if (info != null && info.publicDescription.isNotEmpty()) {
                    RichText(
                        info.publicDescription,
                        Modifier.fillMaxWidth().padding(top = 12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSizeSp = 14f,
                    )
                }
            }
        }
    }
}

@Composable
fun SubredditSearchScreen(name: String, onBack: () -> Unit, viewModel: SubredditSearchViewModel = hiltViewModel()) {
    LaunchedEffect(name) { viewModel.setSubreddit(name) }
    val query by viewModel.query.collectAsStateWithLifecycle()
    val sorting by viewModel.sorting.collectAsStateWithLifecycle()
    val preferences by viewModel.contentPreferences.collectAsStateWithLifecycle(DefaultContentPreferences)
    var text by rememberSaveable { mutableStateOf("") }
    var showSort by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(end = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BackButton(onBack)
                com.cosmos.unreddit.ui.search.SearchField(
                    value = text,
                    onValueChange = { text = it },
                    onSearch = { if (text.isNotBlank()) viewModel.setQuery(text.trim()) },
                    placeholder = stringResource(R.string.search_hint_subreddit, "r/$name"),
                    autoFocus = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { showSort = true }, shapes = IconButtonDefaults.shapes()) {
                    Icon(Icons.AutoMirrored.Rounded.Sort, stringResource(R.string.sort_title))
                }
            }
        },
    ) { padding ->
        if (query.isBlank()) {
            com.cosmos.unreddit.ui.common.compose.EmptyState(
                Modifier.padding(padding).padding(top = 64.dp),
                message = stringResource(R.string.search_hint_subreddit, "r/$name"),
                icon = Icons.Rounded.Search,
            )
        } else {
            PostFeed(
                posts = viewModel.postDataFlow.collectAsLazyPagingItems(),
                preferences = preferences,
                viewModel = viewModel,
                modifier = Modifier.padding(top = padding.calculateTopPadding()),
            )
        }
    }
    if (showSort) SortSheet(sorting, SortContext.SEARCH, onDismiss = { showSort = false }, onSort = viewModel::setSorting)
}
