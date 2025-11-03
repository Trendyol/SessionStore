plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.ksp)
    alias(libs.plugins.maven.publish)
}

version = publishedLibs.versions.sessionStore.get()
group = "com.trendyol"

android {
    namespace = "com.trendyol.sessionstore"

    defaultConfig {
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
        consumerProguardFiles("consumer-rules.pro")
    }
}

kotlin {
    explicitApi()
}

kotlin {
    jvmToolchain(21)
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = false)
    pom {
        name = "Session Store"
        description = "Android session-scoped data storage with automatic lifecycle management."
        url = "https://github.com/Trendyol/SessionStore"
        licenses {
            license {
                name = "SessionStore License"
                url = "https://github.com/Trendyol/SessionStore/blob/main/LICENSE"
            }
        }
        developers {
            developer {
                id = "ertugrul"
                name = "Ertuğrul Karagöz"
                email = "ertugrulkaragoz12@gmail.com"
            }
        }
        scm {
            connection = "scm:git:github.com/Trendyol/SessionStore.git"
            developerConnection = "scm:git:ssh://github.com/Trendyol/SessionStore.git"
            url = "https://github.com/Trendyol/SessionStore/tree/main"
        }
    }
    val signingKeyId = System
        .getenv("ORG_GRADLE_PROJECT_signingInMemoryKeyId")
        .orEmpty()
    val signingKeyPassword = System
        .getenv("ORG_GRADLE_PROJECT_signingInMemoryKeyPassword")
        .orEmpty()
    val signingKey = System
        .getenv("ORG_GRADLE_PROJECT_signingInMemoryKey")
        .orEmpty()
    if (signingKeyId.isNotEmpty() && signingKey.isNotEmpty() && signingKeyPassword.isNotEmpty()) {
        signAllPublications()
    }
}

dependencies {
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.lifecycle.process)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.truth)
}
