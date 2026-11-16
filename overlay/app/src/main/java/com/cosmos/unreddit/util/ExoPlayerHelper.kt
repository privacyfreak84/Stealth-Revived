package com.cosmos.unreddit.util

import android.content.Context
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import java.io.File

/**
 * Builds ExoPlayer media sources backed by a small on-disk video cache.
 *
 * The cache lives in its own folder and is shared by every open player: ExoPlayer allows one
 * SimpleCache per folder (a second instance throws), and it deletes any file in its folder
 * that it does not recognise, so it must never point at the app's whole cache directory.
 * Call [release] when the screen that created the helper goes away; the cache is deleted
 * once the last helper is released.
 */
class ExoPlayerHelper(context: Context) {

    private val appContext = context.applicationContext
    private var released = false

    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setAllowCrossProtocolRedirects(true)
        .setUserAgent(LinkUtil.USER_AGENT)

    private val cacheDataSourceFactory = CacheDataSource.Factory()
        .setCache(acquire(appContext))
        .setUpstreamDataSourceFactory(httpDataSourceFactory)
        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

    val defaultMediaSourceFactory by lazy { DefaultMediaSourceFactory(cacheDataSourceFactory) }

    fun setRequestProperties(requestProperties: Map<String, String>) {
        httpDataSourceFactory.setDefaultRequestProperties(requestProperties)
    }

    fun getMediaItem(url: String): MediaItem = MediaItem.fromUri(url)

    fun getMediaSource(url: String): MediaSource = getMediaSource(getMediaItem(url))

    fun getMediaSource(mediaItem: MediaItem): MediaSource =
        defaultMediaSourceFactory.createMediaSource(mediaItem)

    /** Releases this helper's hold on the shared cache. Safe to call more than once. */
    fun release() {
        if (released) return
        released = true
        release(appContext)
    }

    /** Kept for callers of the old API. */
    fun clearCache() = release()

    private companion object {
        const val CACHE_FOLDER = "exoplayer_media"
        const val CACHE_BYTES = 100L * 1024 * 1024

        private var cache: SimpleCache? = null
        private var databaseProvider: StandaloneDatabaseProvider? = null
        private var users = 0

        @Synchronized
        fun acquire(context: Context): SimpleCache {
            users++
            cache?.let { return it }
            val provider = StandaloneDatabaseProvider(context).also { databaseProvider = it }
            return SimpleCache(
                File(context.cacheDir, CACHE_FOLDER),
                LeastRecentlyUsedCacheEvictor(CACHE_BYTES),
                provider,
            ).also { cache = it }
        }

        @Synchronized
        fun release(context: Context) {
            users = (users - 1).coerceAtLeast(0)
            if (users > 0) return
            val current = cache ?: return
            cache = null
            runCatching { current.release() }
            databaseProvider?.let { provider ->
                runCatching { SimpleCache.delete(File(context.cacheDir, CACHE_FOLDER), provider) }
            }
        }
    }
}
