plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.hikari.app"
    // 36, not 35: OkHttp 5.4.0's Android artifact declares `minCompileSdk = 36`
    // in its AAR metadata (and every extension built on the current keiyoushi /
    // Aniyomi ecosystem links against OkHttp 5), so a project compiling against
    // 35 cannot depend on it at all — AGP fails the build outright. This is a
    // COMPILE-time level only: `targetSdk` below is untouched, so no runtime
    // behaviour changed. See the `agp` note in gradle/libs.versions.toml.
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hikari.app"
        minSdk = 24
        targetSdk = 34
	versionCode = 204
	versionName = "0.10.33"
        // CI injects the exact commit SHA the APK was built from, so the
        // in-app update checker can compare it against main's HEAD.
        val gitSha = System.getenv("GIT_SHA") ?: "unknown"
        buildConfigField("String", "GIT_SHA", "\"$gitSha\"")
        // ---- Processors this app is built for ----
        // armeabi-v7a is the 32-bit arm build (older/cheaper phones) and
        // arm64-v8a is the 64-bit one (every modern phone). x86 and x86_64 are
        // only ever found in Android emulators, so their native libraries —
        // tens of MB of ffmpeg/media binaries — were dead weight in every
        // phone's download, which is what made the single APK so large.
        // ("remove x86 and x86_64, from our apk as its for emulator we dont
        // need it, its just increasing app size")
        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a"))
        }
    }

    // ---- One APK per phone, plus a universal one ----
    // With this on, a release build produces `app-armeabi-v7a-release.apk`,
    // `app-arm64-v8a-release.apk` and `app-universal-release.apk` instead of a
    // single file that carries every processor's libraries. A user can then
    // download just their own phone's build (much less data) or the universal
    // one that works anywhere — and because only the two arm ABIs are built at
    // all now, even the universal APK is smaller than it used to be.
    //
    // Every APK keeps the SAME applicationId, signing key and versionCode, so
    // installations of one over another (and the in-app updater) all work.
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
                // Sign with debug key on forks so the APK can be installed on Android devices
                signingConfig = signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
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
            // ---- Coroutines 1.8.1 ships a multi-release jar too ----
            //
            // kotlinx-coroutines-core brings its own `versions/9/module-info.class`,
            // which collides with the same path from OkHttp's multi-release jars
            // above. The rule above discarded `versions/9/**`, but coroutines
            // also drops an empty `META-INF/versions/9` DIRECTORY entry into
            // the merged jar, which AGP 8.6 rejects with the same duplicate
            // resource error. Exclude the root of the tree as well.
            excludes += "/META-INF/versions/**"
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
    // Brotli + Zstandard decompression — Cloudflare and several extension hosts
    // (most notably AnimeFlv via Keiyoushi, which sends `content-encoding: zstd`)
    // compress with zstd. OkHttp does not decompress zstd out of the box:
    // without the transparent interceptor from okhttp-zstd, OkHttp hands the
    // compressed byte stream straight to Jsoup / the JSON parser, which fails
    // immediately with "malformed input".
    //
    // Two parts:
    //  * `okhttp-zstd` (com.github.luben:zstd-jni under the hood) — the
    //    standard transparent OkHttp interceptor, installed on Hikari's
    //    NetworkHelper OkHttpClient so any extension using the shared client
    //    gets zstd decompression for free.
    //  * `zstd-kmp-okio` — the pure-Kotlin / Okio-native zstd stream Keiyoushi's
    //    `keiyoushi.utils.Decompress` calls DIRECTLY on manual byte arrays. An
    //    extension that bypassed the OkHttp interceptor and called
    //    Decompress.zstd() died with NoClassDefFoundError on the first request — the "extension
    //    shows no catalog, and there is no Cloudflare page either" report for a
    //    whole family at once. Aniyomi's own app ships exactly this set.
    implementation(libs.okhttp.brotli)
    implementation(libs.okhttp.zstd)
    implementation(libs.zstd.kmp.okio)
    implementation(libs.jsoup)

    implementation(libs.coil.compose)
    // SVG decoding for extension logos: plenty of plugin/addon icons are
    // `.svg` (e.g. SkyStream's dramayo → dramayo.stream/static/dramayo.svg),
    // and Coil 2 answers those with a decode failure — i.e. the monochrome
    // glyph placeholder on every row. Registered in HikariApp's ImageLoader.
    implementation(libs.coil.svg)
    // Animated GIF decoding, for collection/folder covers ("Animated GIF URL"
    // in the cover editor): without it Coil draws only the first frame.
    implementation(libs.coil.gif)

    // ---- The manga reader (Nekoread's reader, ported whole) ----
    //
    // Three libraries the ported viewer stack is built on, all of them the exact
    // artifacts Nekoread itself uses (a JitPack commit for the Tachiyomi fork of
    // SubsamplingScaleImageView — the view that region-decodes a long strip from
    // its cache file instead of holding a page-sized bitmap in memory — and the
    // Tachiyomi fork of DirectionalViewPager, which is the ONE view that can page
    // vertically as well as horizontally, i.e. the "paged vertical" reading mode).
    //
    // recyclerview is explicit rather than assumed: the webtoon viewer IS a
    // RecyclerView (with Nekoread's own WebtoonLayoutManager, which subclasses
    // LinearLayoutManager), and a reader that only compiled because a Compose
    // dependency happened to bring RecyclerView along would break the day it did
    // not.
    implementation("com.github.tachiyomiorg:subsampling-scale-image-view:66e0db195d")
    implementation("com.github.tachiyomiorg:DirectionalViewPager:1.0.0") {
        // DirectionalViewPager re-exports an older androidx.viewpager; keep only
        // the version pinned below, or the two copies fight over DuplicateClass.
        exclude(group = "androidx.viewpager", module = "viewpager")
    }
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.viewpager:viewpager:1.1.0")

    implementation(libs.kotlinx.serialization.json)
    // Two kotlinx.serialization modules nothing in Hikari's own source touches,
    // added because ANIYOMI EXTENSIONS link against them BY NAME:
    //
    //  * kotlinx-serialization-json-okio provides
    //    `kotlinx.serialization.json.okio.OkioStreamsKt`, which the keiyoushi
    //    utils library (`keiyoushi.utils.Json.parseAs`) reaches through
    //    `Json.decodeFromBufferedSource` on every response-backed parse. It is a
    //    SEPARATE artifact from kotlinx-serialization-json, so an extension that
    //    called it died with
    //    `NoClassDefFoundError: kotlinx/serialization/json/okio/OkioStreamsKt`.
    //  * kotlinx-serialization-protobuf provides
    //    `kotlinx.serialization.protobuf.ProtoBuf`, which
    //    `keiyoushi.utils.ProtobufKt` reads at class-initialisation time
    //    (`val protoInstance: ProtoBuf = Injekt.get()`). Without it, merely
    //    loading that file throws.
    //
    // Both are pure Kotlin with no native code, and proguard-rules.pro's
    // `-keep class kotlinx.serialization.**` already keeps them whole. The
    // ProtoBuf singleton itself is registered in HikariApp (see
    // `registerAniyomiSingletons`).
    implementation(libs.kotlinx.serialization.json.okio)
    implementation(libs.kotlinx.serialization.protobuf)
    implementation(libs.kotlin.reflect)

    implementation(libs.jackson.databind)
    implementation(libs.jackson.module.kotlin)

    implementation(libs.nicehttp)
    implementation(libs.conscrypt.android)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.rhino)
    implementation(libs.ktor.http)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.ksoup)
    implementation(libs.kotlinx.datetime)
    implementation(libs.atomicfu)
    implementation(libs.newpipeextractor)

    // Mihon/Aniyomi's dependency-injection container. Aniyomi extension APKs
    // don't receive their dependencies as constructor arguments — the extension
    // loader instantiates a source and the source immediately pulls what it
    // needs (`Application`, `Json`, `NetworkHelper`, `JavaScriptEngine`, …) out
    // of the global `Injekt` scope. Hikari has no DI container of its own, so
    // the same container (mihonapp's fork, which rebuilds injekt for modern
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

    // (The bundled yt-dlp "universal extractor" — dev.ffmpegkit-maintained's
    // yt-dlp-android, a full CPython 3.13 embedded through Chaquopy — was
    // removed in 0.9.1. It was ~15 MB of the APK, arm64-only, and only ever
    // ran on pages every other engine had already failed on. See CHANGELOG.)

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
