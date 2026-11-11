package com.cosmos.unreddit.data.remote.api.reddit.model

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi

class AboutUserChildJsonAdapter(moshi: Moshi) : JsonAdapter<AboutUserChild>() {
    private val delegate: JsonAdapter<AboutUserChild> = moshi.adapter(AboutUserChild::class.java)

    override fun fromJson(reader: JsonReader): AboutUserChild? = delegate.fromJson(reader)

    override fun toJson(writer: JsonWriter, value: AboutUserChild?) {
        delegate.toJson(writer, value)
    }
}
