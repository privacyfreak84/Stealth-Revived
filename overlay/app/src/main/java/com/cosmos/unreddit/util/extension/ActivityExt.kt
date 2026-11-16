package com.cosmos.unreddit.util.extension

import android.app.Activity
import com.cosmos.unreddit.UnredditApplication

val Activity.unredditApplication: UnredditApplication
    get() = application as UnredditApplication
