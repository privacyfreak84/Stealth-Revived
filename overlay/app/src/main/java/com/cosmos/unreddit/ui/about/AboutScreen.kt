package com.cosmos.unreddit.ui.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cosmos.unreddit.BuildConfig
import com.cosmos.unreddit.R
import com.cosmos.unreddit.data.model.CreditItem
import com.cosmos.unreddit.ui.common.compose.BackButton
import com.cosmos.unreddit.ui.common.compose.LocalLinkActions
import com.cosmos.unreddit.ui.common.compose.listPadding

private const val PROJECT_LINK = "https://github.com/privacyfreak84/Stealth-Revived"

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val links = LocalLinkActions.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text(stringResource(R.string.fragment_about_label)) },
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
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Surface(
                        shape = MaterialShapes.Cookie12Sided.toShape(),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(112.dp),
                    ) {
                        Image(
                            painterResource(R.drawable.ic_launcher_foreground), null,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Text(
                        stringResource(R.string.brand_app_name),
                        style = MaterialTheme.typography.headlineMediumEmphasized,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    Text(
                        stringResource(R.string.label_version, BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        stringResource(R.string.app_description),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { links.openBrowser(PROJECT_LINK) }, shapes = ButtonDefaults.shapes()) {
                            Icon(Icons.Rounded.Code, null, Modifier.size(ButtonDefaults.IconSize))
                            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                            Text(stringResource(R.string.label_source_code))
                        }
                    }
                    Text(
                        stringResource(R.string.about_fork_attribution, stringResource(R.string.gitlab_link)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
            creditSection(R.string.about_section_credits, Credits.CREDITS)
            creditSection(R.string.about_section_libraries, Credits.LIBRARIES)
            item { SectionTitle(R.string.about_section_contributors) }
            itemsIndexed(Credits.CONTRIBUTORS) { index, contributor ->
                SegmentedListItem(
                    onClick = { links.openBrowser(contributor.link) },
                    shapes = ListItemDefaults.segmentedShapes(index, Credits.CONTRIBUTORS.size),
                    leadingContent = { Icon(Icons.Rounded.Person, null) },
                    supportingContent = { Text(stringResource(contributor.description)) },
                ) { Text(contributor.name) }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: Int) {
    Text(
        stringResource(title),
        style = MaterialTheme.typography.titleSmallEmphasized,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp),
    )
}

private fun androidx.compose.foundation.lazy.LazyListScope.creditSection(
    title: Int,
    credits: List<CreditItem.Credit>,
) {
    item { SectionTitle(title) }
    itemsIndexed(credits) { index, credit ->
        val links = LocalLinkActions.current
        SegmentedListItem(
            onClick = { links.openBrowser(credit.link) },
            shapes = ListItemDefaults.segmentedShapes(index, credits.size),
            overlineContent = { Text(credit.author) },
            supportingContent = { Text(credit.description) },
            trailingContent = {
                IconButton(onClick = { links.openBrowser(credit.licenseLink) }, shapes = IconButtonDefaults.shapes()) {
                    Icon(Icons.Rounded.Gavel, stringResource(R.string.label_license))
                }
            },
        ) { Text(credit.title) }
    }
}
