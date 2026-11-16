package com.cosmos.unreddit.ui.profile

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.Comment
import com.cosmos.unreddit.data.model.SavedItem
import com.cosmos.unreddit.ui.common.compose.CommentActionSheet
import com.cosmos.unreddit.ui.common.compose.EmptyState
import com.cosmos.unreddit.ui.common.compose.LocalLinkActions
import com.cosmos.unreddit.ui.common.compose.PostActionSheet
import com.cosmos.unreddit.ui.common.compose.PostCard
import com.cosmos.unreddit.ui.common.compose.RichText
import com.cosmos.unreddit.ui.common.compose.ShapedAvatar
import com.cosmos.unreddit.ui.common.compose.listPadding
import com.cosmos.unreddit.ui.common.compose.relativeTime
import com.cosmos.unreddit.ui.common.compose.rememberPostCallbacks
import com.cosmos.unreddit.ui.main.TopLevelDestination
import com.cosmos.unreddit.ui.main.TopLevelReselect
import com.cosmos.unreddit.ui.postlist.DefaultContentPreferences
import com.cosmos.unreddit.ui.profilemanager.ProfileManagerSheet

/** The current local profile and what it has saved. */
@Composable
fun ProfileScreen(viewModel: ProfileViewModel = hiltViewModel()) {
    val profile by viewModel.selectedProfile.collectAsStateWithLifecycle(initialValue = null)
    val items by viewModel.savedItems.collectAsStateWithLifecycle(initialValue = null)
    val preferences by viewModel.contentPreferences.collectAsStateWithLifecycle(DefaultContentPreferences)
    var tab by rememberSaveable { mutableStateOf(0) }
    var showManager by rememberSaveable { mutableStateOf(false) }
    var postMenu by remember { mutableStateOf<com.cosmos.unreddit.data.model.db.PostEntity?>(null) }
    var commentMenu by remember { mutableStateOf<Comment.CommentEntity?>(null) }
    val callbacks = rememberPostCallbacks(viewModel) { postMenu = it }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        TopLevelReselect.events.collect { if (it == TopLevelDestination.PROFILE) listState.animateScrollToItem(0) }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(profile?.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                subtitle = { Text(stringResource(R.string.tab_profile_saved)) },
                navigationIcon = {
                    profile?.let {
                        ShapedAvatar(
                            it.name, null, size = 40.dp,
                            shape = MaterialShapes.Cookie12Sided.toShape(),
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { showManager = true },
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.padding(end = 12.dp),
                    ) {
                        Icon(Icons.Rounded.ManageAccounts, null, Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                        Text(stringResource(R.string.action_switch_profile))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        val posts = items.orEmpty().filterIsInstance<SavedItem.Post>()
        val comments = items.orEmpty().filterIsInstance<SavedItem.Comment>()
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(top = padding.calculateTopPadding()).fillMaxSize(),
            contentPadding = listPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "tabs") {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                ) {
                    ToggleButton(
                        checked = tab == 0, onCheckedChange = { tab = 0 },
                        shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Article, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("${stringResource(R.string.label_saved_posts)} · ${posts.size}")
                    }
                    ToggleButton(
                        checked = tab == 1, onCheckedChange = { tab = 1 },
                        shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.ChatBubble, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("${stringResource(R.string.label_saved_comments)} · ${comments.size}")
                    }
                }
            }
            if (items != null && (if (tab == 0) posts else comments).isEmpty()) {
                item(key = "empty") { EmptyState(message = stringResource(R.string.empty_saved)) }
            }
            if (tab == 0) {
                items(posts, key = { "p" + it.post.id }) { item ->
                    PostCard(item.post, preferences, callbacks, Modifier.animateItem())
                }
            } else {
                items(comments, key = { "c" + it.comment.name }) { item ->
                    SavedCommentCard(item.comment, onMenu = { commentMenu = it }, modifier = Modifier.animateItem())
                }
            }
        }
    }

    if (showManager) ProfileManagerSheet(onDismiss = { showManager = false })
    postMenu?.let { post ->
        PostActionSheet(post, onToggleSave = { viewModel.toggleSavePost(post) }, onDismiss = { postMenu = null })
    }
    commentMenu?.let { comment ->
        CommentActionSheet(comment, onToggleSave = { viewModel.toggleSaveComment(comment) }, onDismiss = { commentMenu = null })
    }
}

/** A saved comment, opening its thread on tap. */
@Composable
fun SavedCommentCard(
    comment: Comment.CommentEntity,
    onMenu: (Comment.CommentEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val links = LocalLinkActions.current
    val open = {
        comment.linkPermalink?.let { links.openPost(comment.permalink, singleThread = true) }
            ?: links.openPost(comment.permalink, singleThread = true)
    }
    Card(
        onClick = open,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShapedAvatar(comment.subreddit, null, size = 28.dp)
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(
                        comment.linkTitle.orEmpty(),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${comment.subreddit} · u/${comment.author} · ${relativeTime(comment.created)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            RichText(
                comment.body,
                Modifier.fillMaxWidth().padding(top = 8.dp),
                fontSizeSp = 14.5f,
                onClick = open,
                onLongClick = { onMenu(comment) },
            )
        }
    }
}
