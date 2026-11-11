package com.cosmos.unreddit.data.remote.api.reddit.model

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi

class AboutChildJsonAdapter(moshi: Moshi) : JsonAdapter<AboutChild>() {
    private val delegate: JsonAdapter<AboutChild> = moshi.adapter(AboutChild::class.java)

    override fun fromJson(reader: JsonReader): AboutChild? = delegate.fromJson(reader)

    override fun toJson(writer: JsonWriter, value: AboutChild?) {
        delegate.toJson(writer, value)
    }
}
