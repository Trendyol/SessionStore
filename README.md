# SessionStore

SessionStore is a lightweight Android library that provides session-scoped data storage for your applications. It offers a simple key-value API for storing arbitrary objects that automatically persist across configuration changes and process death, while being intelligently cleaned up when a new user session begins.

## Why SessionStore Exists

Android's Bundle has a strict size limitation of approximately 1MB for saving and restoring instance state. When your application needs to pass large objects between activities or fragments, retain complex state across configuration changes, or persist substantial data during process death, the Bundle quickly becomes insufficient. Exceeding this limit results in `TransactionTooLargeException`, causing crashes or silent data loss.

SessionStore solves this problem by providing an alternative storage mechanism that uses Room database for persistence, eliminating size constraints while maintaining the same lifecycle guarantees you'd expect from saved instance state. The library automatically detects whether your activity is starting fresh, being recreated from a configuration change, or recovering from process death, and manages data cleanup accordingly. When a genuine new session begins—such as a fresh app launch after process termination without saved state—all previous session data is automatically purged, ensuring you don't accumulate stale data over time.

## Quick Start

Here's a minimal example to get you started with SessionStore using JSON serialization:

```kotlin
// Initialize in your Application class
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Install SessionStore with a JSON serialization adapter
        val adapter = JsonSerializationAdapter { /* your implementation */ }
        SessionStore.install(this, adapter)
    }
}

// Use SessionStore from your activities
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            // Get an editor and store some data
            val editor = SessionStore.getEditor()
            editor.setObject("user_profile", UserProfile(name = "John", age = 30))

            // Retrieve data later
            val profile = editor.getObject("user_profile", UserProfile::class.java)
        }
    }
}
```

## Setting Up

Add the SessionStore dependencies to your module's `build.gradle.kts` file.

```kotlin
dependencies {
    implementation("com.trendyol.sessionstore:<latest-version>")
}
```

## How to Use

SessionStore requires initialization in your Application class's `onCreate` method before you can store or retrieve data. The installation process registers activity lifecycle callbacks to detect the launch reason—whether it's a first launch, process death recovery, or configuration change—and initializes the session accordingly.

### Installing SessionStore

Call `install` on the SessionStore companion object in your Application class's `onCreate` method, passing your Application instance and a serialization adapter. Once installed, SessionStore is ready to accept read and write operations from anywhere in your app.

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val serializationAdapter = createSerializationAdapter()
        SessionStore.install(this, serializationAdapter)
    }

    private fun createSerializationAdapter(): SerializationAdapter {
        // Return your JsonSerializationAdapter or ByteArraySerializationAdapter
        return MoshiJsonAdapter(moshi)
    }
}
```

Don't forget to register your Application class in your AndroidManifest.xml:

```xml
<application
    android:name=".MyApplication"
    ...>
    ...
</application>
```

### Implementing a Serialization Adapter

SessionStore requires a serialization adapter to convert your objects to and from a storable format. You can choose between JSON-based serialization or binary serialization depending on your needs.

For JSON serialization using Moshi, create an adapter that implements `JsonSerializationAdapter`:

```kotlin
class MoshiJsonAdapter(private val moshi: Moshi) : JsonSerializationAdapter {
    override fun serialize(obj: Any): String {
        val adapter = moshi.adapter(obj::class.java)
        return adapter.toJson(obj)
    }

    override fun <T : Any> deserialize(json: String, type: Class<T>): T? {
        return try {
            val adapter = moshi.adapter(type)
            adapter.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }
}
```

For binary serialization using Android's Parcelable, create an adapter that implements `ByteArraySerializationAdapter`:

```kotlin
class ParcelableAdapter : ByteArraySerializationAdapter {
    override fun serialize(obj: Any): ByteArray {
        val parcel = Parcel.obtain()
        try {
            parcel.writeParcelable(obj as Parcelable, 0)
            return parcel.marshall()
        } finally {
            parcel.recycle()
        }
    }

    override fun <T : Any> deserialize(byte: ByteArray, type: Class<T>): T? {
        val parcel = Parcel.obtain()
        try {
            parcel.unmarshall(byte, 0, byte.size)
            parcel.setDataPosition(0)
            return parcel.readParcelable(type.classLoader)
        } catch (e: Exception) {
            null
        } finally {
            parcel.recycle()
        }
    }
}
```

### Storing and Retrieving Data

Once SessionStore is installed, obtain an editor to perform read and write operations. All operations are suspend functions that execute on background threads, making them safe to call from the main thread without blocking.

```kotlin
lifecycleScope.launch {
    val editor = SessionStore.getEditor()

    // Store different types of objects
    editor.setObject("shopping_cart", ShoppingCart(items = listOf(...)))
    editor.setObject("search_filters", SearchFilters(category = "electronics"))
    editor.setObject("user_preferences", UserPreferences(theme = "dark"))

    // Retrieve objects later
    val cart = editor.getObject("shopping_cart", ShoppingCart::class.java)
    val filters = editor.getObject("search_filters", SearchFilters::class.java)

    // Objects from previous sessions return null
    val oldData = editor.getObject("non_existent_key", Any::class.java) // returns null
}
```

### Session Lifecycle and Data Cleanup

SessionStore maintains a session counter that increments only on genuine fresh app launches—when the app starts without any saved state from a previous process. During configuration changes like screen rotations, or when the system kills your process but later restores it with saved state, the session counter remains unchanged and your data persists.

When a new session begins, SessionStore automatically deletes all data from previous sessions in a background thread, ensuring you start with a clean slate. This means you never have to worry about manually cleaning up old data or checking whether data is stale. Each session is completely isolated from previous sessions, and you can rely on this behavior to implement features like temporary caches, wizard flows, or multi-step forms that should reset on fresh launches.

## API Reference

The SessionStore API is designed around simple, coroutine-based interfaces that handle threading and lifecycle management automatically.

### SessionStore Interface

The `SessionStore` interface serves as the main entry point for the library. It provides two primary methods: `install` for initializing the session store and `getEditor` for obtaining an editor to perform read and write operations.

**`install(application: Application, serializationAdapter: SerializationAdapter)`**

Must be called once in your Application class's `onCreate` method. It accepts the Application instance and a serialization adapter. This initialization process registers activity lifecycle callbacks to detect whether the first activity is starting fresh, recovering from process death, or being recreated from a configuration change. Based on this detection, SessionStore either increments the session counter and cleans up old data, or maintains the existing session number.

**`suspend fun getEditor(): SessionStoreEditor`**

Returns a `SessionStoreEditor` instance bound to the current session. This method suspends until `install` completes, ensuring the session is ready before any data operations occur. All operations performed through the editor are scoped to the current session number and benefit from automatic cleanup when sessions change.

### SessionStoreEditor Interface

The `SessionStoreEditor` interface provides a straightforward key-value API for storing and retrieving objects within the current session.

**`suspend fun setObject(key: String, value: Any)`**

Stores an object in the current session under the specified key. The object is serialized using your configured serialization adapter and stored in the Room database associated with the current session number. If an object already exists with the same key in the current session, it's replaced with the new value. The operation executes on a background thread and suspends until completion.

**`suspend fun <T : Any> getObject(key: String, type: Class<T>): T?`**

Retrieves a previously stored object by its key and deserializes it to the specified type. It queries the database for objects matching both the key and the current session number, deserializes the result using your serialization adapter, and returns it. Returns null if no object is found, the data was from a previous session and has been cleaned up, or deserialization failed. All operations are thread-safe and use Kotlin coroutines with appropriate dispatchers to prevent blocking the main thread.

### SerializationAdapter Interface

The `SerializationAdapter` interface serves as a marker interface and base type for the two specialized serialization strategies: `JsonSerializationAdapter` and `ByteArraySerializationAdapter`. It exists primarily to provide a common type that the `SessionStore.install` method can accept, allowing you to choose the serialization approach that best fits your application.

You never implement `SerializationAdapter` directly. Instead, you implement one of its subtypes depending on whether you want to use JSON-based serialization or binary serialization. This design gives you the flexibility to use existing JSON libraries like Moshi or Gson, or to leverage Android's Parcelable system and other binary formats without being forced into a specific serialization strategy.

### ByteArraySerializationAdapter Interface

The `ByteArraySerializationAdapter` interface defines the contract for binary serialization mechanisms. It extends `SerializationAdapter` and declares two methods: `serialize` to convert objects into byte arrays, and `deserialize` to reconstruct objects from byte arrays.

The `serialize` method receives an object and must return a byte array representation suitable for storage. This is your opportunity to use Android's Parcelable system, Protocol Buffers, or any other binary serialization format. The byte array is stored directly in the Room database.

The `deserialize` method receives a byte array and a Class type parameter, and must reconstruct the original object. The type parameter helps ensure type safety during deserialization and guides your deserialization logic. If deserialization fails for any reason—corrupted data, version mismatches, or missing classes—your implementation should return null rather than throwing an exception. This graceful failure handling prevents crashes when deserializing data from older app versions.

Binary serialization is typically faster and more compact than JSON, making it ideal for performance-critical applications or when working with large data structures. If your objects are already Parcelable or you're using Protocol Buffers, `ByteArraySerializationAdapter` is the natural choice.

### JsonSerializationAdapter Interface

The `JsonSerializationAdapter` interface defines the contract for JSON-based serialization. Like its binary counterpart, it extends `SerializationAdapter` and declares two methods: `serialize` to convert objects to JSON strings, and `deserialize` to reconstruct objects from JSON strings.

The `serialize` method receives an object and must return a JSON string representation. You can use any JSON library you prefer—Moshi, Gson, Jackson, or kotlinx.serialization. The JSON string is stored directly in the Room database, making it easily inspectable during debugging.

The `deserialize` method receives a JSON string and a Class type parameter, reconstructing the original object from the JSON data. As with binary deserialization, the type parameter ensures type safety and guides your deserialization logic. If deserialization fails—due to malformed JSON, schema changes, or missing fields—return null rather than throwing an exception to prevent crashes.

JSON serialization offers excellent debuggability since you can inspect the stored data directly in the database as human-readable text. However, JSON is generally slower and produces larger data than binary formats, so consider your performance requirements when choosing between the two approaches. For most applications with moderate data sizes and existing JSON infrastructure, `JsonSerializationAdapter` provides the best balance of convenience and maintainability.

## License

```
Copyright 2025 Trendyol

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```