package com.cosmos.unreddit.ui.common.compose

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.GalleryMedia
import com.cosmos.unreddit.data.model.MediaType
import com.cosmos.unreddit.data.model.db.Redirect
import com.cosmos.unreddit.ui.navigation.MediaRequest
import com.cosmos.unreddit.ui.navigation.NavHandoff
import com.cosmos.unreddit.ui.navigation.RedditLinkRouter
import com.cosmos.unreddit.ui.navigation.Routes
import com.cosmos.unreddit.util.LinkRedirector
import com.cosmos.unreddit.util.LinkUtil
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface LinkActionsEntryPoint {
    fun linkRedirector(): LinkRedirector
}

/**
 * Everything a screen does with a link: open it in the app when it is Reddit content or
 * media, otherwise in a custom tab (through the privacy enhancer's redirects), share it,
 * or copy it.
 */
class LinkActions(
    private val context: Context,
    private val navController: NavController,
) {
    private val redirector: LinkRedirector by lazy {
        EntryPointAccessors.fromApplication(
            context.applicationContext, LinkActionsEntryPoint::class.java
        ).linkRedirector()
    }

    /** Pending "redirect this link?" question, shown by [LinkRedirectDialog]. */
    val pendingRedirect: MutableState<LinkRedirector.LinkRedirect?> = mutableStateOf(null)

    fun navigate(route: String) = navController.navigate(route) { launchSingleTop = true }

    fun openSubreddit(name: String) = navigate(Routes.subreddit(name))
    fun openUser(name: String) = navigate(Routes.user(name))
    fun openPost(permalink: String, singleThread: Boolean = false) =
        navigate(Routes.post(permalink, singleThread))

    fun openMedia(link: String, type: MediaType) =
        navigate(Routes.media(NavHandoff.putMedia(MediaRequest.Link(link, type))))

    fun openGallery(items: List<GalleryMedia>) =
        navigate(Routes.media(NavHandoff.putMedia(MediaRequest.Gallery(items))))

    fun handleLink(link: String) {
        when (val type = LinkUtil.getLinkType(link)) {
            MediaType.REDDIT_SUBREDDIT -> openSubreddit(link.substringAfter("r/").trim('/'))
            MediaType.REDDIT_USER -> openUser(link.substringAfter("u/").trim('/'))
            MediaType.REDDIT_LINK -> openRedditLink(link)
            MediaType.REDDIT_PERMALINK -> openRedditLink("https://www.reddit.com$link")
            MediaType.IMGUR_ALBUM, MediaType.IMGUR_GALLERY, MediaType.IMGUR_GIF,
            MediaType.IMGUR_VIDEO, MediaType.IMGUR_IMAGE, MediaType.IMGUR_LINK,
            MediaType.GFYCAT, MediaType.REDGIFS, MediaType.STREAMABLE,
            MediaType.IMAGE, MediaType.VIDEO -> openMedia(link, type)
            else -> openExternal(link)
        }
    }

    fun openRedditLink(link: String) {
        val route = RedditLinkRouter.route(link)
        if (route != null) navigate(route) else openBrowser(link)
    }

    /** Opens a non-Reddit link, applying the privacy enhancer when it is on. */
    fun openExternal(link: String) {
        val redirect = if (redirector.isPrivacyEnhancerOn) redirector.getRedirectLink(link) else null
        when (redirect?.mode) {
            Redirect.RedirectMode.ON -> openBrowser(redirect.redirect.toString())
            Redirect.RedirectMode.ALWAYS_ASK -> pendingRedirect.value = redirect
            else -> openBrowser(link)
        }
    }

    fun openBrowser(link: String) {
        runCatching {
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setShareState(CustomTabsIntent.SHARE_STATE_ON)
                .build()
                .launchUrl(context, Uri.parse(link))
        }.onFailure { openInOtherApp(link) }
    }

    fun openInOtherApp(link: String) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(link)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun share(text: String, title: String? = null) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            title?.let { putExtra(Intent.EXTRA_TITLE, it) }
        }
        context.startActivity(
            Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun copy(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("link", text))
        Toast.makeText(context, R.string.toast_link_copied, Toast.LENGTH_SHORT).show()
    }
}

val LocalLinkActions = staticCompositionLocalOf<LinkActions> {
    error("LinkActions not provided")
}

/** The privacy enhancer's "open the redirected link?" question. */
@Composable
fun LinkRedirectDialog(actions: LinkActions) {
    val redirect = actions.pendingRedirect.value ?: return
    val dismiss = { actions.pendingRedirect.value = null }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(stringResource(R.string.dialog_privacy_enhancer_title)) },
        text = {
            Text(
                stringResource(
                    R.string.dialog_privacy_enhancer_message,
                    redirect.original.host,
                    redirect.redirect.host
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { dismiss(); actions.openBrowser(redirect.redirect.toString()) }) {
                Text(stringResource(R.string.dialog_yes))
            }
        },
        dismissButton = {
            TextButton(onClick = { dismiss(); actions.openBrowser(redirect.original.toString()) }) {
                Text(stringResource(R.string.dialog_no))
            }
        },
    )
}

