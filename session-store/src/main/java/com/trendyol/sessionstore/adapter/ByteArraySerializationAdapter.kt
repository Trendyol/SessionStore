package com.trendyol.sessionstore.adapter

/**
 * Serialization adapter for binary data formats.
 *
 * This interface is designed for serialization mechanisms that work with binary data,
 * such as Android Parcelable, Protocol Buffers, or other binary formats. Objects are
 * serialized to byte arrays and stored directly in the database.
 *
 * ## When to Use
 * This adapter is ideal for Android Parcelable objects, Protocol Buffers, or custom binary formats
 * where performance is critical. Binary serialization is typically faster than JSON-based approaches.
 *
 * @see SerializationAdapter base interface
 * @see JsonSerializationAdapter for JSON-based serialization
 */
public interface ByteArraySerializationAdapter : SerializationAdapter {
    /**
     * Serializes an object to a byte array.
     *
     * This method is called when storing an object via [com.trendyol.sessionstore.SessionStoreEditor.setObject].
     * The implementation should convert the object to a byte array using the desired
     * binary serialization mechanism (Parcelable, Protocol Buffers, etc.).
     *
     * @param obj The object to serialize. Can be any type supported by your implementation.
     *
     * @return A byte array representation of the object that can be stored in the database.
     */
    public fun serialize(obj: Any): ByteArray

    /**
     * Deserializes a byte array back into an object of the specified type.
     *
     * This method is called when retrieving an object via [com.trendyol.sessionstore.SessionStoreEditor.getObject].
     * The implementation should reconstruct the object from the byte array using the
     * desired binary deserialization mechanism.
     *
     * @param byte The byte array containing the serialized object data.
     * @param type The [Class] of the object to deserialize. Used to ensure type safety
     *             and guide the deserialization process.
     *
     * @return The deserialized object of type [T], or `null` if deserialization fails.
     *         Returning null is preferred over throwing exceptions.
     *
     * @param T The expected type of the deserialized object.
     *
     */
    public fun <T : Any> deserialize(byte: ByteArray, type: Class<T>): T?
}
