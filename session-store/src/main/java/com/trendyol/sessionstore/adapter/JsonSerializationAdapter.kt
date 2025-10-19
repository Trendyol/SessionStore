package com.trendyol.sessionstore.adapter

/**
 * Serialization adapter for JSON-based data formats.
 *
 * This interface is designed for serialization mechanisms that work with JSON,
 * such as Moshi, Gson, Jackson, or kotlinx.serialization. Objects are serialized
 * to JSON strings, which are then converted to UTF-8 byte arrays for storage.
 *
 * ## When to Use
 * JSON serialization is ideal when your app already uses JSON libraries (Moshi, Gson), needs human-readable
 * data for debugging, shares serialization with REST APIs, or benefits from flexible schema evolution.
 * While generally slower than binary formats, JSON offers better debuggability
 * and easier schema changes. For performance-critical use cases, consider [ByteArraySerializationAdapter] instead.
 *
 * @see SerializationAdapter base interface
 * @see ByteArraySerializationAdapter for binary serialization
 */
public  interface JsonSerializationAdapter : SerializationAdapter {
    /**
     * Serializes an object to a JSON string.
     *
     * This method is called when storing an object via [com.trendyol.sessionstore.SessionStoreEditor.setObject].
     * The implementation should convert the object to a JSON string using the desired
     * JSON library (Moshi, Gson, etc.). The string will be stored directly in the database.
     *
     * @param obj The object to serialize. Can be any type supported by your JSON library.
     *
     * @return A JSON string representation of the object.
     *
     */
    public fun serialize(obj: Any): String

    /**
     * Deserializes a JSON string back into an object of the specified type.
     *
     * This method is called when retrieving an object via [com.trendyol.sessionstore.SessionStoreEditor.getObject].
     * The implementation should reconstruct the object from the JSON string using the
     * desired JSON library.
     *
     * @param json The JSON string containing the serialized object data.
     * @param type The [Class] of the object to deserialize. Used to ensure type safety
     *             and guide the deserialization process.
     *
     * @return The deserialized object of type [T], or `null` if deserialization fails.
     *         Returning null is preferred over throwing exceptions.
     *
     * @param T The expected type of the deserialized object.
     *
     */
    public fun <T : Any> deserialize(json: String, type: Class<T>): T?
}
