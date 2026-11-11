package com.cosmos.unreddit.ui.profilemanager

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.ProfileItem
import com.cosmos.unreddit.data.model.db.Profile

private const val NAME_MIN = 3
private const val NAME_MAX = 20

/** Create, rename, select and delete local profiles. */
@Composable
fun ProfileManagerSheet(
    onDismiss: () -> Unit,
    viewModel: ProfileManagerViewModel = hiltViewModel(),
) {
    val items by viewModel.profiles.collectAsStateWithLifecycle(initialValue = emptyList())
    val profiles = items.filterIsInstance<ProfileItem.UserProfile>().map { it.profile }
    var editing by remember { mutableStateOf<Profile?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Profile?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(true)) {
        Column(
            Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            Text(
                stringResource(R.string.action_manage_profiles),
                style = MaterialTheme.typography.headlineSmallEmphasized,
                modifier = Modifier.padding(start = 8.dp, bottom = 12.dp),
            )
            val count = profiles.size + 1
            profiles.forEachIndexed { index, profile ->
                SegmentedListItem(
                    onClick = { viewModel.selectProfile(profile); onDismiss() },
                    shapes = ListItemDefaults.segmentedShapes(index, count),
                    leadingContent = { Icon(Icons.Rounded.Person, null) },
                    trailingContent = {
                        Row {
                            IconButton(onClick = { editing = profile }, shapes = IconButtonDefaults.shapes()) {
                                Icon(Icons.Rounded.Edit, stringResource(R.string.action_rename))
                            }
                            if (profile.canDelete) {
                                IconButton(onClick = { deleting = profile }, shapes = IconButtonDefaults.shapes()) {
                                    Icon(Icons.Rounded.Delete, stringResource(R.string.action_delete))
                                }
                            }
                        }
                    },
                ) { Text(profile.name) }
            }
            SegmentedListItem(
                onClick = { creating = true },
                shapes = ListItemDefaults.segmentedShapes(count - 1, count),
                leadingContent = { Icon(Icons.Rounded.Add, null) },
            ) { Text(stringResource(R.string.profile_manager_add_profile)) }
        }
    }

    if (creating) {
        ProfileNameDialog(
            title = stringResource(R.string.dialog_create_profile_title),
            confirm = stringResource(R.string.dialog_create_profile_button),
            initial = "",
            existing = profiles,
            onConfirm = { viewModel.addProfile(it); creating = false },
            onDismiss = { creating = false },
        )
    }
    editing?.let { profile ->
        ProfileNameDialog(
            title = stringResource(R.string.dialog_rename_profile_title),
            confirm = stringResource(R.string.dialog_rename_profile_button),
            initial = profile.name,
            existing = profiles - profile,
            onConfirm = { viewModel.renameProfile(profile, it); editing = null },
            onDismiss = { editing = null },
        )
    }
    deleting?.let { profile ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.dialog_delete_profile_title)) },
            text = { Text(stringResource(R.string.dialog_delete_profile_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteProfile(profile); deleting = null }) {
                    Text(stringResource(R.string.dialog_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.dialog_no)) }
            },
        )
    }
}

@Composable
private fun ProfileNameDialog(
    title: String,
    confirm: String,
    initial: String,
    existing: List<Profile>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    var error by remember { mutableStateOf<Int?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; error = null },
                label = { Text(stringResource(R.string.dialog_create_profile_hint)) },
                isError = error != null,
                supportingText = error?.let { { Text(stringResource(it)) } },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val trimmed = name.trim()
                error = when {
                    trimmed.isBlank() -> R.string.profile_blank_error
                    trimmed.length !in NAME_MIN..NAME_MAX -> R.string.profile_name_length_error
                    existing.any { it.name.equals(trimmed, true) } -> R.string.profile_already_exists_error
                    else -> null
                }
                if (error == null) onConfirm(trimmed)
            }) { Text(confirm) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } },
    )
}
