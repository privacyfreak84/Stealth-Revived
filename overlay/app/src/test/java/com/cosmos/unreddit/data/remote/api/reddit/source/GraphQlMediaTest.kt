package com.cosmos.unreddit.data.remote.api.reddit.source

import com.cosmos.unreddit.data.model.Sort
import com.cosmos.unreddit.data.model.TimeSorting
import com.cosmos.unreddit.data.remote.api.reddit.source.GraphQlRedditSource.Companion.FeedRoute
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GraphQlMediaTest {

    private fun asset(id: String, url: String, mime: String = "image/jpeg", status: String = "VALID") =
        JSONObject()
            .put("id", id).put("url", url).put("mimetype", mime).put("status", status)
            .put("width", 3000).put("height", 2000)
            .put("xxxlarge", JSONObject().put("url", "$url?w=1080")
                .put("dimensions", JSONObject().put("width", 1080).put("height", 720)))

    private fun galleryPost(vararg assets: JSONObject) = JSONObject().put(
        "gallery",
        JSONObject().put("items", JSONArray().apply {
            assets.forEachIndexed { i, a -> put(JSONObject().put("id", "item$i").put("caption", "c$i").put("media", a)) }
        })
    )

    @Test
    fun galleryKeepsEveryValidItemInOrder() {
        val post = galleryPost(
            asset("a", "https://i.redd.it/a.jpg"),
            asset("b", "https://i.redd.it/b.jpg", status = "FAILED"),
            asset("c", "https://v.redd.it/c.mp4", mime = "video/mp4"),
        )
        val (data, metadata) = GraphQlRedditSource.graphQlGallery(post)!!
        val items = data.getJSONArray("items")
        assertEquals(2, items.length())
        assertEquals("a", items.getJSONObject(0).getString("media_id"))
        assertEquals("c", items.getJSONObject(1).getString("media_id"))
        assertEquals("https://i.redd.it/a.jpg", metadata.getJSONObject("a").getJSONObject("s").getString("u"))
        val video = metadata.getJSONObject("c").getJSONObject("s")
        assertEquals("https://v.redd.it/c.mp4", video.getString("mp4"))
        assertFalse(video.has("u"))
        assertFalse(metadata.has("b"))
    }

    @Test
    fun noGalleryWhenNoItems() {
        assertNull(GraphQlRedditSource.graphQlGallery(JSONObject()))
        assertNull(GraphQlRedditSource.graphQlGallery(galleryPost(asset("x", "u", status = "FAILED"))))
    }

    @Test
    fun previewPrefersResizedStillThenGallery() {
        val still = JSONObject().put("media", JSONObject().put("still", JSONObject()
            .put("source", JSONObject().put("url", "https://i.redd.it/full.jpg")
                .put("dimensions", JSONObject().put("width", 4000).put("height", 3000)))
            .put("xxlarge", JSONObject().put("url", "https://preview.redd.it/960.jpg")
                .put("dimensions", JSONObject().put("width", 960).put("height", 720)))))
        val source = GraphQlRedditSource.graphQlPreview(still)!!
            .getJSONArray("images").getJSONObject(0).getJSONObject("source")
        assertEquals("https://preview.redd.it/960.jpg", source.getString("url"))
        assertEquals(960, source.getInt("width"))

        val gallery = GraphQlRedditSource.graphQlPreview(galleryPost(asset("a", "https://i.redd.it/a.jpg")))!!
        assertEquals(
            "https://i.redd.it/a.jpg?w=1080",
            gallery.getJSONArray("images").getJSONObject(0).getJSONObject("source").getString("url")
        )
        assertNull(GraphQlRedditSource.graphQlPreview(JSONObject()))
    }

    @Test
    fun redditVideoUsesDownloadThenHls() {
        val media = JSONObject()
            .put("streaming", JSONObject().put("hlsUrl", "https://v.redd.it/x/HLSPlaylist.m3u8").put("isGif", true)
                .put("dimensions", JSONObject().put("width", 720).put("height", 1280)))
        val hls = GraphQlRedditSource.graphQlRedditVideo(media)!!.getJSONObject("reddit_video")
        assertEquals("https://v.redd.it/x/HLSPlaylist.m3u8", hls.getString("fallback_url"))
        assertTrue(hls.getBoolean("is_gif"))
        media.put("download", JSONObject().put("url", "https://v.redd.it/x/CMAF_720.mp4"))
        assertEquals(
            "https://v.redd.it/x/CMAF_720.mp4",
            GraphQlRedditSource.graphQlRedditVideo(media)!!.getJSONObject("reddit_video").getString("fallback_url")
        )
        assertNull(GraphQlRedditSource.graphQlRedditVideo(JSONObject()))
    }

    @Test
    fun mergeKeepsFeedOrderAndFallsBackPerPost() {
        fun child(name: String, tag: String) =
            JSONObject().put("kind", "t3").put("data", JSONObject().put("name", name).put("src", tag))
        val graph = JSONArray().put(child("t3_a", "gql")).put(child("t3_b", "gql")).put(child("t3_c", "gql"))
        val rest = JSONObject().put("data", JSONObject().put("children",
            JSONArray().put(child("t3_c", "rest")).put(child("t3_a", "rest"))))
        val merged = GraphQlRedditSource.mergeById(graph, rest)
        assertEquals(listOf("t3_a", "t3_b", "t3_c"), (0 until 3).map { merged.getJSONObject(it).getJSONObject("data").getString("name") })
        assertEquals(listOf("rest", "gql", "rest"), (0 until 3).map { merged.getJSONObject(it).getJSONObject("data").getString("src") })
    }

    @Test
    fun feedRouteNeverMixesCursors() {
        assertEquals(FeedRoute.REST_THEN_GRAPHQL, GraphQlRedditSource.feedRoute(null, showNsfw = true))
        assertEquals(FeedRoute.GRAPHQL, GraphQlRedditSource.feedRoute(null, showNsfw = false))
        assertEquals(FeedRoute.REST_ONLY, GraphQlRedditSource.feedRoute("t3_abc", showNsfw = false))
        assertEquals(FeedRoute.GRAPHQL, GraphQlRedditSource.feedRoute("eyJ2IjoiMSJ9", showNsfw = true))
    }

    @Test
    fun restFeedUrlCarriesSortTimeAndCursor() {
        assertEquals(
            "https://oauth.reddit.com/r/pics+aww/top.json?raw_json=1&limit=25&include_over_18=1&after=t3_x&t=week",
            GraphQlRedditSource.restFeedUrl("pics+aww", Sort.TOP, TimeSorting.WEEK, "t3_x")
        )
        assertEquals(
            "https://oauth.reddit.com/r/popular/hot.json?raw_json=1&limit=25&include_over_18=1",
            GraphQlRedditSource.restFeedUrl("popular", Sort.BEST, null, null)
        )
    }
}
