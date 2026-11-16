package com.cosmos.unreddit.ui.privacyenhancer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.Resource
import com.cosmos.unreddit.data.model.ServiceExternal
import com.cosmos.unreddit.data.model.db.Redirect
import com.cosmos.unreddit.ui.common.compose.BackButton
import com.cosmos.unreddit.ui.common.compose.ErrorState
import com.cosmos.unreddit.ui.common.compose.LoadingRow
import com.cosmos.unreddit.ui.common.compose.listPadding
import com.cosmos.unreddit.util.LinkValidator

/** Redirect links for supported services to privacy-friendly frontends. */
@Composable
fun PrivacyEnhancerScreen(onBack: () -> Unit, viewModel: PrivacyEnhancerViewModel = hiltViewModel()) {
    val enabled by viewModel.privacyEnhancerEnabled.collectAsStateWithLifecycle(initialValue = false)
    val instances by viewModel.instances.collectAsStateWithLifecycle()
    val services by viewModel.services.collectAsStateWithLifecycle(initialValue = emptyList())
    val redirects by viewModel.redirects.collectAsStateWithLifecycle(initialValue = emptyList())
    var editing by remember { mutableStateOf<ServiceExternal?>(null) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(redirects) { viewModel.updateLinkRedirector(redirects) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text(stringResource(R.string.preference_privacy_enhancer)) },
                subtitle = { Text(stringResource(R.string.preference_privacy_enhancer_summary)) },
                navigationIcon = { BackButton(onBack) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(top = padding.calculateTopPadding()).fillMaxSize(),
            contentPadding = listPadding(),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            item {
                SegmentedListItem(
                    checked = enabled,
                    onCheckedChange = viewModel::setPrivacyEnhancerEnabled,
                    shapes = ListItemDefaults.segmentedShapes(0, 1),
                    leadingContent = { Icon(Icons.Rounded.Shield, null) },
                    trailingContent = {
                        Switch(
                            checked = enabled, onCheckedChange = null,
                            thumbContent = if (enabled) {
                                { Icon(Icons.Rounded.Check, null, Modifier.size(SwitchDefaults.IconSize)) }
                            } else null,
                        )
                    },
                    modifier = Modifier.padding(bottom = 16.dp),
                ) {
                    Text(
                        stringResource(
                            if (enabled) R.string.preference_privacy_enhancer_enabled
                            else R.string.preference_privacy_enhancer_disabled
                        ),
                        style = MaterialTheme.typography.titleMediumEmphasized,
                    )
                }
            }
            when (val state = instances) {
                is Resource.Loading -> item { LoadingRow() }
                is Resource.Error -> item { ErrorState() }
                is Resource.Success -> itemsIndexed(services, key = { _, it -> it.service }) { index, service ->
                    val redirect = redirects.find { it.service == service.service }
                    val mode = redirect?.mode ?: Redirect.RedirectMode.OFF
                    SegmentedListItem(
                        onClick = { editing = service },
                        enabled = enabled,
                        shapes = ListItemDefaults.segmentedShapes(index, services.size),
                        supportingContent = {
                            Text(
                                if (mode == Redirect.RedirectMode.OFF) stringResource(mode.label)
                                else stringResource(
                                    R.string.preference_privacy_enhancer_service_summary,
                                    stringResource(mode.label), redirect?.redirect.orEmpty()
                                )
                            )
                        },
                    ) { Text(service.name ?: service.service) }
                }
            }
        }
    }

    editing?.let { service ->
        ServiceDialog(
            service = service,
            saved = redirects.find { it.service == service.service },
            onSave = { viewModel.updateRedirect(it); editing = null },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun ServiceDialog(
    service: ServiceExternal,
    saved: Redirect?,
    onSave: (Redirect) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = service.redirect.flatMap { it.instances }
    var mode by remember { mutableStateOf(saved?.mode ?: Redirect.RedirectMode.OFF) }
    var instance by remember { mutableStateOf(saved?.redirect?.takeIf { it.isNotBlank() } ?: options.firstOrNull().orEmpty()) }
    var error by remember { mutableStateOf<Int?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(service.name ?: service.service) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val modes = Redirect.RedirectMode.values()
                Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                    modes.forEachIndexed { index, item ->
                        ToggleButton(
                            checked = mode == item,
                            onCheckedChange = { mode = item },
                            shapes = when (index) {
                                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                modes.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            },
                            modifier = Modifier.weight(1f),
                        ) { Text(stringResource(item.label), maxLines = 1) }
                    }
                }
                ExposedDropdownMenuBox(expanded = menuOpen, onExpandedChange = { menuOpen = it }) {
                    OutlinedTextField(
                        value = instance,
                        onValueChange = { instance = it; error = null },
                        label = { Text(stringResource(R.string.instance_hint)) },
                        isError = error != null,
                        supportingText = error?.let { { Text(stringResource(it)) } },
                        singleLine = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(menuOpen) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                    )
                    ExposedDropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        options.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = { instance = option; menuOpen = false; error = null },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val trimmed = instance.trim()
                error = when {
                    !mode.isEnabled -> null
                    trimmed.isBlank() -> R.string.instance_empty_error
                    !LinkValidator(trimmed).isValid -> R.string.instance_invalid_error
                    else -> null
                }
                if (error == null) {
                    onSave(
                        Redirect(
                            pattern = service.pattern,
                            redirect = trimmed.ifEmpty { options.firstOrNull().orEmpty() },
                            service = service.service,
                            mode = mode,
                        )
                    )
                }
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } },
    )
}
