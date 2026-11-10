package com.cosmos.unreddit.data.remote.api.reddit.source

import android.util.Base64
import android.util.Log
import com.cosmos.unreddit.data.model.Sort
import com.cosmos.unreddit.data.model.TimeSorting
import com.cosmos.unreddit.data.remote.api.reddit.RedditOAuthManager
import com.cosmos.unreddit.data.remote.api.reddit.model.AboutChildJsonAdapter
import com.cosmos.unreddit.data.remote.api.reddit.model.AboutUserChildJsonAdapter
import com.cosmos.unreddit.data.remote.api.reddit.model.Listing
import com.cosmos.unreddit.data.remote.api.reddit.model.ListingJsonAdapter
import com.cosmos.unreddit.data.remote.api.reddit.model.MoreChildren
import com.cosmos.unreddit.data.remote.api.reddit.model.MoreChildrenJsonAdapter
import com.cosmos.unreddit.di.DispatchersModule.IoDispatcher
import com.cosmos.unreddit.di.NetworkModule.GenericOkHttp
import com.cosmos.unreddit.di.NetworkModule.RedditMoshi
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Uses Reddit's persisted Android GraphQL operations for feeds, search and comments,
 * adapting responses to Stealth's existing models. The old JSON source remains a fallback.
 */
@Singleton
class GraphQlRedditSource @Inject constructor(
    private val fallback: RedditSource,
    @RedditMoshi moshi: Moshi,
    @GenericOkHttp private val client: OkHttpClient,
    private val oauthManager: RedditOAuthManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : BaseRedditSource by fallback {

    private val listingAdapter = ListingJsonAdapter(moshi)
    private val moreChildrenAdapter = MoreChildrenJsonAdapter(moshi)

    override suspend fun getSubreddit(
        subreddit: String,
        sort: Sort,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing = withFallback(
        "subreddit feed",
        primary = {
            withContext(ioDispatcher) {
                val variables = JSONObject()
                    .put("after", after ?: JSONObject.NULL)
                    .put("includeSubredditInPosts", true)
                    .put("pageSize", PAGE_SIZE)
                    .put("sort", feedSort(sort))
                    .put("subredditName", subreddit)
                feedRange(sort, timeSorting)?.let { variables.put("range", it) }

                val data = gql("SubredditFeedElements", SUBREDDIT_FEED_HASH, variables)
                val connection = data.optJSONObject("postFeed")
                    ?.optJSONObject("elements")
                    ?: throw IOException("Reddit omitted the subreddit feed")
                postListing(connection, unwrapPostInfo = true)
            }
        },
        backup = { fallback.getSubreddit(subreddit, sort, timeSorting, after) }
    )

    override suspend fun searchInSubreddit(
        subreddit: String,
        query: String,
        sort: Sort?,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing = searchPosts(query + " subreddit:" + subreddit, sort, timeSorting, after)

    override suspend fun searchPost(
        query: String,
        sort: Sort?,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing = searchPosts(query, sort, timeSorting, after)

    override suspend fun searchSubreddit(
        query: String,
        sort: Sort?,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing = withFallback(
        "subreddit search",
        primary = {
            withContext(ioDispatcher) {
                val variables = JSONObject()
                    .put("afterCursor", after ?: JSONObject.NULL)
                    .put("filters", safeSearchFilters())
                    .put("pageSize", PAGE_SIZE)
                    .put("productSurface", "")
                    .put("query", query)
                val data = gql("SearchCommunities", SEARCH_COMMUNITIES_HASH, variables)
                val connection = data.optJSONObject("search")
                    ?.optJSONObject("general")
                    ?.optJSONObject("communities")
                    ?: throw IOException("Reddit omitted subreddit search results")
                val edges = connection.optJSONArray("edges") ?: JSONArray()
                val children = JSONArray()
                for (index in 0 until edges.length()) {
                    val node = edges.optJSONObject(index)?.optJSONObject("node") ?: continue
                    children.put(aboutChild(node))
                }
                listing(children, pageCursor(connection))
            }
        },
        backup = { fallback.searchSubreddit(query, sort, timeSorting, after) }
    )

    override suspend fun searchUser(
        query: String,
        sort: Sort?,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing = withFallback(
        "people search",
        primary = {
            withContext(ioDispatcher) {
                val variables = JSONObject()
                    .put("afterCursor", after ?: JSONObject.NULL)
                    .put("filters", safeSearchFilters())
                    .put("pageSize", PAGE_SIZE)
                    .put("productSurface", "")
                    .put("query", query)
                val data = gql("SearchPeople", SEARCH_USERS_HASH, variables)
                val connection = data.optJSONObject("search")
                    ?.optJSONObject("general")
                    ?.optJSONObject("authors")
                    ?: throw IOException("Reddit omitted people search results")
                val edges = connection.optJSONArray("edges") ?: JSONArray()
                val children = JSONArray()
                for (index in 0 until edges.length()) {
                    val node = edges.optJSONObject(index)?.optJSONObject("node") ?: continue
                    children.put(aboutUserChild(node))
                }
                listing(children, pageCursor(connection))
            }
        },
        backup = { fallback.searchUser(query, sort, timeSorting, after) }
    )

    override suspend fun getPost(
        permalink: String,
        limit: Int?,
        sort: Sort
    ): List<Listing> = withFallback(
        "post and comments",
        primary = {
            withContext(ioDispatcher) {
                val reference = parsePostReference(permalink)
                val postJson = requestJson(
                    OAUTH_BASE + "/by_id/t3_" + reference.postId + ".json?raw_json=1"
                )
                val postListing = listingAdapter.fromJson(postJson.toString())
                    ?: throw IOException("Reddit returned an unreadable post")
                val variables = commentsVariables(
                    reference.postId, sort, reference.commentId, permalink
                )
                val data = gql("PostComments", POST_COMMENTS_HASH, variables)
                listOf(postListing, listing(commentTrees(data, reference.postId), null))
            }
        },
        backup = { fallback.getPost(permalink, limit, sort) }
    )

    override suspend fun getMoreChildren(children: String, linkId: String): MoreChildren {
        val token = children.split(',').singleOrNull()?.trim()
            ?: return fallback.getMoreChildren(children, linkId)
        val cursor = decodeCursor(token)
            ?: return fallback.getMoreChildren(children, linkId)

        return withFallback(
            "comment continuation",
            primary = {
                withContext(ioDispatcher) {
                    val postId = linkId.removePrefix("t3_")
                    val variables = commentsVariables(postId, Sort.BEST, null, "")
                        .put("after", cursor)
                    val data = gql("PostComments", POST_COMMENTS_HASH, variables)
                    val wrapper = JSONObject()
                        .put("json", JSONObject().put("data",
                            JSONObject().put("things", commentTrees(data, postId))))
                    moreChildrenAdapter.fromJson(wrapper.toString())
                        ?: throw IOException("Could not decode additional comments")
                }
            },
            backup = { fallback.getMoreChildren(children, linkId) }
        )
    }

    private suspend fun searchPosts(
        query: String,
        sort: Sort?,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing = withFallback(
        "post search",
        primary = {
            withContext(ioDispatcher) {
                val selectedSort = searchSort(sort)
                val filters = safeSearchFilters()
                if (timeSorting != null && timeSorting != TimeSorting.ALL && selectedSort != "NEW") {
                    filters.put(JSONObject()
                        .put("key", "time_range")
                        .put("value", timeSorting.type))
                }
                val variables = JSONObject()
                    .put("afterCursor", after ?: JSONObject.NULL)
                    .put("filters", filters)
                    .put("pageSize", PAGE_SIZE)
                    .put("productSurface", "")
                    .put("query", query)
                    .put("sort", selectedSort)
                val data = gql("SearchPosts", SEARCH_POSTS_HASH, variables)
                val connection = data.optJSONObject("search")
                    ?.optJSONObject("general")
                    ?.optJSONObject("posts")
                    ?: throw IOException("Reddit omitted post search results")
                postListing(connection, unwrapPostInfo = false)
            }
        },
        backup = {
            val marker = " subreddit:"
            if (query.contains(marker)) {
                val subreddit = query.substringAfterLast(marker).trim()
                val searchQuery = query.substringBeforeLast(marker).trim()
                fallback.searchInSubreddit(subreddit, searchQuery, sort, timeSorting, after)
            } else {
                fallback.searchPost(query, sort, timeSorting, after)
            }
        }
    )

    private fun postListing(connection: JSONObject, unwrapPostInfo: Boolean): Listing {
        val edges = connection.optJSONArray("edges") ?: JSONArray()
        val children = JSONArray()
        for (index in 0 until edges.length()) {
            val node = edges.optJSONObject(index)?.optJSONObject("node") ?: continue
            val post = if (unwrapPostInfo) node.optJSONObject("postInfo") ?: node else node
            if (post.has("id")) children.put(postChild(post))
        }
        return listing(children, pageCursor(connection))
    }

    private fun postChild(post: JSONObject): JSONObject {
        val community = post.optJSONObject("subreddit") ?: JSONObject()
        val author = post.optJSONObject("authorInfo") ?: JSONObject()
        val content = post.optJSONObject("content") ?: JSONObject()
        val flair = post.optJSONObject("flair") ?: JSONObject()
        val media = post.optJSONObject("media") ?: JSONObject()
        val url = post.optString("url")
        val subreddit = firstNonEmpty(
            community.optString("name"),
            post.optString("subredditName"),
            subredditFromPermalink(post.optString("permalink"))
        )
        val data = JSONObject()
            .put("subreddit", subreddit)
            .put("link_flair_richtext", parseJsonArray(flair.optString("richtext")))
            .put("author_flair_richtext", JSONArray())
            .put("title", firstNonEmpty(post.optString("title"), post.optString("postTitle")))
            .put("subreddit_name_prefixed", firstNonEmpty(community.optString("prefixedName"), "r/" + subreddit))
            .put("name", normalizeFullname(post.optString("id"), "t3_"))
            .put("upvote_ratio", post.optDouble("upvoteRatio", 1.0))
            .put("total_awards_received", 0)
            .put("is_original_content", false)
            .put("link_flair_text", nullableString(flair.optString("text")))
            .put("author_flair_text", JSONObject.NULL)
            .put("gallery_data", JSONObject.NULL)
            .put("score", post.optInt("score", 0))
            .put("post_hint", nullableString(post.optString("postHint")))
            .put("is_self", post.optBoolean("isSelfPost", false))
            .put("crosspost_parent_list", JSONObject.NULL)
            .put("domain", firstNonEmpty(post.optString("domain"), domainFromUrl(url)))
            .put("selftext_html", nullableString(safeHtml(content.optString("html"), content.optString("markdown"))))
            .put("suggested_sort", "confidence")
            .put("archived", post.optBoolean("isArchived", false))
            .put("over_18", post.optBoolean("isNsfw", false))
            .put("preview", JSONObject.NULL)
            .put("all_awardings", JSONArray())
            .put("spoiler", post.optBoolean("isSpoiler", false))
            .put("locked", post.optBoolean("isLocked", false))
            .put("distinguished", nullableString(post.optString("distinguishedAs").lowercase(Locale.US)))
            .put("author", firstNonEmpty(author.optString("name"), author.optString("displayName"), "[deleted]"))
            .put("num_comments", post.optInt("commentCount", 0))
            .put("permalink", post.optString("permalink"))
            .put("stickied", post.optBoolean("isStickied", false))
            .put("url", url)
            .put("created_utc", epochSeconds(post.optString("createdAt")))
            .put("media", JSONObject.NULL)
            .put("media_metadata", JSONObject.NULL)
            .put("is_gallery", false)
            .put("is_video", media.optJSONObject("video") != null || media.optJSONObject("streaming") != null)
        return JSONObject().put("kind", "t3").put("data", data)
    }

    private fun aboutChild(community: JSONObject): JSONObject {
        val styles = community.optJSONObject("styles") ?: JSONObject()
        val legacy = styles.opt("legacyIcon")
        val icon = when (legacy) {
            is JSONObject -> firstNonEmpty(styles.optString("icon"), legacy.optString("url"))
            is String -> firstNonEmpty(styles.optString("icon"), legacy)
            else -> styles.optString("icon")
        }
        val banner = firstNonEmpty(
            styles.optString("mobileBannerImage"),
            styles.optString("bannerBackgroundImage"),
            styles.optString("legacyBannerBackgroundImage")
        )
        val name = community.optString("name")
        val description = firstNonEmpty(
            community.optString("publicDescriptionText"),
            community.optJSONObject("description")?.optString("markdown").orEmpty()
        )
        val data = JSONObject()
            .put("wiki_enabled", JSONObject.NULL)
            .put("display_name", name)
            .put("header_img", nullableString(banner))
            .put("title", firstNonEmpty(community.optString("title"), name))
            .put("primary_color", nullableString(styles.optString("primaryColor")))
            .put("active_user_count", JSONObject.NULL)
            .put("icon_img", nullableString(icon))
            .put("subscribers", community.optInt("subscribersCount", 0))
            .put("quarantine", false)
            .put("public_description_html", safeHtml("", description))
            .put("community_icon", icon)
            .put("banner_background_image", banner)
            .put("key_color", nullableString(styles.optString("keyColor")))
            .put("banner_background_color", JSONObject.NULL)
            .put("over18", community.optBoolean("isNsfw", false))
            .put("description_html", safeHtml("", description))
            .put("url", firstNonEmpty(community.optString("path"), "/r/" + name + "/"))
            .put("created_utc", epochSeconds(community.optString("createdAt")))
            .put("icon_color", "")
        return JSONObject().put("kind", "t5").put("data", data)
    }

    private fun aboutUserChild(user: JSONObject): JSONObject {
        val karma = user.optJSONObject("karma") ?: JSONObject()
        val profile = user.optJSONObject("profile") ?: JSONObject()
        val styles = profile.optJSONObject("styles") ?: JSONObject()
        val icon = firstNonEmpty(
            user.optJSONObject("iconSmall")?.optString("url").orEmpty(),
            user.optJSONObject("icon")?.optString("url").orEmpty(),
            styles.optString("icon")
        )
        val data = JSONObject()
            .put("is_suspended", user.optBoolean("isSuspended", false))
            .put("is_employee", user.optBoolean("isEmployee", false))
            .put("subreddit", JSONObject.NULL)
            .put("id", nullableString(user.optString("id")))
            .put("icon_img", nullableString(icon))
            .put("link_karma", karma.optInt("fromPosts", -1))
            .put("total_karma", karma.optInt("total", -1))
            .put("name", firstNonEmpty(user.optString("name"), user.optString("displayName")))
            .put("created_utc", epochSeconds(user.optString("createdAt")))
            .put("snoovatar_img", nullableString(user.optJSONObject("snoovatarIcon")?.optString("url").orEmpty()))
            .put("comment_karma", karma.optInt("fromComments", -1))
        return JSONObject().put("kind", "t2").put("data", data)
    }

    private fun commentTrees(data: JSONObject, postId: String): JSONArray {
        val forest = data.optJSONObject("postInfoById")?.optJSONObject("commentForest")
            ?: throw IOException("Reddit returned no comment forest")
        val trees = forest.optJSONArray("trees")
            ?: throw IOException("Reddit returned no comment list")
        val roots = JSONArray()
        val stack = mutableListOf<Pair<Int, JSONObject>>()
        val postFullname = normalizeFullname(postId, "t3_")

        fun appendReply(parent: JSONObject, child: JSONObject) {
            val parentData = parent.optJSONObject("data") ?: return
            var replies = parentData.optJSONObject("replies")
            if (replies == null) {
                replies = JSONObject().put("kind", "Listing")
                    .put("data", JSONObject().put("children", JSONArray()))
                parentData.put("replies", replies)
            }
            replies.optJSONObject("data")?.optJSONArray("children")?.put(child)
        }

        for (index in 0 until trees.length()) {
            val tree = trees.optJSONObject(index) ?: continue
            val depth = tree.optInt("depth", 0).coerceAtLeast(0)
            while (stack.isNotEmpty() && stack.last().first >= depth) {
                stack.removeAt(stack.lastIndex)
            }
            val more = tree.optJSONObject("more")
            val child = if (more != null) {
                moreChild(tree, more, postFullname, depth)
            } else {
                val node = tree.optJSONObject("node") ?: continue
                commentChild(node, tree, postFullname, depth, index)
            }
            val parent = stack.lastOrNull()?.second
            if (parent == null) roots.put(child) else appendReply(parent, child)
            if (more == null) stack.add(depth to child)
        }
        return roots
    }

    private fun moreChild(
        tree: JSONObject,
        more: JSONObject,
        postFullname: String,
        depth: Int
    ): JSONObject {
        val cursor = more.optString("cursor")
        if (cursor.isBlank()) throw IOException("Reddit returned an empty comment cursor")
        val token = encodeCursor(cursor)
        val parentId = firstNonEmpty(tree.optString("parentId"), postFullname)
        val data = JSONObject()
            .put("count", more.optInt("count", 0))
            .put("name", "more_" + token)
            .put("id", token)
            .put("depth", depth)
            .put("parent_id", parentId)
            .put("children", JSONArray().put(token))
        return JSONObject().put("kind", "more").put("data", data)
    }

    private fun commentChild(
        node: JSONObject,
        tree: JSONObject,
        postFullname: String,
        depth: Int,
        index: Int
    ): JSONObject {
        val content = node.optJSONObject("content") ?: JSONObject()
        val author = node.optJSONObject("authorInfo") ?: JSONObject()
        val flair = node.optJSONObject("authorFlair") ?: JSONObject()
        val postInfo = node.optJSONObject("postInfo") ?: JSONObject()
        val subreddit = postInfo.optJSONObject("subreddit") ?: JSONObject()
        val rawId = node.optString("id")
        val deleted = node.optString("__typename") == "DeletedComment"
        val id = rawId.removePrefix("t1_").ifBlank { "deleted_" + index }
        val fullname = "t1_" + id
        val prefix = firstNonEmpty(
            subreddit.optString("prefixedName"),
            subreddit.optString("name").takeIf { it.isNotBlank() }?.let { "r/" + it }.orEmpty(),
            subredditFromPermalink(node.optString("permalink")).let { if (it.isBlank()) "" else "r/" + it }
        )
        val markdown = firstNonEmpty(
            content.optString("markdown"), content.optString("preview"),
            if (deleted) "[deleted]" else ""
        )
        val bodyHtml = "<div class=\"md\">" + safeHtml(content.optString("html"), markdown) + "</div>"
        val edited = epochSeconds(node.optString("editedAt"))
        val data = JSONObject()
            .put("total_awards_received", 0)
            .put("author_flair_richtext", parseJsonArray(flair.optString("richtext")))
            .put("link_id", postFullname)
            .put("replies", JSONObject().put("kind", "Listing")
                .put("data", JSONObject().put("children", JSONArray())))
            .put("author", if (deleted) "[deleted]" else firstNonEmpty(author.optString("name"), author.optString("displayName"), "[deleted]"))
            .put("score", node.optInt("score", 0))
            .put("all_awardings", JSONArray())
            .put("body_html", bodyHtml)
            .put("edited", if (edited > 0L) edited else false)
            .put("is_submitter", node.optBoolean("isOP", false))
            .put("stickied", node.optBoolean("isStickied", false))
            .put("score_hidden", node.optBoolean("isScoreHidden", false))
            .put("permalink", node.optString("permalink"))
            .put("id", id)
            .put("name", fullname)
            .put("created_utc", epochSeconds(node.optString("createdAt")))
            .put("controversiality", 0)
            .put("author_flair_text", nullableString(flair.optString("text")))
            .put("depth", depth)
            .put("distinguished", nullableString(node.optString("distinguishedAs").lowercase(Locale.US)))
            .put("subreddit_name_prefixed", prefix)
            .put("link_title", nullableString(postInfo.optString("title")))
            .put("link_permalink", nullableString(postInfo.optString("permalink")))
            .put("link_author", nullableString(postInfo.optJSONObject("authorInfo")?.optString("name").orEmpty()))
        return JSONObject().put("kind", "t1").put("data", data)
    }

    private fun commentsVariables(
        postId: String,
        sort: Sort,
        commentId: String?,
        permalink: String
    ): JSONObject {
        val variables = JSONObject()
            .put("id", normalizeFullname(postId, "t3_"))
            .put("sortType", commentSort(sort))
            .put("count", COMMENT_COUNT)
            .put("maxDepth", COMMENT_DEPTH)
            .put("includeConsistencyToken", false)
            .put("includePremiumAvatarTreatment", false)
        if (!commentId.isNullOrBlank()) {
            val context = Regex("[?&]context=(\\d+)").find(permalink)?.groupValues?.getOrNull(1)
                ?.toIntOrNull() ?: 0
            variables.put("focusedCommentContext", JSONObject()
                .put("commentId", normalizeFullname(commentId, "t1_"))
                .put("isFocusedThreadOnly", context == 0))
        }
        Regex("[?&]cursor=([^&]+)").find(permalink)?.groupValues?.getOrNull(1)?.let { token ->
            decodeCursor(token)?.let { variables.put("after", it) }
        }
        return variables
    }

    private suspend fun gql(name: String, hash: String, variables: JSONObject): JSONObject {
        val payload = JSONObject()
            .put("operationName", name)
            .put("variables", variables)
            .put("extensions", JSONObject().put("persistedQuery",
                JSONObject().put("version", 1).put("sha256Hash", hash)))
        val response = requestJson(GRAPHQL_URL, payload.toString())
        val errors = response.optJSONArray("errors")
        if (errors != null && errors.length() > 0) {
            val message = errors.optJSONObject(0)?.optString("message").orEmpty()
            throw IOException("Reddit " + name + " failed: " + message.ifBlank { "GraphQL error" })
        }
        return response.optJSONObject("data") ?: throw IOException("Reddit returned no data for " + name)
    }

    private suspend fun requestJson(url: String, body: String? = null): JSONObject {
        var attempt = 0
        var refreshed = false
        while (attempt < MAX_ATTEMPTS) {
            val credentials = oauthManager.getCredentials()
                ?: throw IOException("Could not obtain Reddit OAuth credentials")
            val builder = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("Authorization", credentials.authorization)
                .header("User-Agent", credentials.userAgent)
                .header("client-vendor-id", credentials.deviceId)
                .header("X-Reddit-Device-Id", credentials.deviceId)
                .header("x-reddit-retry", "algo=no-retries")
                .header("x-reddit-compression", "1")
                .header("x-reddit-qos", credentials.qos)
                .header("x-reddit-media-codecs", credentials.codecs)
            credentials.loid?.let { builder.header("x-reddit-loid", it) }
            credentials.session?.let { builder.header("x-reddit-session", it) }
            if (body == null) builder.get() else {
                builder.header("Content-Type", "application/json; charset=UTF-8")
                    .post(body.toRequestBody(JSON_MEDIA_TYPE))
            }

            val response = try {
                client.newCall(builder.build()).execute()
            } catch (e: IOException) {
                if (attempt + 1 >= MAX_ATTEMPTS) throw e
                Thread.sleep(RETRY_BASE_MS * (attempt + 1))
                attempt++
                continue
            }

            val status = response.code
            val retryAfter = response.header("Retry-After")?.toLongOrNull()
            val responseText = response.body?.string().orEmpty()
            response.close()

            if (status == 401 && !refreshed) {
                oauthManager.invalidate()
                refreshed = true
                attempt++
                continue
            }
            if ((status == 429 || status in 500..599) && attempt + 1 < MAX_ATTEMPTS) {
                val delay = (retryAfter?.times(1000L) ?: RETRY_BASE_MS * (attempt + 1))
                    .coerceIn(RETRY_BASE_MS, MAX_RETRY_MS)
                Thread.sleep(delay)
                attempt++
                continue
            }
            if (status !in 200..299) {
                val detail = responseText.take(180).replace('\n', ' ')
                throw IOException("Reddit returned HTTP " + status + if (detail.isBlank()) "" else ": " + detail)
            }
            return try {
                JSONObject(responseText)
            } catch (e: Exception) {
                throw IOException("Reddit returned invalid JSON", e)
            }
        }
        throw IOException("Reddit request exhausted its retry budget")
    }

    private suspend fun <T> withFallback(
        label: String,
        primary: suspend () -> T,
        backup: suspend () -> T
    ): T {
        try {
            return primary()
        } catch (e: CancellationException) {
            throw e
        } catch (primaryError: Exception) {
            Log.w(TAG, label + " GraphQL request failed; trying legacy JSON", primaryError)
            try {
                return backup()
            } catch (backupError: Exception) {
                backupError.addSuppressed(primaryError)
                throw backupError
            }
        }
    }

    private fun listing(children: JSONArray, after: String?): Listing {
        val data = JSONObject()
            .put("modhash", JSONObject.NULL)
            .put("dist", children.length())
            .put("children", children)
            .put("after", after ?: JSONObject.NULL)
            .put("before", JSONObject.NULL)
        return listingAdapter.fromJson(
            JSONObject().put("kind", "Listing").put("data", data).toString()
        ) ?: throw IOException("Could not decode Reddit listing")
    }

    private fun safeSearchFilters(): JSONArray = JSONArray().put(
        JSONObject().put("key", "safe_search").put("value", "off")
    )

    private fun pageCursor(connection: JSONObject): String? {
        val page = connection.optJSONObject("pageInfo") ?: return null
        if (!page.optBoolean("hasNextPage", false)) return null
        return page.optString("endCursor").takeIf { it.isNotBlank() }
    }

    private fun feedRange(sort: Sort, time: TimeSorting?): String? {
        if (sort != Sort.TOP && sort != Sort.CONTROVERSIAL) return null
        return time?.takeUnless { it == TimeSorting.ALL }?.type?.uppercase(Locale.US)
    }

    private fun feedSort(sort: Sort): String = when (sort) {
        Sort.BEST -> "BEST"
        Sort.HOT -> "HOT"
        Sort.NEW -> "NEW"
        Sort.TOP -> "TOP"
        Sort.RISING -> "RISING"
        Sort.CONTROVERSIAL -> "CONTROVERSIAL"
        else -> "HOT"
    }

    private fun searchSort(sort: Sort?): String = when (sort) {
        Sort.HOT -> "HOT"
        Sort.TOP -> "TOP"
        Sort.NEW -> "NEW"
        Sort.COMMENTS -> "COMMENTS"
        else -> "RELEVANCE"
    }

    private fun commentSort(sort: Sort): String = when (sort) {
        Sort.TOP -> "TOP"
        Sort.NEW -> "NEW"
        Sort.OLD -> "OLD"
        Sort.CONTROVERSIAL -> "CONTROVERSIAL"
        Sort.QA -> "QA"
        else -> "CONFIDENCE"
    }

    private fun safeHtml(html: String, markdown: String): String {
        val fallback = "<p>" + escapeHtml(markdown).replace("\n\n", "</p><p>").replace("\n", "<br>") + "</p>"
        return Jsoup.clean(html.takeIf { it.isNotBlank() } ?: fallback, Safelist.basicWithImages())
    }

    private fun parseJsonArray(value: String): JSONArray = try {
        if (value.isBlank()) JSONArray() else JSONArray(value)
    } catch (_: Exception) {
        JSONArray()
    }

    private fun epochSeconds(value: String): Long = try {
        if (value.isBlank()) 0L else {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
                isLenient = false
            }
            (parser.parse(value.take(19))?.time ?: 0L) / 1000L
        }
    } catch (_: Exception) {
        0L
    }

    private fun subredditFromPermalink(permalink: String): String =
        Regex("/r/([^/]+)/").find(permalink)?.groupValues?.getOrNull(1).orEmpty()

    private fun domainFromUrl(url: String): String =
        Regex("^https?://([^/]+)", RegexOption.IGNORE_CASE).find(url)?.groupValues?.getOrNull(1).orEmpty()

    private fun normalizeFullname(id: String, prefix: String): String =
        if (id.startsWith(prefix)) id else prefix + id

    private fun nullableString(value: String?): Any =
        value?.takeIf { it.isNotBlank() } ?: JSONObject.NULL

    private fun firstNonEmpty(vararg values: String): String =
        values.firstOrNull { it.isNotBlank() }.orEmpty()

    private fun escapeHtml(value: String): String = value
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;").replace("'", "&#39;")

    companion object {
        private const val TAG = "GraphQlReddit"
        private const val OAUTH_BASE = "https://oauth.reddit.com"
        private const val GRAPHQL_URL = "https://gql-fed.reddit.com/"
        private const val PAGE_SIZE = 25
        private const val COMMENT_COUNT = 200
        private const val COMMENT_DEPTH = 10
        private const val MAX_ATTEMPTS = 3
        private const val RETRY_BASE_MS = 450L
        private const val MAX_RETRY_MS = 3_000L
        private val JSON_MEDIA_TYPE = "application/json; charset=UTF-8".toMediaType()

        private const val SUBREDDIT_FEED_HASH = "97046e2a48050f10a507db354a8da60955ffccb586dcf63163bdfc6610d5cf7a"
        private const val SEARCH_COMMUNITIES_HASH = "a3333069c1ead3ceec6d9bf4da4185ff584f69d0ba8b586e545b32ea62170932"
        private const val SEARCH_POSTS_HASH = "8610e33521c90caa8d12576800c18b708ce0d19df3dfac8e2aad55d9860e4bf9"
        private const val SEARCH_USERS_HASH = "d9211ac33f72079f25cb4c8a8e00a2b4889e984f70278ec91283965c7856d33d"
        private const val POST_COMMENTS_HASH = "314f29c2494fd78c7844ef2c6b09acb4bef5b37ab50b4ddc01691d6d5c8433f8"

        internal data class PostReference(val postId: String, val commentId: String?)

        internal fun parsePostReference(permalink: String): PostReference {
            val withoutQuery = permalink.substringBefore('?')
            val path = withoutQuery.substringAfter("://", withoutQuery).substringAfter('/', "")
            val parts = path.split('/').filter { it.isNotBlank() }
            val index = parts.indexOf("comments")
            if (index >= 0) {
                val postId = parts.getOrNull(index + 1)
                    ?.takeIf { it.matches(Regex("[A-Za-z0-9]+")) }
                    ?: throw IOException("Invalid Reddit post permalink")
                val commentId = parts.getOrNull(index + 3)
                    ?.takeIf { it.matches(Regex("[A-Za-z0-9]+")) }
                return PostReference(postId, commentId)
            }
            val fullName = Regex("t3_([A-Za-z0-9]+)").find(permalink)?.groupValues?.getOrNull(1)
            val id = fullName ?: permalink.trim().trim('/').takeIf {
                it.matches(Regex("[A-Za-z0-9]+"))
            }
            return PostReference(id ?: throw IOException("Invalid Reddit post permalink"), null)
        }

        internal fun encodeCursor(cursor: String): String = Base64.encodeToString(
            cursor.toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )

        internal fun decodeCursor(token: String): String? = try {
            String(
                Base64.decode(token, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING),
                Charsets.UTF_8
            ).takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }
}
