package com.cosmos.unreddit.ui.subscriptions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmos.unreddit.R
import com.cosmos.unreddit.ui.common.compose.EmptyState
import com.cosmos.unreddit.ui.common.compose.LocalLinkActions
import com.cosmos.unreddit.ui.common.compose.ShapedAvatar
import com.cosmos.unreddit.ui.common.compose.listPadding
import com.cosmos.unreddit.ui.main.LocalBottomBarPadding
import com.cosmos.unreddit.ui.main.TopLevelDestination
import com.cosmos.unreddit.ui.main.TopLevelReselect
import com.cosmos.unreddit.ui.navigation.Routes

@Composable
fun SubscriptionsScreen(viewModel: SubscriptionsViewModel = hiltViewModel()) {
    val links = LocalLinkActions.current
    val subscriptions by viewModel.filteredSubscriptions.collectAsStateWithLifecycle(initialValue = null)
    var query by rememberSaveable { mutableStateOf("") }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()
    val expandedFab by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

    LaunchedEffect(query) { viewModel.setSearchQuery(query) }
    LaunchedEffect(Unit) {
        TopLevelReselect.events.collect {
            if (it == TopLevelDestination.SUBSCRIPTIONS) listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text(stringResource(R.string.fragment_subscriptions_label)) },
                subtitle = subscriptions?.let { list ->
                    { Text("${list.size}") }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { links.navigate(Routes.search()) },
                expanded = expandedFab,
                icon = { Icon(Icons.Rounded.Search, null) },
                text = { Text(stringResource(R.string.menu_search)) },
                modifier = Modifier.padding(bottom = LocalBottomBarPadding.current),
            )
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(top = padding.calculateTopPadding()).fillMaxSize(),
            contentPadding = listPadding(extraBottom = 72.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            item(key = "filter") {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.subscriptions_filter_hint)) },
                    leadingIcon = { Icon(Icons.Rounded.FilterList, null) },
                    trailingIcon = if (query.isNotEmpty()) {
                        { IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Clear, null) } }
                    } else null,
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                )
            }
            val list = subscriptions.orEmpty()
            if (subscriptions != null && list.isEmpty()) {
                item(key = "empty") { EmptyState(message = stringResource(R.string.empty_data_subscriptions)) }
            }
            itemsIndexed(list, key = { _, it -> it.name }) { index, subscription ->
                SegmentedListItem(
                    onClick = { links.openSubreddit(subscription.name) },
                    shapes = ListItemDefaults.segmentedShapes(index, list.size),
                    leadingContent = { ShapedAvatar(subscription.name, subscription.icon) },
                    modifier = Modifier.animateItem(),
                ) { Text("r/${subscription.name}", style = MaterialTheme.typography.titleMedium) }
            }
        }
    }
}
