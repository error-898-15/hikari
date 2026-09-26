import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.hikari.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hikari.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 214
        versionName = "0.10.43"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Pass the app version into C++ (used by the crash handler's log header)
        externalNativeBuild {
            cmake {
                cppFlags += "-DHIKARI_VERSION_NAME=\\\"${versionName}\\\""
                cppFlags += "-DHIKARI_VERSION_CODE=${versionCode}"
            }
        }

        ndk {
            // Target the architectures that cover virtually all modern Android devices.
            // 32-bit x86 is obsolete; 64-bit x86 is only used by emulators.
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a")
            // Generate an additional universal APK containing all ABIs for distribution channels
            // that do not support split APKs (e.g. GitHub Releases).
            isUniversalApk = true
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildTypes {
        release {
            // ---- Shrinking: ON ----
            //
            // The release build was shipping every class of every dependency:
            // ~70 MB of dex, most of it code nothing in this APK can reach
            // (unused Compose components, unused Material icons, library
            // internals). `isShrinkResources` needs `isMinifyEnabled`, so the
            // two go together.
            //
            // It is safe HERE because app/proguard-rules.pro starts with
            // `-dontobfuscate` and keeps every namespace that a plugin or an
            // extension links against by name — read that file before changing
            // anything in it, and re-read it before turning either of these
            // back off in a panic: the fix for a broken plugin is a kept
            // namespace there, not a lost 10 MB here.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            // CI passes signing config via env vars (decoded from the SIGNING_KEY
            // repo secret). Local builds stay unsigned.
            val storePath = System.getenv("SIGNING_STORE_PATH")
            if (!storePath.isNullOrBlank()) {
                signingConfig = signingConfigs.create("release") {
                    storeFile = file(storePath)
                    storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                    keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                    keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
                }
            } else {
                // Fallback for forks: sign with debug key so the APK can be installed on Android devices
                signingConfig = signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // ---- OkHttp 5 ships Java 9 multi-release jars ----
            //
            // okhttp-brotli, logging-interceptor and okhttp-dnsoverhttps are
            // published as MULTI-RELEASE jars: each one carries the same
            // JVM-only metadata under `META-INF/versions/9/` — a JPMS/OSGi
            // bundle manifest plus the `module-info.class` next to it. AGP's
            // Java-resource merger copies those like any other resource, so the
            // SECOND one it encounters collides with the first and breaks
            // `:app:mergeDebugJavaResource` (and the release build behind it)
            // with "2 files found with path 'META-INF/versions/9/OSGI-INF/MANIFEST.MF'".
            //
            // Android does not use Java 9 modules or OSGi bundles — ART loads
            // classes from dex, not from jar resources. Discarding the whole
            // `versions/9/` tree is the standard fix and harmless.
            excludes += "/META-INF/versions/9/**"
        }
    }
}

dependencies {
    // ---- Bundled plugin SDKs ----
    //
    // Placed in app/libs/ so Gradle can compile against their classes without
    // pulling them from a remote repository that could go away or change:
    //
    //  * cloudstream3.jar — the CloudStream plugin SDK. Extracted straight out of
    //    CloudStream's own APK (`app-release.apk` from the latest GitHub
    //    release: dex2jar -> unpack). It provides every `com.lagradost.*` class
    //    a CloudStream plugin compiles against, including NiceHttp,
    //    AcraApplication and the Jackson databind helpers.
    //
    //  * aniyomi-mpv-lib.aar — mpv-android's own AAR from aniyomiorg/aniyomi-mpv-lib,
    //    the video player core. Bundled directly.
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Navigation & DataStore
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)

    // Media3 (ExoPlayer) — the fallback player
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.datasource.okhttp)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.exoplayer.dash)

    // Networking
    //
    // OkHttp is pinned to 5.4.0 in gradle/libs.versions.toml (the okhttp-android
    // artifact, which is the only 5.x flavor that supports Android without the
    // Java-9 multi-release collisions of okhttp-jvm).
    implementation(libs.okhttp)
    implementation(libs.okhttp.brotli)
    implementation(libs.jsoup)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Image loading
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)
    implementation(libs.coil.gif)

    // Jackson — required at RUNTIME because CloudStream plugins use Jackson
    // for all their JSON parsing (e.g. `parseJson<T>()`). The plugin's bytecode
    // contains direct calls to Jackson methods; if Jackson is missing from the
    // app's runtime classpath, those calls crash with NoClassDefFoundError.
    implementation(libs.jackson.databind)
    implementation(libs.jackson.module.kotlin)

    // Room database (history, bookmarks, downloaded extension metadata)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Injekt — Aniyomi extensions compile against Injekt's static service
    // locator (`Injekt.get<Application>()`, etc.). The bridge (which runs in
    // Kotlin and patches the registrar) is installed and primed in
    // HikariApp.onCreate — see AniyomiExtensionManager.
    implementation("com.github.mihonapp:injekt:91edab2317")

    // RxJava 1 — Aniyomi's `AnimeHttpSource` still carries the deprecated Rx
    // `fetch*` API that `AnimeCatalogueSource`'s default methods delegate to
    // (extlib-14 extensions only implement the suspend methods, so the Rx bridge
    // vendored in RxCoroutineBridge is what actually runs them).
    implementation("io.reactivex:rxjava:1.3.8")

    // Aniyomi's `HttpServer` (a NanoHTTPD local proxy some extensions use for
    // streams that want same-origin requests). Hikari never starts one, but the
    // class still has to LINK — `AnimeHttpSource.createHttpServer()` and the
    // video-resolving paths name it.
    implementation("org.nanohttpd:nanohttpd:2.3.1")

    // `HttpLoggingInterceptor` — NetworkHelper's OkHttp stack. (Kept on the same
    // version ref as `okhttp` itself: the logging interceptor's `Client`-facing
    // API moved in 5.x and a 4.x jar against a 5.x core is a link error.)
    implementation(libs.logging.interceptor)

    // Required to LINK the CloudStream jar's generated ViewBinding classes
    // (ToastBinding et al.) — they implement androidx.viewbinding.ViewBinding
    // and call ViewBindings.findChildViewById. Without the viewbinding runtime
    // ART fails the class link and surfaces it as
    // NoClassDefFoundError: Lcom/lagradost/cloudstream3/databinding/ToastBinding;
    // which killed the app whenever a plugin called CommonActivity.showToast.
    implementation(libs.androidx.viewbinding)

    // CardView is the declared type of ToastBinding.getRoot() (and the root of
    // res/layout/hikari_toast.xml), so the class must be on the compile+runtime
    // classpath too.
    implementation(libs.androidx.cardview)

    // Cryptography
    implementation(libs.cryptography.core)
    implementation(libs.cryptography.provider.optimal)

    // ---- Device pairing (Settings → Backup & Restore → Pair & sync) ----
    //
    // The feature itself is a few hundred lines: two `PairHost`/`PairClient`
    // objects and a screen. Its two dependencies are what turn "the code on one
    // screen" into "the code on one screen that the other device can read":
    //
    //  * zxing-core — the QR encoder AND decoder, pure Java, no Android
    //    dependencies, no camera permission, ~500 KB. Encoding a pairing payload
    //    and decoding a camera frame are both one call into it
    //    (see com.hikari.app.pair.QrCode). It is the same library every Android
    //    scanner app is built on; the alternative (a barcode API that only
    //    exists on devices with Play Services) is exactly the wrong trade for an
    //    app whose whole point is that it runs on a cheap Chinese television.
    //  * CameraX — the scanner's camera. camera-view brings `PreviewView` (the
    //    surface that actually draws the preview and does the device's own
    //    rotation/scale handling, which doing it by hand is a week of bugs), and
    //    camera-camera2 the Camera2CameraImpl underneath it. The OTHER camera in
    //    this app is the system WebView's, which is none of our business.
    //
    // Both are only ever used by the pairing screen: a device that never opens it
    // never starts a camera or a decoder.
    implementation(libs.zxing.core)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // ---- The in-app lock (Settings → Privacy & Browsing → App lock) ----
    //
    //  * biometric — the fingerprint/face prompt itself. The androidx one, not
    //    the platform's: this app runs from API 24 and the platform
    //    BiometricPrompt only exists from API 28, so the platform API would mean
    //    writing (and testing) two different prompts on two different paths.
    //  * lifecycle-process — ProcessLifecycleOwner, the ONE signal that means
    //    "the app went to the background". Locking on the activity's own stop
    //    would re-lock behind the player (a separate activity), i.e. a password
    //    prompt every time a film ends.
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.lifecycle.process)

    debugImplementation(libs.androidx.compose.ui.tooling)
}

// ---------------------------------------------------------------------------
// org.json must never be on the runtime classpath.
//
// Android already provides org.json — it lives in
// /apex/com.android.art/javalib/core-libart.jar — and the boot class loader is
// consulted before ours, so a copy we ship is shadowed: the device runs the
// platform's implementation whichever version we bundle.
//
// NiceHttp (com.github.Blatzar:NiceHttp, the client CloudStream plugins link
// against) declares org.json:json at runtime scope, and that is a *modern*
// org.json with APIs the platform does not have. Shipping it is what broke
// 0.9.2. Our own code calls the platform-compatible `JSONObject(s)`; but with
// shrinking on, R8 treated the bundled copy as the definition and inlined its
// constructor body into our classes, which turned that call into a direct
// `new JSONTokener(String, JSONParserConfiguration)` — a constructor that
// exists only in the bundled copy. At runtime the name resolved to the
// platform's JSONTokener, which has no such constructor, so the app died at
// startup with NoSuchMethodError (0.9.2 / build 158, in AppStore.parseProviders).
// Without shrinking the call stayed a call and went to the platform's
// implementation, which is why 0.9.0 was fine.
//
// Excluding it here makes compile time, R8's view and the device agree on one
// org.json: the platform's. Any dependency that declares org.json:json brings
// this crash back with it, so check for it when adding one.
// ---------------------------------------------------------------------------
configurations.configureEach {
    exclude(mapOf("group" to "org.json", "module" to "json"))
}

// ---------------------------------------------------------------------------
// NiceHttp drags a second OkHttp version onto the classpath — pin it back
//
// NiceHttp (com.github.Blatzar:NiceHttp, the client CloudStream plugins link
// against) declares `com.squareup.okhttp3:okhttp-jvm:5.3.2` and
// `com.squareup.okhttp3:okhttp-dnsoverhttps:5.3.2`. The two requests resolve
// differently:
//
//  * `okhttp-jvm` is only an ALIAS. Its Gradle metadata points at the
//    multiplatform root `com.squareup.okhttp3:okhttp`, so the request takes part
//    in normal conflict resolution and Hikari's own 5.4.0 wins — which is why
//    no okhttp-jvm jar appears anywhere in the build, and why the Android
//    artifact (`okhttp-android`, the one whose AAR metadata forced compileSdk
//    36) is the only OkHttp core on the classpath.
//  * `okhttp-dnsoverhttps` is a plain, standalone module. Nothing else asks for
//    it at a newer version, so its 5.3.2 request stands and a 5.3.2 jar ends up
//    beside a 5.4.0 core — it is one of the three jars that broke
//    `:app:mergeDebugJavaResource` (see the packaging block above).
//
// A 5.3.x OkHttp module calling into 5.4.0 internals is a NoSuchMethodError
// waiting for whoever touches it. Nothing in Hikari uses DnsOverHttps (checked
// across app/src) and neither does the bundled cloudstream3.jar, but plugins
// link against NiceHttp's tree, so the class has to stay on the classpath:
// pinning it to 5.4.0 puts the whole OkHttp family on ONE version, which is the
// same rule the rest of this build follows (see the okhttp note in
// gradle/libs.versions.toml and the org.json block above).
// ---------------------------------------------------------------------------
configurations.configureEach {
    resolutionStrategy.force("com.squareup.okhttp3:okhttp-dnsoverhttps:${libs.versions.okhttp.get()}")
}
