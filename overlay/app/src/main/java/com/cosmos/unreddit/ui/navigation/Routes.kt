package com.cosmos.unreddit.ui.navigation

import android.net.Uri
import com.cosmos.unreddit.data.model.GalleryMedia
import com.cosmos.unreddit.data.model.MediaType
import com.cosmos.unreddit.data.model.db.PostEntity

/** String routes for the Compose navigation graph. */
object Routes {
    const val HOME_GRAPH = "home_graph"
    const val SUBSCRIPTIONS_GRAPH = "subscriptions_graph"
    const val PROFILE_GRAPH = "profile_graph"
    const val SETTINGS_GRAPH = "settings_graph"

    const val HOME = "home"
    const val SUBSCRIPTIONS = "subscriptions"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val ABOUT = "settings/about"
    const val PRIVACY_ENHANCER = "settings/privacy"
    const val BACKUP = "settings/backup"

    const val POST = "post?permalink={permalink}&thread={thread}"
    const val SUBREDDIT = "subreddit/{name}"
    const val SUBREDDIT_SEARCH = "subreddit/{name}/search"
    const val USER = "user/{name}"
    const val SEARCH = "search?query={query}"
    const val MEDIA = "media/{key}"

    val topLevel = setOf(HOME, SUBSCRIPTIONS, PROFILE, SETTINGS)

    fun post(permalink: String, singleThread: Boolean = false) =
        "post?permalink=${Uri.encode(permalink)}&thread=$singleThread"

    fun subreddit(name: String) = "subreddit/${Uri.encode(name.removePrefix("r/"))}"
    fun subredditSearch(name: String) = "subreddit/${Uri.encode(name.removePrefix("r/"))}/search"
    fun user(name: String) = "user/${Uri.encode(name.removePrefix("u/"))}"
    fun search(query: String = "") = "search?query=${Uri.encode(query)}"
    fun media(key: String) = "media/$key"
}

/**
 * Objects too large or rich for route arguments are handed to the next screen here:
 * the clicked post (so its header shows instantly) and media viewer requests.
 */
object NavHandoff {
    private val posts = mutableMapOf<String, PostEntity>()
    private val media = mutableMapOf<String, MediaRequest>()
    private var nextKey = 0

    fun putPost(post: PostEntity) {
        posts[post.permalink] = post
    }

    fun peekPost(permalink: String): PostEntity? = posts[permalink]

    fun putMedia(request: MediaRequest): String {
        val key = (nextKey++).toString()
        media[key] = request
        return key
    }

    fun getMedia(key: String): MediaRequest? = media[key]
}

sealed interface MediaRequest {
    data class Link(val link: String, val type: MediaType) : MediaRequest
    data class Gallery(val items: List<GalleryMedia>) : MediaRequest
}

/** Maps Reddit web links onto in-app routes, or returns null when the app can't show them. */
object RedditLinkRouter {
    private val hosts = Regex("""^(.+\.)?reddit\.com$""")

    fun route(link: String): String? {
        val uri = runCatching { Uri.parse(link) }.getOrNull() ?: return null
        val host = uri.host?.lowercase() ?: return relative(uri)
        val segments = uri.pathSegments.filter { it.isNotBlank() }
        if (host == "redd.it") {
            return segments.firstOrNull()?.let { Routes.post("/comments/$it") }
        }
        if (!hosts.matches(host)) return null
        return fromSegments(segments)
    }

    private fun relative(uri: Uri): String? =
        fromSegments(uri.pathSegments.filter { it.isNotBlank() })

    private fun fromSegments(segments: List<String>): String? {
        if (segments.isEmpty()) return null
        val first = segments[0].lowercase()
        return when {
            first == "gallery" && segments.size > 1 -> Routes.post("/comments/${segments[1]}")
            first == "comments" && segments.size > 1 -> Routes.post("/comments/${segments[1]}")
            (first == "r" || first == "u" || first == "user") &&
                segments.size >= 4 && segments[2] == "comments" -> {
                val base = "/${segments[0]}/${segments[1]}/comments/${segments[3]}"
                if (segments.size >= 6) {
                    Routes.post("$base/${segments[4]}/${segments[5]}", singleThread = true)
                } else {
                    Routes.post(base)
                }
            }
            first == "r" && segments.size >= 2 -> Routes.subreddit(segments[1])
            (first == "u" || first == "user") && segments.size >= 2 -> Routes.user(segments[1])
            else -> null
        }
    }
}
