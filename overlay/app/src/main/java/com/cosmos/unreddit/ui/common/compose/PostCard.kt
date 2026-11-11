package com.cosmos.unreddit.ui.common.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Comment
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.MediaType
import com.cosmos.unreddit.data.model.PostType
import com.cosmos.unreddit.data.model.db.PostEntity
import com.cosmos.unreddit.data.model.preferences.ContentPreferences

/** Callbacks a post card can raise. */
interface PostCallbacks {
    fun onOpen(post: PostEntity)
    fun onMedia(post: PostEntity)
    fun onLink(post: PostEntity)
    fun onSubreddit(post: PostEntity)
    fun onUser(post: PostEntity)
    fun onToggleSave(post: PostEntity)
    fun onMenu(post: PostEntity)
}

/**
 * A feed post: Expressive card with a shaped community avatar, title, media or text
 * preview, and a connected action group (score, comments, save, more).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostCard(
    post: PostEntity,
    preferences: ContentPreferences,
    callbacks: PostCallbacks,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
) {
    val seen = post.seen && !expanded
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .combinedClickable(
                onClick = { callbacks.onOpen(post) },
                onLongClick = { callbacks.onMenu(post) },
            ),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = if (expanded) MaterialTheme.colorScheme.surfaceContainerLow
            else MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(Modifier.padding(top = 14.dp, bottom = 10.dp)) {
            PostHeader(post, callbacks, Modifier.padding(horizontal = 16.dp))
            Text(
                post.title,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                style = if (expanded) MaterialTheme.typography.titleLargeEmphasized
                else MaterialTheme.typography.titleMedium,
                color = if (seen) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
            )
            PostFlags(post, Modifier.padding(horizontal = 16.dp))
            PostContent(post, preferences, callbacks, expanded)
            PostActions(post, callbacks, Modifier.padding(horizontal = 12.dp))
        }
    }
}

@Composable
private fun PostHeader(post: PostEntity, callbacks: PostCallbacks, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        ShapedAvatar(
            name = post.subreddit,
            imageUrl = null,
            size = 36.dp,
            modifier = Modifier.clickable { callbacks.onSubreddit(post) },
        )
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(
                post.subreddit,
                style = MaterialTheme.typography.labelLargeEmphasized,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { callbacks.onSubreddit(post) },
            )
            Text(
                "u/${post.author} · ${relativeTime(post.created)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { callbacks.onUser(post) },
            )
        }
        if (post.isStickied) {
            Icon(
                Icons.Rounded.PushPin,
                stringResource(R.string.label_pinned),
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun PostFlags(post: PostEntity, modifier: Modifier) {
    val tags = buildList {
        if (post.isOver18) add(stringResource(R.string.flair_nsfw) to true)
        if (post.isSpoiler) add(stringResource(R.string.flair_spoiler) to true)
        if (post.isOC) add(stringResource(R.string.flair_oc) to false)
        if (post.isLocked) add(stringResource(R.string.label_locked) to false)
        if (post.isArchived) add(stringResource(R.string.label_archived) to false)
    }
    if (tags.isEmpty()) return
    FlowRow(
        modifier.padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        tags.forEach { (label, warning) ->
            if (warning) {
                Tag(
                    label,
                    container = MaterialTheme.colorScheme.errorContainer,
                    content = MaterialTheme.colorScheme.onErrorContainer,
                )
            } else {
                Tag(label)
            }
        }
    }
}

@Composable
private fun PostContent(
    post: PostEntity,
    preferences: ContentPreferences,
    callbacks: PostCallbacks,
    expanded: Boolean,
) {
    when (post.type) {
        PostType.IMAGE, PostType.VIDEO -> MediaPreview(post, preferences, callbacks)
        PostType.LINK -> LinkPreview(post, preferences, callbacks)
        PostType.TEXT -> if (!expanded) {
            val preview = post.previewText?.toString()?.trim()
            if (!preview.isNullOrEmpty()) {
                Text(
                    preview,
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun MediaPreview(post: PostEntity, preferences: ContentPreferences, callbacks: PostCallbacks) {
    val image = post.preview ?: return
    var revealed by rememberSaveable(post.id) { mutableStateOf(post.shouldShowPreview(preferences)) }
    Box(
        Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 8.dp)
            .fillMaxWidth()
            .heightIn(max = 420.dp)
            .aspectRatio(4f / 3f)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable { if (revealed) callbacks.onMedia(post) else revealed = true },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = image,
            contentDescription = post.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().then(if (revealed) Modifier else Modifier.blur(32.dp)),
        )
        if (!revealed) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.85f),
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.VisibilityOff, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_reveal), style = MaterialTheme.typography.labelLarge)
                }
            }
        } else if (post.type == PostType.VIDEO || post.mediaType.isVideo()) {
            Surface(
                shape = MaterialShapes.Cookie9Sided.toShape(),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(64.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.PlayArrow, stringResource(R.string.action_play), Modifier.size(32.dp))
                }
            }
        }
        if (revealed && post.gallery.size > 1) {
            Tag(
                stringResource(R.string.label_gallery_count, post.gallery.size),
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                container = Color.Black.copy(alpha = 0.6f),
                content = Color.White,
            )
            Icon(
                Icons.Rounded.Collections, null, tint = Color.White,
                modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp),
            )
        }
    }
}

private fun MediaType.isVideo(): Boolean = this in setOf(
    MediaType.REDDIT_VIDEO, MediaType.REDDIT_GIF, MediaType.IMGUR_VIDEO, MediaType.IMGUR_GIF,
    MediaType.GFYCAT, MediaType.REDGIFS, MediaType.STREAMABLE, MediaType.VIDEO,
)

@Composable
private fun LinkPreview(post: PostEntity, preferences: ContentPreferences, callbacks: PostCallbacks) {
    Surface(
        onClick = { callbacks.onLink(post) },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 8.dp).fillMaxWidth(),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            val showImage = post.preview != null && post.shouldShowPreview(preferences)
            if (showImage) {
                AsyncImage(
                    model = post.preview,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(64.dp).clip(MaterialTheme.shapes.medium),
                )
            } else {
                Surface(
                    shape = MaterialShapes.Clover4Leaf.toShape(),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(48.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Link, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    post.domain,
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    post.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Score, comments, save and overflow as a connected Expressive button group. */
@Composable
private fun PostActions(post: PostEntity, callbacks: PostCallbacks, modifier: Modifier) {
    var saved by remember(post.id) { mutableStateOf(post.saved) }
    Row(
        modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        ToggleButton(
            checked = false,
            onCheckedChange = { callbacks.onOpen(post) },
            shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
            colors = ToggleButtonDefaults.tonalToggleButtonColors(),
        ) {
            Icon(Icons.Rounded.ArrowUpward, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(post.score, style = MaterialTheme.typography.labelLarge)
        }
        ToggleButton(
            checked = false,
            onCheckedChange = { callbacks.onOpen(post) },
            shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
            colors = ToggleButtonDefaults.tonalToggleButtonColors(),
        ) {
            Icon(Icons.AutoMirrored.Rounded.Comment, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(post.commentsNumber, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.weight(1f))
        val scale by animateFloatAsState(
            if (saved) 1.1f else 1f, MaterialTheme.motionScheme.fastSpatialSpec(), label = "saveScale"
        )
        FilledTonalIconToggleButton(
            checked = saved,
            onCheckedChange = {
                callbacks.onToggleSave(post)
                post.saved = !post.saved
                saved = post.saved
            },
            shapes = IconButtonDefaults.toggleableShapes(),
            modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        ) {
            AnimatedContent(saved, label = "saveIcon") { isSaved ->
                Icon(
                    if (isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    stringResource(if (isSaved) R.string.unsave else R.string.save),
                )
            }
        }
        IconButton(onClick = { callbacks.onMenu(post) }, shapes = IconButtonDefaults.shapes()) {
            Icon(Icons.Rounded.MoreVert, stringResource(R.string.action_more))
        }
    }
}
