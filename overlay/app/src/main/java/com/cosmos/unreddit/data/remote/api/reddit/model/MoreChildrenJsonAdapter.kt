package com.cosmos.unreddit.data.remote.api.reddit.model

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi

class MoreChildrenJsonAdapter(moshi: Moshi) : JsonAdapter<MoreChildren>() {
    private val delegate: JsonAdapter<MoreChildren> = moshi.adapter(MoreChildren::class.java)

    override fun fromJson(reader: JsonReader): MoreChildren? = delegate.fromJson(reader)

    override fun toJson(writer: JsonWriter, value: MoreChildren?) {
        delegate.toJson(writer, value)
    }
}
