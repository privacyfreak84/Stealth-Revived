package com.cosmos.unreddit.ui.mediaviewer

import android.Manifest
import android.os.Build
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.GalleryMedia
import com.cosmos.unreddit.data.model.Resource
import com.cosmos.unreddit.data.worker.MediaDownloadWorker
import com.cosmos.unreddit.ui.common.compose.ErrorState
import com.cosmos.unreddit.ui.common.compose.LocalLinkActions
import com.cosmos.unreddit.ui.navigation.MediaRequest
import com.cosmos.unreddit.ui.theme.StealthTheme
import com.cosmos.unreddit.ui.common.compose.stealthApp
import com.cosmos.unreddit.util.ExoPlayerHelper
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.MergingMediaSource
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.StyledPlayerView
import kotlinx.coroutines.delay
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Full-screen images, galleries and videos with an Expressive floating toolbar. */
@Composable
fun MediaViewerScreen(
    request: MediaRequest?,
    onClose: () -> Unit,
    viewModel: MediaViewerViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val links = LocalLinkActions.current
    LaunchedEffect(request) {
        when (request) {
            is MediaRequest.Gallery -> {
                viewModel.setSelectedPage(request.start.coerceIn(0, (request.items.size - 1).coerceAtLeast(0)))
                viewModel.setMedia(request.items)
            }
            is MediaRequest.Link -> viewModel.loadMedia(request.link, request.type)
            // The request lives in memory; after process death there is nothing to show.
            null -> onClose()
        }
    }
    val media by viewModel.media.collectAsStateWithLifecycle()
    val muted by viewModel.isVideoMuted.collectAsStateWithLifecycle(initialValue = true)
    var controlsVisible by remember { mutableStateOf(true) }
    // One video cache for the whole viewer; every video page shares it.
    val helper = remember { ExoPlayerHelper(context) }
    DisposableEffect(helper) { onDispose { helper.release() } }
    BackHandler(onBack = onClose)

    // The viewer is always dark, whatever the app theme.
    StealthTheme(scheme = context.stealthApp.colorScheme, darkTheme = true) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            when (val state = media) {
                is Resource.Loading -> ContainedLoadingIndicator(Modifier.align(Alignment.Center).size(72.dp))
                is Resource.Error -> ErrorState(
                    Modifier.align(Alignment.Center),
                    message = stringResource(
                        if (state.code == 403 || state.code == 404) R.string.dialog_media_not_found_body
                        else R.string.network_retry_message
                    ),
                    onRetry = (request as? MediaRequest.Link)?.let { link ->
                        { viewModel.loadMedia(link.link, link.type, true) }
                    },
                )
                is Resource.Success -> {
                    val items = state.data
                    val pager = rememberPagerState(viewModel.selectedPage.value) { items.size }
                    var zoomed by remember { mutableStateOf(false) }
                    LaunchedEffect(pager.currentPage) { viewModel.setSelectedPage(pager.currentPage); zoomed = false }
                    HorizontalPager(
                        state = pager,
                        userScrollEnabled = !zoomed,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = 1,
                    ) { page ->
                        val item = items[page]
                        when (item.type) {
                            GalleryMedia.Type.VIDEO -> VideoPage(
                                item = item,
                                helper = helper,
                                active = page == pager.currentPage,
                                muted = muted,
                                controlsVisible = controlsVisible,
                                onTap = { controlsVisible = !controlsVisible },
                            )
                            else -> ZoomableImage(
                                url = item.url,
                                onTap = { controlsVisible = !controlsVisible },
                                onZoomChanged = { if (page == pager.currentPage) zoomed = it },
                            )
                        }
                    }
                    val current = items.getOrNull(pager.currentPage)
                    AnimatedVisibility(
                        visible = controlsVisible,
                        enter = fadeIn(controlsFade()) + slideInVertically(controlsSlide()) { it / 2 },
                        exit = fadeOut(controlsFade()) + slideOutVertically(controlsSlide()) { it / 2 },
                        modifier = Modifier.align(Alignment.BottomCenter),
                    ) {
                        Column(
                            Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            current?.description?.takeIf { it.isNotBlank() }?.let {
                                Text(
                                    it,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                                )
                            }
                            MediaToolbar(
                                index = pager.currentPage,
                                count = items.size,
                                current = current,
                                muted = muted,
                                onClose = onClose,
                                onMute = { viewModel.setMuted(!muted) },
                                onShare = { current?.let { links.share(it.url) } },
                                onOpen = { current?.let { links.openBrowser(it.url) } },
                            )
                        }
                    }
                }
            }
            AnimatedVisibility(
                controlsVisible && media !is Resource.Success,
                modifier = Modifier.statusBarsPadding().padding(8.dp),
            ) {
                FilledIconButton(onClick = onClose, shapes = IconButtonDefaults.shapes()) {
                    Icon(Icons.Rounded.Close, stringResource(R.string.action_close))
                }
            }
        }
    }
}

@Composable
private fun MediaToolbar(
    index: Int,
    count: Int,
    current: GalleryMedia?,
    muted: Boolean,
    onClose: () -> Unit,
    onMute: () -> Unit,
    onShare: () -> Unit,
    onOpen: () -> Unit,
) {
    val context = LocalContext.current
    val download = {
        current?.let {
            MediaDownloadWorker.enqueueWork(context.applicationContext, it.url, it.type, it.sound)
            Toast.makeText(context, R.string.toast_download_started, Toast.LENGTH_SHORT).show()
        }
        Unit
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        // Notifications are optional; storage is required before Android 10.
        if (granted || Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) download()
    }
    HorizontalFloatingToolbar(
        expanded = true,
        colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
        leadingContent = {
            IconButton(onClick = onClose, shapes = IconButtonDefaults.shapes()) {
                Icon(Icons.Rounded.Close, stringResource(R.string.action_close))
            }
        },
        trailingContent = {
            IconButton(onClick = onOpen, shapes = IconButtonDefaults.shapes()) {
                Icon(Icons.AutoMirrored.Rounded.OpenInNew, stringResource(R.string.action_open_in_browser))
            }
        },
    ) {
        if (count > 1) {
            Text(
                "${index + 1} ${stringResource(R.string.media_page_counter_label)} $count",
                style = MaterialTheme.typography.labelLargeEmphasized,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
        if (current?.type == GalleryMedia.Type.VIDEO) {
            IconButton(onClick = onMute, shapes = IconButtonDefaults.shapes()) {
                Icon(
                    if (muted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                    stringResource(if (muted) R.string.action_unmute else R.string.action_mute),
                )
            }
        }
        IconButton(
            onClick = {
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                        permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> download()
                    else -> permission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            },
            shapes = IconButtonDefaults.shapes(),
        ) { Icon(Icons.Rounded.Download, stringResource(R.string.action_download)) }
        IconButton(onClick = onShare, shapes = IconButtonDefaults.shapes()) {
            Icon(Icons.Rounded.Share, stringResource(R.string.menu_share_link))
        }
    }
}

/**
 * Pinch and double-tap zoomable image. One-finger drags at 1x are left unconsumed so the
 * gallery pager can swipe; panning only takes over once the image is zoomed.
 */
@Composable
private fun ZoomableImage(url: String, onTap: () -> Unit, onZoomChanged: (Boolean) -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var bounds by remember { mutableStateOf(IntSize.Zero) }
    fun clamp(value: Offset, s: Float): Offset {
        val maxX = bounds.width * (s - 1f) / 2f
        val maxY = bounds.height * (s - 1f) / 2f
        return Offset(value.x.coerceIn(-maxX, maxX), value.y.coerceIn(-maxY, maxY))
    }
    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { bounds = it }
            .tapOrDoubleTap(
                Unit,
                onTap = onTap,
                onDoubleTap = { tap ->
                    if (scale > 1f) {
                        scale = 1f; offset = Offset.Zero
                    } else {
                        scale = 2.5f
                        offset = clamp((Offset(bounds.width / 2f, bounds.height / 2f) - tap) * 1.5f, scale)
                    }
                    onZoomChanged(scale > 1f)
                },
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val fingers = event.changes.count { it.pressed }
                        if (fingers > 1 || scale > 1f) {
                            val newScale = (scale * event.calculateZoom()).coerceIn(1f, 6f)
                            offset = if (newScale == 1f) Offset.Zero else clamp(offset + event.calculatePan(), newScale)
                            scale = newScale
                            onZoomChanged(scale > 1f)
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().graphicsLayer {
                scaleX = scale; scaleY = scale
                translationX = offset.x; translationY = offset.y
            },
        )
    }
}

/** ExoPlayer video with a play button, a seek slider and double-tap to skip. */
@Composable
private fun VideoPage(
    item: GalleryMedia,
    helper: ExoPlayerHelper,
    active: Boolean,
    muted: Boolean,
    controlsVisible: Boolean,
    onTap: () -> Unit,
) {
    val context = LocalContext.current
    val player = remember(item.url) {
        item.url.toHttpUrlOrNull()?.takeIf { it.host.contains("redgifs", true) }?.let { url ->
            helper.setRequestProperties(url.queryParameterNames.associateWith { url.queryParameter(it) ?: "" })
        }
        ExoPlayer.Builder(context).setMediaSourceFactory(helper.defaultMediaSourceFactory).build().apply {
            val videoItem = helper.getMediaItem(item.url)
            if (item.sound != null) {
                setMediaSource(MergingMediaSource(helper.getMediaSource(videoItem), helper.getMediaSource(item.sound)))
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        // Reddit sometimes has no audio track; fall back to video only.
                        setMediaItem(videoItem)
                        prepare()
                    }
                })
            } else {
                setMediaSource(helper.getMediaSource(videoItem))
            }
            repeatMode = Player.REPEAT_MODE_ALL
            prepare()
        }
    }
    var playing by remember { mutableStateOf(true) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    // While the thumb is held, show where it is instead of the playing position.
    var scrubbing by remember { mutableStateOf<Float?>(null) }
    var skipHint by remember { mutableStateOf<Int?>(null) }
    var pageWidth by remember { mutableIntStateOf(0) }

    DisposableEffect(player) { onDispose { player.release() } }
    LaunchedEffect(active, playing) { player.playWhenReady = active && playing }
    LaunchedEffect(muted) { player.volume = if (muted) 0f else 1f }
    LaunchedEffect(active) {
        while (active) {
            duration = player.duration.takeIf { it > 0 } ?: 0L
            position = player.currentPosition.coerceAtLeast(0L)
            delay(200)
        }
    }
    LaunchedEffect(skipHint) { if (skipHint != null) { delay(650); skipHint = null } }

    fun skip(by: Long) {
        val end = player.duration.takeIf { it > 0 } ?: return
        player.seekTo((player.currentPosition + by).coerceIn(0L, end))
        position = player.currentPosition
    }

    Box(
        Modifier.fillMaxSize().onSizeChanged { pageWidth = it.width }.tapOrDoubleTap(
            player,
            onTap = onTap,
            onDoubleTap = { tap ->
                val forward = tap.x > pageWidth / 2f
                skip(if (forward) SKIP_MS else -SKIP_MS)
                skipHint = if (forward) 1 else -1
            },
        ),
    ) {
        AndroidView(
            factory = {
                StyledPlayerView(it).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { it.player = player },
            modifier = Modifier.fillMaxSize(),
        )
        AnimatedVisibility(
            visible = skipHint != null,
            enter = fadeIn(controlsFade()), exit = fadeOut(controlsFade()),
            modifier = Modifier
                .align(if ((skipHint ?: 1) > 0) Alignment.CenterEnd else Alignment.CenterStart)
                .padding(horizontal = 40.dp),
        ) {
            Surface(
                shape = MaterialShapes.Pill.toShape(),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Icon(
                        if ((skipHint ?: 1) > 0) Icons.Rounded.FastForward else Icons.Rounded.FastRewind,
                        null,
                    )
                    Text(
                        stringResource(R.string.video_skip_seconds, SKIP_MS / 1000),
                        style = MaterialTheme.typography.labelLargeEmphasized,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        }
        AnimatedVisibility(
            controlsVisible,
            enter = fadeIn(controlsFade()), exit = fadeOut(controlsFade()),
            modifier = Modifier.align(Alignment.Center),
        ) {
            FilledIconButton(
                onClick = { playing = !playing },
                shapes = IconButtonDefaults.shapes(shape = MaterialShapes.Cookie9Sided.toShape()),
                modifier = Modifier.size(80.dp),
            ) {
                Icon(
                    if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    stringResource(if (playing) R.string.action_pause else R.string.action_play),
                    Modifier.size(40.dp),
                )
            }
        }
        AnimatedVisibility(
            controlsVisible,
            enter = fadeIn(controlsFade()), exit = fadeOut(controlsFade()),
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 96.dp),
        ) {
            val fraction = scrubbing ?: if (duration > 0) position.toFloat() / duration else 0f
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            ) {
                TimeLabel(if (duration > 0) (fraction * duration).toLong() else position)
                Slider(
                    value = fraction.coerceIn(0f, 1f),
                    onValueChange = { scrubbing = it },
                    onValueChangeFinished = {
                        scrubbing?.let { value -> if (duration > 0) player.seekTo((value * duration).toLong()) }
                        position = player.currentPosition
                        scrubbing = null
                    },
                    enabled = duration > 0,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                )
                TimeLabel(duration)
            }
        }
    }
}

@Composable
private fun TimeLabel(millis: Long) {
    Text(
        formatDuration(millis),
        color = Color.White,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.45f), MaterialTheme.shapes.small)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

private const val SKIP_MS = 10_000L

internal fun formatDuration(millis: Long): String {
    val total = (millis.coerceAtLeast(0L) / 1000L)
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}

private const val CONTROLS_ANIM_MS = 120

private fun controlsFade() = tween<Float>(CONTROLS_ANIM_MS)

private fun controlsSlide() = tween<IntOffset>(CONTROLS_ANIM_MS)

/**
 * A single tap fires on release, with no wait. detectTapGestures holds single taps until the
 * double-tap timeout (about 300 ms) has passed, which made the controls feel slow. Here a
 * double tap fires [onTap] a second time to undo the first toggle, then [onDoubleTap].
 */
private fun Modifier.tapOrDoubleTap(
    key: Any?,
    onTap: () -> Unit,
    onDoubleTap: (Offset) -> Unit,
): Modifier = pointerInput(key) {
    awaitEachGesture {
        awaitFirstDown()
        val firstUp = waitForUpOrCancellation() ?: return@awaitEachGesture
        firstUp.consume()
        onTap()
        withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) { awaitFirstDown() }
            ?: return@awaitEachGesture
        val secondUp = waitForUpOrCancellation() ?: return@awaitEachGesture
        secondUp.consume()
        onTap()
        onDoubleTap(secondUp.position)
    }
}
