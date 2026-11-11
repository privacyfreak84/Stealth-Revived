package com.cosmos.unreddit.ui.mediaviewer

import android.Manifest
import android.os.Build
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
            is MediaRequest.Gallery -> viewModel.setMedia(request.items)
            is MediaRequest.Link -> viewModel.loadMedia(request.link, request.type)
            // The request lives in memory; after process death there is nothing to show.
            null -> onClose()
        }
    }
    val media by viewModel.media.collectAsStateWithLifecycle()
    val muted by viewModel.isVideoMuted.collectAsStateWithLifecycle(initialValue = true)
    var controlsVisible by remember { mutableStateOf(true) }
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
                        enter = fadeIn() + slideInVertically { it / 2 },
                        exit = fadeOut() + slideOutVertically { it / 2 },
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

/** Pinch and double-tap zoomable image. */
@Composable
private fun ZoomableImage(url: String, onTap: () -> Unit, onZoomChanged: (Boolean) -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = { tap ->
                        if (scale > 1f) {
                            scale = 1f; offset = Offset.Zero
                        } else {
                            scale = 2.5f
                            offset = (Offset(size.width / 2f, size.height / 2f) - tap) * 1.5f
                        }
                        onZoomChanged(scale > 1f)
                    },
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 6f)
                    if (newScale > 1f || scale > 1f) {
                        scale = newScale
                        offset = if (scale == 1f) Offset.Zero else offset + pan
                        onZoomChanged(scale > 1f)
                    }
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

/** ExoPlayer video with Expressive controls and a wavy progress line. */
@Composable
private fun VideoPage(
    item: GalleryMedia,
    active: Boolean,
    muted: Boolean,
    controlsVisible: Boolean,
    onTap: () -> Unit,
) {
    val context = LocalContext.current
    val helper = remember { ExoPlayerHelper(context) }
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
    var progress by remember { mutableFloatStateOf(0f) }

    DisposableEffect(player) { onDispose { player.release(); helper.clearCache() } }
    LaunchedEffect(active, playing) { player.playWhenReady = active && playing }
    LaunchedEffect(muted) { player.volume = if (muted) 0f else 1f }
    LaunchedEffect(active) {
        while (active) {
            val duration = player.duration.takeIf { it > 0 } ?: 1L
            progress = (player.currentPosition.toFloat() / duration).coerceIn(0f, 1f)
            delay(100)
        }
    }

    Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures(onTap = { onTap() }) }) {
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
            controlsVisible,
            enter = fadeIn(), exit = fadeOut(),
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
            enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 104.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                LinearWavyProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(player) {
                            detectTapGestures { tap ->
                                val fraction = (tap.x / size.width).coerceIn(0f, 1f)
                                if (player.duration > 0) player.seekTo((player.duration * fraction).toLong())
                            }
                        },
                )
            }
        }
    }
}
