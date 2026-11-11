package com.cosmos.unreddit.ui.common.compose

import android.util.TypedValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.RedditText
import com.cosmos.unreddit.ui.common.widget.RedditView
import com.cosmos.unreddit.ui.main.LocalBottomBarPadding
import com.cosmos.unreddit.util.DateUtil
import kotlin.math.absoluteValue

/** Centered Expressive loading indicator for whole-screen loads. */
@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ContainedLoadingIndicator(Modifier.size(64.dp))
    }
}

/** Smaller loading indicator for list footers. */
@Composable
fun LoadingRow(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
        LoadingIndicator(Modifier.size(40.dp))
    }
}

@Composable
fun ErrorState(
    modifier: Modifier = Modifier,
    message: String = stringResource(R.string.network_retry_message),
    onRetry: (() -> Unit)? = null,
) {
    MessageState(
        icon = Icons.Rounded.CloudOff,
        message = message,
        modifier = modifier,
        action = onRetry?.let {
            {
                Button(onClick = it, shapes = ButtonDefaults.shapes()) {
                    Icon(Icons.Rounded.Refresh, null, Modifier.size(ButtonDefaults.IconSize))
                    Text(
                        stringResource(R.string.network_retry_action),
                        Modifier.padding(start = ButtonDefaults.IconSpacing)
                    )
                }
            }
        },
    )
}

@Composable
fun EmptyState(
    modifier: Modifier = Modifier,
    message: String = stringResource(R.string.empty_data),
    icon: ImageVector = Icons.Rounded.Inbox,
) = MessageState(icon, message, modifier)

@Composable
private fun MessageState(
    icon: ImageVector,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Surface(
            shape = MaterialShapes.Cookie9Sided.toShape(),
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(96.dp),
        ) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(40.dp)) }
        }
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        action?.invoke()
    }
}

@Composable
fun BackButton(onClick: () -> Unit) {
    IconButton(onClick = onClick, shapes = IconButtonDefaults.shapes()) {
        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back))
    }
}

/** Expressive avatar shapes, picked per name so each community keeps its own silhouette. */
private val avatarShapes = listOf(
    MaterialShapes.Cookie9Sided, MaterialShapes.Sunny, MaterialShapes.Clover4Leaf,
    MaterialShapes.SoftBurst, MaterialShapes.Cookie6Sided, MaterialShapes.Pentagon,
    MaterialShapes.Flower, MaterialShapes.Gem,
)

@Composable
fun rememberNameShape(name: String): Shape {
    val polygon = avatarShapes[name.lowercase().hashCode().absoluteValue % avatarShapes.size]
    return polygon.toShape()
}

/** Image avatar with a letter fallback, clipped to an Expressive shape. */
@Composable
fun ShapedAvatar(
    name: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    shape: Shape = rememberNameShape(name),
) {
    val letter = name.removePrefix("r/").removePrefix("u/").firstOrNull()?.uppercase() ?: "?"
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.tertiaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            letter,
            style = if (size >= 64.dp) MaterialTheme.typography.headlineMediumEmphasized
            else MaterialTheme.typography.titleMediumEmphasized,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Small tonal label such as NSFW, Spoiler or OC. */
@Composable
fun Tag(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.secondaryContainer,
    content: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    Text(
        text,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(container)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = content,
    )
}

/**
 * Reddit rich text (links, spoilers, tables, code, inline images and GIFs), drawn by the
 * existing span-based renderer inside Compose.
 */
@Composable
fun RichText(
    text: RedditText,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontSizeSp: Float = 15f,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val links = LocalLinkActions.current
    val argb = color.toArgb()
    AndroidView(
        modifier = modifier,
        factory = { context -> RedditView(context) },
        update = { view ->
            view.setOnLinkClickListener(object : RedditView.OnLinkClickListener {
                override fun onLinkClick(link: String) = links.handleLink(link)
                override fun onLinkLongClick(link: String) = links.copy(link)
            })
            if (view.tag !== text) {
                view.tag = text
                view.setText(text)
            }
            view.setTextColor(argb)
            view.applyTextSize(fontSizeSp)
            view.setOnClickListener(onClick?.let { action -> { _ -> action() } })
            view.setOnLongClickListener(onLongClick?.let { action -> { _ -> action(); true } })
            if (onClick == null) view.isClickable = false
        },
    )
}

private fun android.view.ViewGroup.applyTextSize(sp: Float) {
    for (i in 0 until childCount) {
        when (val child = getChildAt(i)) {
            is android.widget.TextView -> child.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
            is android.view.ViewGroup -> child.applyTextSize(sp)
        }
    }
}

@Composable
fun relativeTime(millis: Long): String {
    val context = LocalContext.current
    return remember(millis) { DateUtil.getTimeDifference(context, millis) }
}

/** Standard list padding that clears the bottom navigation bar on top-level screens. */
@Composable
fun listPadding(top: Dp = 8.dp, extraBottom: Dp = 0.dp): PaddingValues =
    PaddingValues(
        start = 12.dp,
        end = 12.dp,
        top = top,
        bottom = LocalBottomBarPadding.current + extraBottom + 16.dp,
    )

/** The application object, reachable from any Compose context. */
val android.content.Context.stealthApp: com.cosmos.unreddit.UnredditApplication
    get() = applicationContext as com.cosmos.unreddit.UnredditApplication
