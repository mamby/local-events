plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

apply(from = rootProject.file("gradle/validate-androidkit-resources.gradle"))

android {
    namespace = "net.mamby.events"
    compileSdk = 37
    ndkVersion = "30.0.16248370"

    defaultConfig {
        applicationId = "net.mamby.events"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "net.mamby.events.HiltTestRunner"
    }

    /* -------------------------
     * FLAVOR DIMENSION
     * ------------------------- */
    flavorDimensions += "env"

    productFlavors {

        create("dev") {
            dimension = "env"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
        }

        create("beta") {
            dimension = "env"
            applicationIdSuffix = ".beta"
            versionNameSuffix = "-beta"
        }

        create("stage") {
            dimension = "env"
            applicationIdSuffix = ".stage"
            versionNameSuffix = "-stage"
        }

        create("prod") {
            dimension = "env"
        }
    }

    /* -------------------------
     * BUILD TYPES
     * ------------------------- */
    buildTypes {
        debug {
            isMinifyEnabled = false
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    androidResources {
        localeFilters += providers.gradleProperty("androidKitSupportedLocales").get()
            .split(',').map { tag ->
                // Android resource lookup uses the legacy Indonesian qualifier.
                if (tag == "id") "in" else "b+" + tag.replace('-', '+')
            }
        generateLocaleConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        abortOnError = true
        fatal += "MissingTranslation"
        fatal += "ExtraTranslation"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

androidComponents {
    beforeVariants(selector().withBuildType("debug")) { variantBuilder ->
        if (variantBuilder.productFlavors.none { (_, flavor) -> flavor == "dev" }) {
            variantBuilder.enable = false
        }
    }

    onVariants { variant ->
        val flavor = variant.productFlavors.single { (dimension) -> dimension == "env" }.second
        val environmentVariable = "LOCAL_EVENTS_API_${flavor.uppercase()}"
        val apiBaseUrl = providers.environmentVariable(environmentVariable)
            .map { url ->
                com.android.build.api.variant.BuildConfigField(
                    "String",
                    "\"$url\"",
                    "API base URL for the $flavor environment."
                )
            }
            .orElse(
                providers.provider {
                    throw GradleException("Missing env var $environmentVariable")
                }
            )

        requireNotNull(variant.buildConfigFields) {
            "BuildConfig must be enabled for the ${variant.name} variant."
        }.put("LOCAL_EVENTS_API_BASE_URL", apiBaseUrl)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    val androidKitBom = platform("net.mamby.androidkit:bom:0.1.41-SNAPSHOT")
    val lifecycleVersion = "2.11.0"
    val media3Version = "1.11.1"
    val navigationVersion = "2.10.2"
    val ktorVersion = "3.6.0"
    val coilVersion = "3.6.3"

    implementation(composeBom)
    implementation(androidKitBom)
    androidTestImplementation(composeBom)

    implementation("net.mamby.androidkit:compose")
    implementation("androidx.activity:activity-compose:1.14.0-alpha03")
    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-text")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.core:core-splashscreen:1.2.0")
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.4.0")
    implementation("androidx.lifecycle:lifecycle-process:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleVersion")
    implementation("androidx.navigation:navigation-compose:$navigationVersion")

    implementation("androidx.media3:media3-common:$media3Version")
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-ui-compose:$media3Version")

    implementation("io.coil-kt.coil3:coil-compose:$coilVersion")
    implementation("io.coil-kt.coil3:coil-network-ktor3:$coilVersion")
    implementation("io.coil-kt.coil3:coil-svg:$coilVersion")

    implementation("com.google.dagger:hilt-android:2.60.1")
    ksp("com.google.dagger:hilt-compiler:2.60.1")
    kspAndroidTest("com.google.dagger:hilt-compiler:2.60.1")

    implementation("io.ktor:ktor-client-android:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.datastore:datastore-preferences-core:1.2.1")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")

    androidTestImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test:rules:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.60.1")
}
