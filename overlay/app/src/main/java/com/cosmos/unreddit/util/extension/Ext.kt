package com.cosmos.unreddit.util.extension

import android.content.res.Resources
import android.os.Build
import android.util.TypedValue
import android.webkit.MimeTypeMap
import androidx.annotation.DimenRes

fun Int?.formatNumber(): String {
    return when {
        this == null -> ""
        this < 1000 -> this.toString()
        this < 1_000_000 -> "${String.format("%.1f", this.div(1000f))}k"
        else -> "${String.format("%.1f", this.div(1_000_000f))}m"
    }
}

fun MimeTypeMap.getMimeTypeFromUrl(url: String): String? {
    return getMimeTypeFromExtension(url.extension)
}

fun Resources.getFloatValue(@DimenRes id: Int): Float {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        getFloat(id)
    } else {
        val outValue = TypedValue()
        getValue(id, outValue, true)
        outValue.float
    }
}
