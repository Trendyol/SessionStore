package com.trendyol.sessionstore.sample.common

import android.os.Parcel
import android.os.Parcelable
import com.trendyol.sessionstore.adapter.ByteArraySerializationAdapter

internal class ParcelableSerializationAdapter : ByteArraySerializationAdapter {

    override fun serialize(obj: Any): ByteArray {
        if (obj !is Parcelable) {
            throw IllegalArgumentException(
                "ParcelAdapter requires objects to implement Parcelable. " +
                    "Got: ${obj.javaClass.name}"
            )
        }

        val parcel = Parcel.obtain()
        try {
            parcel.writeParcelable(obj, 0)
            return parcel.marshall()
        } finally {
            parcel.recycle()
        }
    }

    override fun <T : Any> deserialize(byte: ByteArray, type: Class<T>): T? {
        if (!Parcelable::class.java.isAssignableFrom(type)) {
            throw IllegalArgumentException(
                "ParcelAdapter requires type to implement Parcelable. " +
                    "Got: ${type.name}"
            )
        }

        val parcel = Parcel.obtain()
        try {
            parcel.unmarshall(byte, 0, byte.size)
            parcel.setDataPosition(0)

            val classLoader = type.classLoader
            return parcel.readParcelable<Parcelable>(classLoader) as? T
        } catch (e: Exception) {
            return null
        } finally {
            parcel.recycle()
        }
    }
}
