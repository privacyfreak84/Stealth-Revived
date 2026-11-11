package com.cosmos.unreddit.data.remote.api.reddit.model

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi

class ListingJsonAdapter(moshi: Moshi) : JsonAdapter<Listing>() {
    private val delegate: JsonAdapter<Listing> = moshi.adapter(Listing::class.java)

    override fun fromJson(reader: JsonReader): Listing? = delegate.fromJson(reader)

    override fun toJson(writer: JsonWriter, value: Listing?) {
        delegate.toJson(writer, value)
    }
}
