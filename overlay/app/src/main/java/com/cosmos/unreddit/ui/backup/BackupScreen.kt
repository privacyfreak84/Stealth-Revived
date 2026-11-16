package com.cosmos.unreddit.ui.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.Resource
import com.cosmos.unreddit.data.model.backup.BackupType
import com.cosmos.unreddit.ui.common.compose.BackButton

enum class BackupOperation { BACKUP, RESTORE }

private enum class Step { OPERATION, TYPE, FILE, RUNNING }

/** Back up or restore profiles, subscriptions and saved items. */
@Composable
fun BackupScreen(onBack: () -> Unit, viewModel: BackupViewModel = hiltViewModel()) {
    val operation by viewModel.operation.collectAsStateWithLifecycle()
    val type by viewModel.backupType.collectAsStateWithLifecycle()
    val uri by viewModel.chosenUri.collectAsStateWithLifecycle()
    val status by viewModel.operationStatus.collectAsStateWithLifecycle()
    var step by rememberSaveable { mutableStateOf(Step.OPERATION) }

    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {
        viewModel.setUri(it)
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { viewModel.setUri(it) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text(stringResource(R.string.fragment_backup_label)) },
                navigationIcon = {
                    BackButton {
                        when (step) {
                            Step.OPERATION, Step.RUNNING -> onBack()
                            Step.TYPE -> step = Step.OPERATION
                            Step.FILE -> step = if (operation == BackupOperation.RESTORE) Step.TYPE else Step.OPERATION
                        }
                    }
                },
            )
        },
    ) { padding ->
        AnimatedContent(step, label = "backupStep", modifier = Modifier.padding(padding).fillMaxSize()) { current ->
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                when (current) {
                    Step.OPERATION -> {
                        Heading(stringResource(R.string.backup_choose_operation))
                        Choice(Icons.Rounded.CloudUpload, stringResource(R.string.backup_operation_backup), 0, 2) {
                            viewModel.setOperation(BackupOperation.BACKUP)
                            viewModel.setBackupType(BackupType.STEALTH)
                            viewModel.setUri(null)
                            step = Step.FILE
                        }
                        Choice(Icons.Rounded.CloudDownload, stringResource(R.string.backup_operation_restore), 1, 2) {
                            viewModel.setOperation(BackupOperation.RESTORE)
                            viewModel.setUri(null)
                            step = Step.TYPE
                        }
                    }
                    Step.TYPE -> {
                        Heading(stringResource(R.string.backup_choose_type))
                        BackupType.values().forEachIndexed { index, backupType ->
                            SegmentedListItem(
                                onClick = { viewModel.setBackupType(backupType); step = Step.FILE },
                                shapes = ListItemDefaults.segmentedShapes(index, BackupType.values().size),
                                leadingContent = {
                                    Icon(painterResource(backupType.icon), null, Modifier.size(28.dp))
                                },
                            ) { Text(backupType.displayName) }
                        }
                    }
                    Step.FILE -> {
                        val explanation = when {
                            operation == BackupOperation.BACKUP -> R.string.backup_location_explanation_backup
                            type == BackupType.REDDIT -> R.string.backup_location_explanation_restore_reddit
                            else -> R.string.backup_location_explanation_restore_stealth
                        }
                        Heading(stringResource(explanation))
                        OutlinedButton(
                            onClick = {
                                if (operation == BackupOperation.BACKUP) create.launch("stealth_backup.json")
                                else open.launch(arrayOf("application/json", "application/octet-stream"))
                            },
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Rounded.Description, null, Modifier.size(ButtonDefaults.IconSize))
                            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                            Text(
                                if (uri != null) stringResource(R.string.backup_file_chosen)
                                else stringResource(R.string.backup_location_button_pick)
                            )
                        }
                        Button(
                            onClick = { viewModel.runOperation(); step = Step.RUNNING },
                            enabled = uri != null,
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        ) { Text(stringResource(R.string.backup_button_lets_go)) }
                    }
                    Step.RUNNING -> Status(status, operation, type, onFinish = onBack)
                }
            }
        }
    }
}

@Composable
private fun Heading(text: String) {
    Text(text, style = MaterialTheme.typography.titleLargeEmphasized, modifier = Modifier.padding(bottom = 16.dp))
}

@Composable
private fun Choice(icon: ImageVector, label: String, index: Int, count: Int, onClick: () -> Unit) {
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(index, count),
        leadingContent = { Icon(icon, null) },
    ) { Text(label, style = MaterialTheme.typography.titleMedium) }
}

@Composable
private fun Status(
    status: Resource<List<com.cosmos.unreddit.data.model.backup.Profile>>,
    operation: BackupOperation?,
    type: BackupType?,
    onFinish: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        when (status) {
            is Resource.Loading -> ContainedLoadingIndicator(Modifier.size(96.dp))
            is Resource.Success -> {
                Badge(Icons.Rounded.CheckCircle, success = true)
                Text(
                    stringResource(
                        when {
                            operation == BackupOperation.BACKUP -> R.string.backup_loading_success_backup
                            type == BackupType.REDDIT -> R.string.backup_loading_success_restore_reddit
                            else -> R.string.backup_loading_success_restore_stealth
                        }
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                status.data.forEach { profile ->
                    Text(
                        "${profile.name} · ${profile.subscriptions.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(onClick = onFinish, shapes = ButtonDefaults.shapes()) {
                    Text(stringResource(R.string.backup_button_finish))
                }
            }
            is Resource.Error -> {
                Badge(Icons.Rounded.ErrorOutline, success = false)
                Text(
                    stringResource(
                        when {
                            operation == BackupOperation.BACKUP -> R.string.backup_loading_failed_backup
                            type == BackupType.REDDIT -> R.string.backup_loading_failed_restore_reddit
                            else -> R.string.backup_loading_failed_restore_stealth
                        }
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Button(onClick = onFinish, shapes = ButtonDefaults.shapes()) {
                    Text(stringResource(R.string.backup_button_finish))
                }
            }
        }
    }
}

@Composable
private fun Badge(icon: ImageVector, success: Boolean) {
    Surface(
        shape = MaterialShapes.SoftBurst.toShape(),
        color = if (success) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
        contentColor = if (success) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.size(112.dp),
    ) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(48.dp)) } }
}

