package com.cosmos.unreddit.ui.policydisclaimer

import android.text.Spanned
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration
import androidx.core.text.HtmlCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.cosmos.unreddit.R
import com.cosmos.unreddit.ui.common.compose.LocalLinkActions

/** Explains Reddit's 2023 API policy change; marks it as seen when dismissed. */
@Composable
fun PolicyDisclaimerDialog(
    onDismiss: () -> Unit,
    viewModel: PolicyDisclaimerViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val links = LocalLinkActions.current
    val linkColor = MaterialTheme.colorScheme.primary
    val message = remember(linkColor) {
        val text = context.resources.getText(R.string.dialog_policy_disclaimer_message)
        val html = if (text is Spanned) {
            HtmlCompat.toHtml(text, HtmlCompat.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
        } else text.toString()
        AnnotatedString.fromHtml(
            html,
            linkStyles = TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)),
            linkInteractionListener = LinkInteractionListener { link ->
                (link as? LinkAnnotation.Url)?.let { links.openBrowser(it.url) }
            },
        )
    }
    val close = {
        viewModel.setPolicyDisclaimerShown(true)
        onDismiss()
    }
    AlertDialog(
        onDismissRequest = close,
        title = { Text(stringResource(R.string.dialog_policy_disclaimer_title)) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = close) { Text(stringResource(R.string.ok)) } },
    )
}
