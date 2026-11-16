package com.cosmos.unreddit.ui.main

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.cosmos.unreddit.R
import com.cosmos.unreddit.ui.navigation.Routes

/** A top-level destination shown in the bottom navigation bar. */
enum class TopLevelDestination(
    val graphRoute: String,
    val startRoute: String,
    @StringRes val label: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    HOME(
        Routes.HOME_GRAPH, Routes.HOME, R.string.navigation_label_home,
        Icons.Rounded.Home, Icons.Outlined.Home
    ),
    SUBSCRIPTIONS(
        Routes.SUBSCRIPTIONS_GRAPH, Routes.SUBSCRIPTIONS, R.string.navigation_label_subscriptions,
        Icons.Rounded.Subscriptions, Icons.Outlined.Subscriptions
    ),
    PROFILE(
        Routes.PROFILE_GRAPH, Routes.PROFILE, R.string.navigation_label_profile,
        Icons.Rounded.AccountCircle, Icons.Outlined.AccountCircle
    ),
    SETTINGS(
        Routes.SETTINGS_GRAPH, Routes.SETTINGS, R.string.navigation_label_settings,
        Icons.Rounded.Settings, Icons.Outlined.Settings
    ),
}

/** Re-taps on the current tab, so the screen can scroll back to the top. */
object TopLevelReselect {
    private val _events = kotlinx.coroutines.flow.MutableSharedFlow<TopLevelDestination>(
        extraBufferCapacity = 1
    )
    val events: kotlinx.coroutines.flow.SharedFlow<TopLevelDestination> = _events

    fun emit(destination: TopLevelDestination) {
        _events.tryEmit(destination)
    }
}

/**
 * Material 3 Expressive bottom navigation: a short (flexible) navigation bar whose
 * selected icon fills in and springs, and which slides away on detail screens.
 */
@Composable
fun StealthNavigationBar(
    visible: Boolean,
    selected: TopLevelDestination?,
    onSelect: (TopLevelDestination) -> Unit,
    onReselect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = MaterialTheme.motionScheme
    // ShortNavigationBar (material3 1.5) coerces each item's width up to the bar's minimum
    // width, so a fixed-width bar (e.g. fillMaxWidth) shows only its first item. The Box
    // relaxes minimum constraints, and the bar itself must not get a size modifier.
    Box(modifier) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(motion.defaultSpatialSpec()) { it } + fadeIn(motion.defaultEffectsSpec()),
            exit = slideOutVertically(motion.fastSpatialSpec()) { it } + fadeOut(motion.fastEffectsSpec()),
        ) {
            ShortNavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    val isSelected = destination == selected
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.12f else 1f,
                        animationSpec = motion.fastSpatialSpec(),
                        label = "navIconScale",
                    )
                    ShortNavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (isSelected) onReselect(destination) else onSelect(destination)
                        },
                        icon = {
                            Crossfade(isSelected, animationSpec = motion.fastEffectsSpec(), label = "navIcon") { filled ->
                                Icon(
                                    imageVector = if (filled) destination.selectedIcon else destination.unselectedIcon,
                                    contentDescription = null,
                                    modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale },
                                )
                            }
                        },
                        label = { Text(stringResource(destination.label)) },
                    )
                }
            }
        }
    }
}
