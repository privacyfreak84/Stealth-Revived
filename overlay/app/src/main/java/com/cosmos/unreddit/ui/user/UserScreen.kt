package com.cosmos.unreddit.ui.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.Comment
import com.cosmos.unreddit.data.model.Resource
import com.cosmos.unreddit.data.model.User
import com.cosmos.unreddit.ui.common.compose.BackButton
import com.cosmos.unreddit.ui.common.compose.CommentActionSheet
import com.cosmos.unreddit.ui.common.compose.EmptyState
import com.cosmos.unreddit.ui.common.compose.ErrorState
import com.cosmos.unreddit.ui.common.compose.LoadingRow
import com.cosmos.unreddit.ui.common.compose.PostFeed
import com.cosmos.unreddit.ui.common.compose.ShapedAvatar
import com.cosmos.unreddit.ui.common.compose.SortContext
import com.cosmos.unreddit.ui.common.compose.SortSheet
import com.cosmos.unreddit.ui.common.compose.listPadding
import com.cosmos.unreddit.ui.common.compose.sortingLabel
import com.cosmos.unreddit.ui.postlist.DefaultContentPreferences
import com.cosmos.unreddit.ui.profile.SavedCommentCard

@Composable
fun UserScreen(name: String, onBack: () -> Unit, viewModel: UserViewModel = hiltViewModel()) {
    LaunchedEffect(name) { viewModel.setUser(name.removeSuffix("/")) }
    val user by viewModel.user.collectAsStateWithLifecycle()
    LaunchedEffect(user) { if (user.isNotBlank()) viewModel.loadUserInfo(false) }

    val about by viewModel.about.collectAsStateWithLifecycle()
    val sorting by viewModel.sorting.collectAsStateWithLifecycle()
    val page by viewModel.page.collectAsStateWithLifecycle()
    val preferences by viewModel.contentPreferences.collectAsStateWithLifecycle(DefaultContentPreferences)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showSort by rememberSaveable { mutableStateOf(false) }
    var commentMenu by remember { mutableStateOf<Comment.CommentEntity?>(null) }
    val info = (about as? Resource.Success)?.data

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("u/$name", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                subtitle = { Text(sortingLabel(sorting)) },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    IconButton(onClick = { showSort = true }, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.AutoMirrored.Rounded.Sort, stringResource(R.string.sort_title))
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
                    if (error.code == 404) R.string.dialog_user_not_found_body
                    else R.string.dialog_user_unauthorized_body
                ),
            )
            return@Scaffold
        }
        val modifier = Modifier.padding(top = padding.calculateTopPadding())
        val header: androidx.compose.foundation.lazy.LazyListScope.() -> Unit = {
            item(key = "header") {
                Column {
                    UserHeader(name, info)
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                    ) {
                        ToggleButton(
                            checked = page == 0, onCheckedChange = { viewModel.setPage(0) },
                            shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Article, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.tab_user_submitted))
                        }
                        ToggleButton(
                            checked = page == 1, onCheckedChange = { viewModel.setPage(1) },
                            shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Rounded.ChatBubble, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.tab_user_comments))
                        }
                    }
                }
            }
        }
        if (page == 0) {
            PostFeed(
                posts = viewModel.postDataFlow.collectAsLazyPagingItems(),
                preferences = preferences,
                viewModel = viewModel,
                modifier = modifier,
                header = header,
            )
        } else {
            val comments = viewModel.commentDataFlow.collectAsLazyPagingItems()
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = listPadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                header()
                when (val refresh = comments.loadState.refresh) {
                    is LoadState.Loading -> item { LoadingRow(Modifier.padding(top = 48.dp)) }
                    is LoadState.Error -> item {
                        ErrorState(message = refresh.error.message ?: stringResource(R.string.network_retry_message), onRetry = comments::retry)
                    }
                    is LoadState.NotLoading -> if (comments.itemCount == 0) item { EmptyState() }
                }
                items(comments.itemCount) { index ->
                    (comments[index] as? Comment.CommentEntity)?.let { comment ->
                        SavedCommentCard(comment, onMenu = { commentMenu = it })
                    }
                }
                if (comments.loadState.append is LoadState.Loading) item { LoadingRow() }
            }
        }
    }

    if (showSort) SortSheet(sorting, SortContext.USER, onDismiss = { showSort = false }, onSort = viewModel::setSorting)
    commentMenu?.let { comment ->
        CommentActionSheet(comment, onToggleSave = { viewModel.toggleSaveComment(comment) }, onDismiss = { commentMenu = null })
    }
}

@Composable
private fun UserHeader(name: String, info: User?) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            ShapedAvatar(name, info?.icon, size = 72.dp, shape = MaterialShapes.Cookie12Sided.toShape())
            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                Text(info?.title?.takeIf { it.isNotBlank() } ?: "u/$name", style = MaterialTheme.typography.titleLargeEmphasized)
                if (info != null) {
                    Stat(stringResource(R.string.user_post_karma), info.getPostKarmaCount())
                    Stat(stringResource(R.string.user_comment_karma), info.getCommentKarmaCount())
                    Stat(stringResource(R.string.user_cake_day), info.getCakeDay())
                }
            }
        }
        info?.publicDescription?.takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            )
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Row(Modifier.padding(top = 2.dp)) {
        Text(value, style = MaterialTheme.typography.labelLargeEmphasized, color = MaterialTheme.colorScheme.primary)
        Text(" $label", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
