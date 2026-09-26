plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.hikari.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hikari.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 204
        versionName = "0.10.33"

        val gitSha = System.getenv("GIT_SHA") ?: "unknown"
        buildConfigField("String", "GIT_SHA", "\"$gitSha\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a")
            isUniversalApk = true
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            val storePath = System.getenv("SIGNING_STORE_PATH")
            if (!storePath.isNullOrBlank() && file(storePath).exists()) {
                signingConfig = signingConfigs.create("release") {
                    storeFile = file(storePath)
                    storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                    keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                    keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
                }
            } else {
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
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
        viewBinding = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE*"
            excludes += "META-INF/NOTICE*"
            excludes += "/META-INF/versions/9/**"
            excludes += "META-INF/versions/**/OSGI-INF/MANIFEST.MF"
            excludes += "META-INF/versions/**/module-info.class"
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
            excludes += "META-INF/versions/9/module-info.class"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

val cloudstreamRawJar = file("libs/cloudstream3.jar")
val cloudstreamCleanJar = layout.buildDirectory.file("cleaned-libs/cloudstream3-clean.jar")

val cloudstreamJarClean by tasks.registering(Jar::class) {
    archiveFileName.set("cloudstream3-clean.jar")
    destinationDirectory.set(layout.buildDirectory.dir("cleaned-libs"))
    from(zipTree(cloudstreamRawJar)) {
        exclude { element ->
            val path = element.path
            val name = path.substringAfterLast('/')
            val isRClass = name == "R.class" || (name.startsWith("R$") && name.endsWith(".class"))
            val isCloudStreamR = path == "com/lagradost/cloudstream3/R.class" ||
                (path.startsWith("com/lagradost/cloudstream3/R$") && name.endsWith(".class"))
            isRClass && !isCloudStreamR
        }
        exclude("com/lagradost/cloudstream3/network/WebViewResolver*.class")
        exclude("com/lagradost/cloudstream3/network/CloudflareKiller*.class")
        exclude("com/lagradost/cloudstream3/CloudStreamApp*.class")
        exclude("com/lagradost/cloudstream3/MainActivity.class")
        exclude("com/lagradost/cloudstream3/MainActivity$*.class")
        exclude("com/lagradost/cloudstream3/databinding/ToastBinding.class")
    }
    duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.EXCLUDE
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(files(cloudstreamCleanJar))
    implementation(files("libs/quickjs-kt-android-1.0.5-nuvio.aar"))
    implementation("com.github.recloudstream:torrentserver:7861970")

    implementation(libs.androidx.core.ktx)
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.datasource.okhttp)
    implementation(libs.androidx.media3.effect)
    implementation("io.github.anilbeesetti:nextlib-media3ext:1.7.1-0.9.0")
    implementation(libs.androidx.splashscreen)

    implementation(libs.okhttp)
    implementation(libs.okhttp.brotli)
    implementation(libs.okhttp.zstd)
    implementation(libs.zstd.kmp.okio)
    implementation(libs.jsoup)

    implementation(libs.coil.compose)
    implementation(libs.coil.svg)
    implementation(libs.coil.gif)

    implementation("eu.kanade.tachiyomi:subsample-image-view:0.9.72") {
        exclude(group = "com.davemorrissey.labs", module = "subsampling-scale-image-view")
    }
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.viewpager:viewpager:1.1.0")

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.cbor)
    implementation(libs.kotlinx.serialization.json.okio)
    implementation(libs.kotlinx.serialization.protobuf)
    implementation(libs.kotlin.reflect)

    implementation(libs.jackson.databind)
    implementation(libs.jackson.module.kotlin)

    implementation(libs.nicehttp)
    implementation(libs.conscrypt.android)
    implementation(libs.androidx.preference.ktx)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.kotlinx.datetime)
    implementation(libs.atomicfu)
    implementation(libs.newpipeextractor)

    implementation("com.github.mihonapp:injekt:91edab2317")
    implementation("io.reactivex:rxjava:1.3.8")
    implementation("org.nanohttpd:nanohttpd:2.3.1")
    implementation(libs.logging.interceptor)

    implementation(libs.androidx.viewbinding)
    implementation(libs.androidx.cardview)

    implementation(libs.cryptography.core)
    implementation(libs.cryptography.provider.optimal)

    implementation(libs.zxing.core)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    implementation(libs.androidx.biometric)
    implementation(libs.androidx.lifecycle.process)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
