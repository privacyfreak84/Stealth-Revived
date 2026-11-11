package com.cosmos.unreddit.ui.postlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.db.Profile
import com.cosmos.unreddit.data.model.preferences.ContentPreferences
import com.cosmos.unreddit.ui.common.compose.LocalLinkActions
import com.cosmos.unreddit.ui.common.compose.PostFeed
import com.cosmos.unreddit.ui.common.compose.SortContext
import com.cosmos.unreddit.ui.common.compose.SortSheet
import com.cosmos.unreddit.ui.common.compose.listPadding
import com.cosmos.unreddit.ui.common.compose.sortingLabel
import com.cosmos.unreddit.ui.main.TopLevelDestination
import com.cosmos.unreddit.ui.main.TopLevelReselect
import com.cosmos.unreddit.ui.navigation.Routes
import com.cosmos.unreddit.ui.profilemanager.ProfileManagerSheet

val DefaultContentPreferences = ContentPreferences(
    showNsfw = false, showNsfwPreview = false, showSpoilerPreview = false
)

private val BrandFont = FontFamily(Font(R.font.stealth_medium))

/** The home feed: posts from the current profile's subscriptions (or r/popular). */
@Composable
fun HomeScreen(viewModel: PostListViewModel = hiltViewModel()) {
    val links = LocalLinkActions.current
    val posts = viewModel.postDataFlow.collectAsLazyPagingItems()
    val preferences by viewModel.contentPreferences.collectAsStateWithLifecycle(DefaultContentPreferences)
    val sorting by viewModel.sorting.collectAsStateWithLifecycle()
    val profile by viewModel.currentProfile.collectAsStateWithLifecycle(initialValue = null)
    val profiles by viewModel.profiles.collectAsStateWithLifecycle(initialValue = emptyList())
    val listState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var showSort by rememberSaveable { mutableStateOf(false) }
    var showProfiles by rememberSaveable { mutableStateOf(false) }
    var showManager by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        TopLevelReselect.events.collect { if (it == TopLevelDestination.HOME) listState.animateScrollToItem(0) }
    }
    LaunchedEffect(sorting) { listState.scrollToItem(0) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.brand_app_name),
                        fontFamily = BrandFont,
                        fontSize = 22.sp,
                    )
                },
                subtitle = {
                    Text(
                        listOfNotNull(profile?.name, sortingLabel(sorting)).joinToString(" · "),
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                actions = {
                    IconButton(onClick = { links.navigate(Routes.search()) }, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.Rounded.Search, stringResource(R.string.menu_search))
                    }
                    IconButton(onClick = { showSort = true }, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.AutoMirrored.Rounded.Sort, stringResource(R.string.sort_title))
                    }
                    IconButton(onClick = { showProfiles = true }, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.Rounded.Person, stringResource(R.string.action_switch_profile))
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
    ) { padding ->
        PostFeed(
            posts = posts,
            preferences = preferences,
            viewModel = viewModel,
            listState = listState,
            contentPadding = listPadding(),
            modifier = Modifier.padding(top = padding.calculateTopPadding()).fillMaxSize(),
        )
    }

    if (showSort) {
        SortSheet(sorting, SortContext.FEED, onDismiss = { showSort = false }, onSort = viewModel::setSorting)
    }
    if (showProfiles) {
        ProfileSwitcherSheet(
            current = profile,
            profiles = profiles,
            onSelect = { viewModel.selectProfile(it); showProfiles = false },
            onManage = { showProfiles = false; showManager = true },
            onDismiss = { showProfiles = false },
        )
    }
    if (showManager) ProfileManagerSheet(onDismiss = { showManager = false })
}

@Composable
private fun ProfileSwitcherSheet(
    current: Profile?,
    profiles: List<Profile>,
    onSelect: (Profile) -> Unit,
    onManage: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(true)) {
        Column(
            Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            Text(
                stringResource(R.string.action_switch_profile),
                style = MaterialTheme.typography.headlineSmallEmphasized,
                modifier = Modifier.padding(start = 8.dp, bottom = 12.dp),
            )
            val count = profiles.size + 1
            profiles.forEachIndexed { index, profile ->
                SegmentedListItem(
                    selected = profile.id == current?.id,
                    onClick = { onSelect(profile) },
                    shapes = ListItemDefaults.segmentedShapes(index, count),
                    leadingContent = { Icon(Icons.Rounded.Person, null) },
                    trailingContent = if (profile.id == current?.id) {
                        { Icon(Icons.Rounded.Check, null) }
                    } else null,
                ) { Text(profile.name) }
            }
            SegmentedListItem(
                onClick = onManage,
                shapes = ListItemDefaults.segmentedShapes(count - 1, count),
                leadingContent = { Icon(Icons.Rounded.ManageAccounts, null) },
                trailingContent = { Icon(Icons.Rounded.Add, null) },
            ) { Text(stringResource(R.string.action_manage_profiles)) }
        }
    }
}
