package com.cosmos.unreddit.ui.common.widget

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Animatable
import android.text.Spanned
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import coil.imageLoader
import coil.request.ImageRequest
import com.cosmos.unreddit.util.InlineImagePlaceholderDrawable

class RedditTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var scrollEnabled: Boolean = false

    init {
        highlightColor = Color.TRANSPARENT
        isClickable = false
        isFocusable = false
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
    }

    fun setText(text: CharSequence, enableScroll: Boolean) {
        scrollEnabled = enableScroll
        this.text = text
        loadInlineImages()
    }

    fun loadInlineImages() {
        val spanned = text as? Spanned ?: return
        val placeholders = spanned.getSpans(0, spanned.length, ImageSpan::class.java)
            .mapNotNull { span ->
                (span.drawable as? InlineImagePlaceholderDrawable)?.let {
                    Triple(span, it.imageUrl, spanned.getSpanStart(span) to spanned.getSpanEnd(span))
                }
            }

        placeholders.forEach { (placeholder, url, range) ->
            val request = ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false)
                .target { drawable ->
                    val availableWidth = (width - paddingLeft - paddingRight)
                        .takeIf { it > 0 }
                        ?: (resources.displayMetrics.widthPixels - paddingLeft - paddingRight)
                    val intrinsicWidth = drawable.intrinsicWidth.takeIf { it > 0 } ?: availableWidth
                    val intrinsicHeight = drawable.intrinsicHeight.takeIf { it > 0 } ?: intrinsicWidth
                    val displayWidth = intrinsicWidth.coerceAtMost(availableWidth.coerceAtLeast(1))
                    val displayHeight = (intrinsicHeight.toFloat() * displayWidth / intrinsicWidth)
                        .toInt().coerceAtLeast(1)
                    drawable.setBounds(0, 0, displayWidth, displayHeight)
                    drawable.callback = this@RedditTextView
                    (drawable as? Animatable)?.start()

                    val updated = SpannableStringBuilder(text)
                    updated.removeSpan(placeholder)
                    updated.setSpan(
                        ImageSpan(drawable, ImageSpan.ALIGN_BASELINE),
                        range.first,
                        range.second,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    setText(updated)
                }
                .build()
            context.imageLoader.enqueue(request)
        }
    }

    override fun scrollTo(x: Int, y: Int) {
        if (scrollEnabled) super.scrollTo(x, y)
    }
}
