package com.cosmos.unreddit.ui.preferences

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NoAdultContent
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmos.unreddit.R
import com.cosmos.unreddit.UiViewModel
import com.cosmos.unreddit.data.model.preferences.BrandingPreferences
import com.cosmos.unreddit.data.model.preferences.UiPreferences
import com.cosmos.unreddit.ui.common.compose.listPadding
import com.cosmos.unreddit.ui.navigation.Routes
import com.cosmos.unreddit.ui.theme.StealthTheme
import com.cosmos.unreddit.ui.theme.supportsDynamicColor
import com.cosmos.unreddit.ui.common.compose.stealthApp

@Composable
fun SettingsScreen(
    onNavigate: (String) -> Unit,
    onShowPolicy: () -> Unit,
    onColorSchemeChanged: () -> Unit,
    viewModel: PreferencesViewModel = hiltViewModel(),
    uiViewModel: UiViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val app = context.stealthApp
    val nightMode by viewModel.nightMode.collectAsStateWithLifecycle(initialValue = UiPreferences.NightMode.FOLLOW_SYSTEM.mode)
    val showNsfw by viewModel.showNsfw.collectAsStateWithLifecycle(initialValue = false)
    val showNsfwPreview by viewModel.showNsfwPreview.collectAsStateWithLifecycle(initialValue = false)
    val showSpoilerPreview by viewModel.showSpoilerPreview.collectAsStateWithLifecycle(initialValue = false)
    val privacyEnabled by viewModel.privacyEnhancerEnabled.collectAsStateWithLifecycle(initialValue = false)
    var showNightDialog by remember { mutableStateOf(false) }
    var scheme by remember { mutableStateOf(app.colorScheme) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val nightLabels = stringArrayResource(R.array.pref_night_mode_labels)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.navigation_label_settings)) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(top = padding.calculateTopPadding()).fillMaxSize(),
            contentPadding = listPadding(),
        ) {
            section(R.string.settings_appearance) {
                item {
                    SettingRow(
                        icon = Icons.Rounded.DarkMode,
                        title = stringResource(R.string.preference_night_mode),
                        summary = UiPreferences.NightMode.asIndex(nightMode)?.let { nightLabels.getOrNull(it) },
                        index = 0, count = 2,
                        onClick = { showNightDialog = true },
                    )
                }
                item {
                    ColorSchemePicker(
                        selected = scheme,
                        index = 1, count = 2,
                        onSelect = { selected ->
                            scheme = selected
                            uiViewModel.setColorScheme(selected)
                            app.applyColorScheme(selected)
                            onColorSchemeChanged()
                        },
                    )
                }
            }
            section(R.string.settings_content) {
                item {
                    SwitchRow(Icons.Rounded.NoAdultContent, stringResource(R.string.preference_show_nsfw), showNsfw, 0, 3) {
                        viewModel.setShowNsfw(it)
                    }
                }
                item {
                    SwitchRow(Icons.Rounded.Visibility, stringResource(R.string.preference_show_nsfw_preview), showNsfwPreview, 1, 3) {
                        viewModel.setShowNsfwPreview(it)
                    }
                }
                item {
                    SwitchRow(Icons.Rounded.VisibilityOff, stringResource(R.string.preference_show_spoiler_preview), showSpoilerPreview, 2, 3) {
                        viewModel.setShowSpoilerPreview(it)
                    }
                }
            }
            section(R.string.settings_data) {
                item {
                    SettingRow(
                        Icons.Rounded.Cloud, stringResource(R.string.preference_reddit_source),
                        stringResource(R.string.settings_reddit_source_summary), 0, 3, onClick = null,
                    )
                }
                item {
                    SettingRow(
                        Icons.Rounded.Shield, stringResource(R.string.preference_privacy_enhancer),
                        stringResource(
                            if (privacyEnabled) R.string.preference_privacy_enhancer_enabled
                            else R.string.preference_privacy_enhancer_disabled
                        ),
                        1, 3,
                    ) { onNavigate(Routes.PRIVACY_ENHANCER) }
                }
                item {
                    SettingRow(
                        Icons.Rounded.SaveAlt, stringResource(R.string.preference_backup),
                        stringResource(R.string.settings_backup_summary), 2, 3,
                    ) { onNavigate(Routes.BACKUP) }
                }
            }
            section(R.string.settings_about) {
                item {
                    SettingRow(
                        Icons.Rounded.Info, stringResource(R.string.preference_about),
                        stringResource(R.string.settings_about_summary), 0, 2,
                    ) { onNavigate(Routes.ABOUT) }
                }
                item {
                    SettingRow(
                        Icons.Rounded.Policy, stringResource(R.string.preference_policy_disclaimer),
                        null, 1, 2, onClick = onShowPolicy,
                    )
                }
            }
        }
    }

    if (showNightDialog) {
        val current = UiPreferences.NightMode.asIndex(nightMode) ?: 3
        AlertDialog(
            onDismissRequest = { showNightDialog = false },
            title = { Text(stringResource(R.string.dialog_night_mode_title)) },
            text = {
                Column {
                    nightLabels.forEachIndexed { index, label ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .selectable(index == current, role = Role.RadioButton) {
                                    UiPreferences.NightMode.asMode(index)?.let { mode ->
                                        viewModel.setNightMode(mode)
                                        app.appTheme = mode
                                        onColorSchemeChanged()
                                    }
                                    showNightDialog = false
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = index == current, onClick = null)
                            Text(label, Modifier.padding(start = 12.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNightDialog = false }) { Text(stringResource(R.string.dialog_cancel)) }
            },
        )
    }
}

private fun LazyListScope.section(title: Int, content: LazyListScope.() -> Unit) {
    item {
        Text(
            stringResource(title),
            style = MaterialTheme.typography.titleSmallEmphasized,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp),
        )
    }
    content()
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    summary: String?,
    index: Int,
    count: Int,
    onClick: (() -> Unit)?,
) {
    SegmentedListItem(
        onClick = onClick ?: {},
        enabled = true,
        shapes = ListItemDefaults.segmentedShapes(index, count),
        leadingContent = { Icon(icon, null) },
        supportingContent = summary?.let { { Text(it) } },
        trailingContent = onClick?.let { { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null) } },
        modifier = Modifier.padding(bottom = ListItemDefaults.SegmentedGap),
    ) { Text(title) }
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    index: Int,
    count: Int,
    onChange: (Boolean) -> Unit,
) {
    SegmentedListItem(
        checked = checked,
        onCheckedChange = onChange,
        shapes = ListItemDefaults.segmentedShapes(index, count),
        leadingContent = { Icon(icon, null) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                thumbContent = if (checked) {
                    { Icon(Icons.Rounded.Check, null, Modifier.size(SwitchDefaults.IconSize)) }
                } else null,
            )
        },
        modifier = Modifier.padding(bottom = ListItemDefaults.SegmentedGap),
    ) { Text(title) }
}

/** Palette swatches; the chosen one morphs into a cookie shape. */
@Composable
private fun ColorSchemePicker(
    selected: BrandingPreferences.ColorScheme,
    index: Int,
    count: Int,
    onSelect: (BrandingPreferences.ColorScheme) -> Unit,
) {
    val labels = stringArrayResource(R.array.brand_color_scheme_labels)
    val options = BrandingPreferences.ColorScheme.values().filter {
        it != BrandingPreferences.ColorScheme.DYNAMIC || supportsDynamicColor
    }
    SegmentedListItem(
        onClick = {},
        shapes = ListItemDefaults.segmentedShapes(index, count),
        leadingContent = { Icon(Icons.Rounded.Palette, null) },
        supportingContent = {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 12.dp),
            ) {
                items(options) { option ->
                    val label = if (option == BrandingPreferences.ColorScheme.DYNAMIC) {
                        stringResource(R.string.brand_color_scheme_dynamic)
                    } else labels[option.ordinal]
                    Swatch(option, label, option == selected) { onSelect(option) }
                }
            }
        },
        modifier = Modifier.padding(bottom = ListItemDefaults.SegmentedGap),
    ) { Text(stringResource(R.string.brand_color_scheme_title)) }
}

@Composable
private fun Swatch(
    scheme: BrandingPreferences.ColorScheme,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val app = LocalContext.current.stealthApp
    val size by animateDpAsState(if (selected) 64.dp else 56.dp, MaterialTheme.motionScheme.fastSpatialSpec(), label = "swatch")
    val shape = if (selected) MaterialShapes.Cookie9Sided.toShape() else MaterialShapes.Circle.toShape()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(76.dp).clip(MaterialTheme.shapes.medium).clickable(onClick = onClick).padding(4.dp),
    ) {
        // Preview each palette in its own colors.
        StealthTheme(scheme = scheme, amoled = app.isAmoled) {
            Box(
                Modifier
                    .size(size)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(2.dp, MaterialTheme.colorScheme.primary, shape),
                contentAlignment = Alignment.Center,
            ) {
                Row {
                    Box(Modifier.size(14.dp).clip(MaterialShapes.Circle.toShape()).background(MaterialTheme.colorScheme.primary))
                    Box(Modifier.size(14.dp).clip(MaterialShapes.Circle.toShape()).background(MaterialTheme.colorScheme.secondary))
                    Box(Modifier.size(14.dp).clip(MaterialShapes.Circle.toShape()).background(MaterialTheme.colorScheme.tertiary))
                }
                if (selected) {
                    Icon(
                        Icons.Rounded.Check, null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.align(Alignment.TopEnd).size(16.dp),
                    )
                }
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
