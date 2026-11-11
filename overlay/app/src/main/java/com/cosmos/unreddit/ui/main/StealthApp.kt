package com.cosmos.unreddit.ui.main

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cosmos.unreddit.R
import com.cosmos.unreddit.UiViewModel
import com.cosmos.unreddit.ui.about.AboutScreen
import com.cosmos.unreddit.ui.backup.BackupScreen
import com.cosmos.unreddit.ui.common.compose.LinkActions
import com.cosmos.unreddit.ui.common.compose.LinkRedirectDialog
import com.cosmos.unreddit.ui.common.compose.LocalLinkActions
import com.cosmos.unreddit.ui.mediaviewer.MediaViewerScreen
import com.cosmos.unreddit.ui.navigation.NavHandoff
import com.cosmos.unreddit.ui.navigation.Routes
import com.cosmos.unreddit.ui.policydisclaimer.PolicyDisclaimerDialog
import com.cosmos.unreddit.ui.postdetails.PostDetailsScreen
import com.cosmos.unreddit.ui.postlist.HomeScreen
import com.cosmos.unreddit.ui.preferences.SettingsScreen
import com.cosmos.unreddit.ui.privacyenhancer.PrivacyEnhancerScreen
import com.cosmos.unreddit.ui.profile.ProfileScreen
import com.cosmos.unreddit.ui.search.SearchScreen
import com.cosmos.unreddit.ui.subreddit.SubredditScreen
import com.cosmos.unreddit.ui.subreddit.SubredditSearchScreen
import com.cosmos.unreddit.ui.subscriptions.SubscriptionsScreen
import com.cosmos.unreddit.ui.user.UserScreen
import com.cosmos.unreddit.util.extension.isPast
import kotlinx.coroutines.delay
import java.util.Calendar

/** Space screens leave at the bottom so content clears the navigation bar. */
val LocalBottomBarPadding = compositionLocalOf<Dp> { 0.dp }

private val NavigationBarHeight = 64.dp

/** Root of the app: navigation graph, Expressive bottom bar, and global snackbars. */
@Composable
fun StealthApp(onColorSchemeChanged: () -> Unit) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val links = remember(navController) { LinkActions(context, navController) }
    val uiViewModel: UiViewModel = hiltViewModel()

    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val topLevel = destination?.route in Routes.topLevel
    val selected = TopLevelDestination.entries.firstOrNull { item ->
        destination?.hierarchy?.any { it.route == item.graphRoute } == true
    }

    // Hide the bar while scrolling down a top-level screen, bring it back on the way up.
    var barScrolledAway by rememberSaveable { mutableStateOf(false) }
    val scrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -12f) barScrolledAway = true
                else if (available.y > 12f) barScrolledAway = false
                return Offset.Zero
            }
        }
    }
    LaunchedEffect(destination?.route) { barScrolledAway = false }

    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomPadding = if (topLevel) NavigationBarHeight + navInset else navInset
    val snackbarHost = remember { SnackbarHostState() }
    var showPolicy by remember { mutableStateOf(false) }

    PolicySnackbar(uiViewModel, snackbarHost) { showPolicy = true }

    CompositionLocalProvider(
        LocalLinkActions provides links,
        LocalBottomBarPadding provides bottomPadding,
    ) {
        Box(Modifier.fillMaxSize().nestedScroll(scrollConnection)) {
            NavHost(
                navController = navController,
                startDestination = Routes.HOME_GRAPH,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { fadeIn(tween()) + scaleIn(initialScale = 0.96f) },
                exitTransition = { fadeOut(tween()) },
                popEnterTransition = { fadeIn(tween()) },
                popExitTransition = { fadeOut(tween()) + scaleOut(targetScale = 0.96f) },
            ) {
                topLevelGraph(Routes.HOME_GRAPH, Routes.HOME) { HomeScreen() }
                topLevelGraph(Routes.SUBSCRIPTIONS_GRAPH, Routes.SUBSCRIPTIONS) { SubscriptionsScreen() }
                topLevelGraph(Routes.PROFILE_GRAPH, Routes.PROFILE) { ProfileScreen() }
                navigation(startDestination = Routes.SETTINGS, route = Routes.SETTINGS_GRAPH) {
                    composable(Routes.SETTINGS) {
                        SettingsScreen(
                            onNavigate = { links.navigate(it) },
                            onShowPolicy = { showPolicy = true },
                            onColorSchemeChanged = onColorSchemeChanged,
                        )
                    }
                    detail(Routes.ABOUT) { AboutScreen(onBack = navController::navigateUp) }
                    detail(Routes.PRIVACY_ENHANCER) { PrivacyEnhancerScreen(onBack = navController::navigateUp) }
                    detail(Routes.BACKUP) { BackupScreen(onBack = navController::navigateUp) }
                }
                detail(
                    Routes.POST,
                    navArgument("permalink") { type = NavType.StringType },
                    navArgument("thread") { type = NavType.BoolType; defaultValue = false },
                ) { entry ->
                    val permalink = entry.arguments?.getString("permalink").orEmpty()
                    PostDetailsScreen(
                        permalink = permalink,
                        singleThread = entry.arguments?.getBoolean("thread") == true,
                        initialPost = NavHandoff.peekPost(permalink),
                        onBack = navController::navigateUp,
                    )
                }
                detail(Routes.SUBREDDIT, navArgument("name") { type = NavType.StringType }) { entry ->
                    SubredditScreen(entry.arguments?.getString("name").orEmpty(), onBack = navController::navigateUp)
                }
                detail(Routes.SUBREDDIT_SEARCH, navArgument("name") { type = NavType.StringType }) { entry ->
                    SubredditSearchScreen(entry.arguments?.getString("name").orEmpty(), onBack = navController::navigateUp)
                }
                detail(Routes.USER, navArgument("name") { type = NavType.StringType }) { entry ->
                    UserScreen(entry.arguments?.getString("name").orEmpty(), onBack = navController::navigateUp)
                }
                detail(
                    Routes.SEARCH,
                    navArgument("query") { type = NavType.StringType; defaultValue = "" },
                ) { entry ->
                    SearchScreen(entry.arguments?.getString("query").orEmpty(), onBack = navController::navigateUp)
                }
                composable(
                    Routes.MEDIA,
                    arguments = listOf(navArgument("key") { type = NavType.StringType }),
                    enterTransition = { fadeIn(tween()) + scaleIn(initialScale = 0.9f) },
                    popExitTransition = { fadeOut(tween()) + scaleOut(targetScale = 0.9f) },
                ) { entry ->
                    MediaViewerScreen(
                        request = NavHandoff.getMedia(entry.arguments?.getString("key").orEmpty()),
                        onClose = navController::navigateUp,
                    )
                }
            }

            StealthNavigationBar(
                visible = topLevel && !barScrolledAway,
                selected = selected,
                onSelect = { item ->
                    navController.navigate(item.graphRoute) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onReselect = { item ->
                    navController.popBackStack(item.startRoute, inclusive = false)
                    TopLevelReselect.emit(item)
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )

            SnackbarHost(
                snackbarHost,
                Modifier.align(Alignment.BottomCenter).padding(bottom = bottomPadding),
            )
        }
        LinkRedirectDialog(links)
        if (showPolicy) PolicyDisclaimerDialog(onDismiss = { showPolicy = false })
    }
}

@Composable
private fun PolicySnackbar(
    uiViewModel: UiViewModel,
    host: SnackbarHostState,
    onLearnMore: () -> Unit,
) {
    val shown by uiViewModel.policyDisclaimerShown.collectAsStateWithLifecycle(initialValue = true)
    val message = stringResource(R.string.snackbar_policy_disclaimer_message, stringResource(R.string.brand_app_name))
    val action = stringResource(R.string.snackbar_policy_disclaimer_action)
    LaunchedEffect(shown) {
        val date = Calendar.getInstance().apply { set(2023, 5, 10) }.timeInMillis
        if (!shown && date.isPast) {
            delay(5_000)
            if (host.showSnackbar(message, action, withDismissAction = true) == SnackbarResult.ActionPerformed) {
                onLearnMore()
            }
        }
    }
}

private fun <T> tween() = androidx.compose.animation.core.tween<T>(durationMillis = 280)

private fun NavGraphBuilder.topLevelGraph(
    graphRoute: String,
    startRoute: String,
    content: @Composable () -> Unit,
) {
    navigation(startDestination = startRoute, route = graphRoute) {
        composable(
            startRoute,
            enterTransition = { fadeIn(tween()) },
            exitTransition = { fadeOut(tween()) },
            popEnterTransition = { fadeIn(tween()) },
            popExitTransition = { fadeOut(tween()) },
        ) { content() }
    }
}

/** Detail screens slide in from the end, like a stack. */
private fun NavGraphBuilder.detail(
    route: String,
    vararg arguments: androidx.navigation.NamedNavArgument,
    content: @Composable (androidx.navigation.NavBackStackEntry) -> Unit,
) {
    composable(
        route,
        arguments = arguments.toList(),
        enterTransition = { slideInHorizontally(tween()) { it / 4 } + fadeIn(tween()) },
        exitTransition = { fadeOut(tween()) },
        popEnterTransition = { fadeIn(tween()) },
        popExitTransition = { slideOutHorizontally(tween()) { it / 4 } + fadeOut(tween()) },
    ) { entry -> content(entry) }
}
