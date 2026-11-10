package com.cosmos.unreddit.util

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.view.Gravity
import androidx.core.text.HtmlCompat
import com.cosmos.unreddit.data.model.Block.TableBlock
import com.cosmos.unreddit.data.model.Block.TextBlock
import com.cosmos.unreddit.data.model.HtmlBlock
import com.cosmos.unreddit.data.model.RedditText
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.util.LinkedList

/**
 * A lightweight marker that preserves an HTML image URL while HtmlCompat builds
 * the comment's Spanned text. RedditTextView replaces it with a Coil-loaded image.
 */
class InlineImagePlaceholderDrawable(val imageUrl: String) : Drawable() {
    override fun draw(canvas: Canvas) = Unit
    override fun setAlpha(alpha: Int) = Unit
    override fun setColorFilter(colorFilter: ColorFilter?) = Unit
    @Deprecated("Deprecated in Android")
    override fun getOpacity(): Int = PixelFormat.TRANSPARENT
}

class HtmlParser(private val defaultDispatcher: CoroutineDispatcher) {

    private val TABLE_REGEX = Regex("<table>.*?</table>", RegexOption.DOT_MATCHES_ALL)
    private val CODE_REGEX = Regex("<pre><code>(.*?)</code></pre>", RegexOption.DOT_MATCHES_ALL)
    private val PLACEHOLDER_REGEX = Regex("<(table|code)_placeholder/>")

    private val tagHandler = RedditTagHandler()

    suspend fun separateHtmlBlocks(html: String?): RedditText = withContext(defaultDispatcher) {
        val redditText = RedditText()
        if (html == null) return@withContext redditText

        val tables = LinkedList<String>()
        val codes = LinkedList<String>()
        var newHtml = html

        newHtml = newHtml.replace(TABLE_REGEX) {
            tables.add(it.groupValues[0])
            TABLE_PLACEHOLDER
        }
        newHtml = newHtml.replace(CODE_REGEX) {
            codes.add(it.groupValues[1])
            CODE_PLACEHOLDER
        }

        if (PLACEHOLDER_REGEX.containsMatchIn(newHtml)) {
            var lastIndex = 0
            for (match in PLACEHOLDER_REGEX.findAll(newHtml)) {
                if (match.range.first > 0) {
                    val previousBlock = newHtml.substring(lastIndex, match.range.first - 1)
                    redditText.addBlock(getTextBlock(previousBlock), HtmlBlock.BlockType.TEXT)
                }
                lastIndex = match.range.last + 1
                if (match.value == TABLE_PLACEHOLDER) {
                    redditText.addBlock(getTableFromHtmlTable(tables.pop()), HtmlBlock.BlockType.TABLE)
                } else if (match.value == CODE_PLACEHOLDER) {
                    val codeBlock = Parser.unescapeEntities(codes.pop(), true)
                    redditText.addBlock(TextBlock(codeBlock), HtmlBlock.BlockType.CODE)
                }
            }
            if (lastIndex < newHtml.length) {
                redditText.addBlock(getTextBlock(newHtml.substring(lastIndex)), HtmlBlock.BlockType.TEXT)
            }
        } else {
            redditText.addBlock(getTextBlock(newHtml), HtmlBlock.BlockType.TEXT)
        }
        return@withContext redditText
    }

    private fun getTableFromHtmlTable(html: String): TableBlock {
        val table = TableBlock()
        val rows = Jsoup.parse(html).select("tr")
        for (row in rows) {
            val cells = if (row.select("td").isNotEmpty()) row.select("td") else row.select("th")
            val tableRow = TableBlock.Row(row.select("td").isEmpty())
            for (cell in cells) {
                tableRow.addColumn(
                    fromHtml(cell.html()),
                    getGravityFromAlign(cell.attr("align"))
                )
            }
            table.addRow(tableRow)
        }
        return table
    }

    private fun getGravityFromAlign(align: String): Int = when (align) {
        "left" -> Gravity.START
        "right" -> Gravity.END
        "center" -> Gravity.CENTER
        else -> Gravity.START
    }

    private fun getTextBlock(html: String): TextBlock = TextBlock(fromHtml(html))

    private fun fromHtml(html: String): CharSequence {
        val overriddenHtml = tagHandler.overrideTags(html)
        return HtmlCompat.fromHtml(
            overriddenHtml,
            HtmlCompat.FROM_HTML_MODE_LEGACY,
            { source ->
                source.takeIf {
                    it.startsWith("https://", true) || it.startsWith("http://", true)
                }?.let { InlineImagePlaceholderDrawable(it) }
            },
            tagHandler
        ).removeSuffix("\n\n")
    }

    companion object {
        private const val TABLE_PLACEHOLDER = "<table_placeholder/>"
        private const val CODE_PLACEHOLDER = "<code_placeholder/>"
    }
}
