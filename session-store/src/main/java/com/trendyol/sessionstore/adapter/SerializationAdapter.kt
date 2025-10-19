package com.trendyol.sessionstore.adapter

/**
 * Base marker interface for serialization adapters.
 *
 * This interface serves as the base for two specialized serialization adapter types:
 * - [ByteArraySerializationAdapter] - for binary serialization (Parcelable, Protocol Buffers)
 * - [JsonSerializationAdapter] - for JSON-based serialization (Moshi, Gson, Jackson)
 *
 * The session store library accepts either adapter type, allowing you to choose the
 * serialization mechanism that best fits your application's needs.
 *
 * ## Choosing an Adapter Type
 *
 * Use [ByteArraySerializationAdapter] for Android Parcelable objects, Protocol Buffers, or other binary
 * formats where performance is critical and data doesn't need to be human-readable. Use [JsonSerializationAdapter]
 * when you're already using JSON libraries like Moshi or Gson, need human-readable data for debugging, work with
 * REST APIs.
 *
 * @see ByteArraySerializationAdapter for binary serialization
 * @see JsonSerializationAdapter for JSON serialization
 * @see com.trendyol.sessionstore.SessionStore.install for providing an adapter implementation
 */
public interface SerializationAdapter
