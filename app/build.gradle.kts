/*
 * SPDX-FileCopyrightText: 2025 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

import com.android.build.api.dsl.ApplicationExtension
import java.io.FileInputStream
import java.util.Properties
import java.util.regex.Pattern

val releaseKeystoreProps: Properties = Properties().also { props ->
    val file = rootProject.file("keystore.properties")
    if (file.exists()) {
        FileInputStream(file).use { props.load(it) }
    }
}

fun releaseKeystoreValue(name: String, envName: String): String? {
    val gradleValue = providers.gradleProperty(name).orNull?.takeIf { it.isNotBlank() }
    if (gradleValue != null) {
        return gradleValue
    }
    val envValue = providers.environmentVariable(envName).orNull?.takeIf { it.isNotBlank() }
    if (envValue != null) {
        return envValue
    }
    return releaseKeystoreProps.getProperty(name)?.takeIf { it.isNotBlank() }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.android.legacy.kapt)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.jetbrains.kotlin.parcelize)
    alias(libs.plugins.jetbrains.kotlinx.serialization)
    alias(libs.plugins.sonarqube)
    alias(libs.plugins.about.libraries)
    checkstyle
}

val gitWorkingBranch = providers.exec {
    commandLine("git", "rev-parse", "--abbrev-ref", "HEAD")
}.standardOutput.asText.map { it.trim() }
val defaultBranches = listOf("master", "dev", "main")
val workingBranch = gitWorkingBranch.getOrElse("")
val normalizedWorkingBranch = workingBranch
    .replaceFirst("^[^A-Za-z]+".toRegex(), "")
    .replace("[^0-9A-Za-z]+".toRegex(), "")

kotlin {
    jvmToolchain(21)
}

configure<ApplicationExtension> {
    compileSdk {
        version = release(URMIX_VERSION_SDK_COMPILE_MAJOR) {
            minorApiLevel = URMIX_VERSION_SDK_COMPILE_MINOR
        }
    }
    namespace = URMIX_APPLICATION_ID

    defaultConfig {
        applicationId = URMIX_APPLICATION_ID
        resValue("string", "app_name", "URMIX")
        minSdk {
            version = release(URMIX_VERSION_SDK_MIN)
        }
        targetSdk {
            version = release(URMIX_VERSION_SDK_TARGET)
        }

        versionCode = System.getProperty("versionCodeOverride")?.toInt() ?: URMIX_VERSION_CODE

        versionName = URMIX_VERSION_NAME
        System.getProperty("versionNameSuffix")?.let { versionNameSuffix = it }

        // FASE 6 §7.3: bundled extractor version for the min_extractor_version
        // gate (catalog teamnewpipe-newpipe-extractor, e.g. "v0.26.5").
        buildConfigField(
            "String",
            "EXTRACTOR_VERSION",
            "\"" + libs.versions.teamnewpipe.newpipe.extractor.get().trimStart('v') + "\""
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // FASE 6 §7.1: one consistent release keystore. Created only when all
        // four values resolve (gradle -P > env > keystore.properties); without
        // them the release build keeps the previous unsigned behaviour so
        // debug/CI builds never break. The keystore itself is git-ignored.
        val keystoreFile = releaseKeystoreValue("storeFile", "URMIX_KEYSTORE_PATH")
        val keystorePassword = releaseKeystoreValue("storePassword", "URMIX_KEYSTORE_PASSWORD")
        val keyAliasValue = releaseKeystoreValue("keyAlias", "URMIX_KEY_ALIAS")
        val keyPasswordValue = releaseKeystoreValue("keyPassword", "URMIX_KEY_PASSWORD")
        if (keystoreFile != null && keystorePassword != null
            && keyAliasValue != null && keyPasswordValue != null
        ) {
            create("release") {
                storeFile = rootProject.file(keystoreFile)
                storePassword = keystorePassword
                keyAlias = keyAliasValue
                keyPassword = keyPasswordValue
            }
        } else {
            logger.warn(
                "FASE 6 §7.1: release signing not configured " +
                    "(storeFile/storePassword/keyAlias/keyPassword); " +
                    "assembleRelease will be unsigned."
            )
        }
    }

    buildTypes {
        debug {
            isDebuggable = true

            // suffix the app id and the app name with git branch name
            if (normalizedWorkingBranch.isEmpty() || workingBranch in defaultBranches) {
                applicationIdSuffix = ".debug"
                resValue("string", "app_name", "URMIX Debug")
            } else {
                applicationIdSuffix = ".debug.$normalizedWorkingBranch"
                resValue("string", "app_name", "URMIX $workingBranch")
            }
        }

        release {
            signingConfig = signingConfigs.findByName("release")
            System.getProperty("packageSuffix")?.let { suffix ->
                applicationIdSuffix = suffix
                resValue("string", "app_name", "URMIX $suffix")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        register("continuous") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            isDefault = true

            // suffix the app id and the app name with git branch name
            if (normalizedWorkingBranch.isEmpty() || workingBranch in defaultBranches) {
                applicationIdSuffix = ".continuous"
                resValue("string", "app_name", "URMIX Continuous")
            } else {
                applicationIdSuffix = ".continuous.$normalizedWorkingBranch"
                resValue("string", "app_name", "URMIX $workingBranch")
            }
        }
    }

    lint {
        lintConfig = file("lint.xml")
        // Continue the debug build even when errors are found
        abortOnError = false
    }

    compileOptions {
        // Flag to enable support for the new language APIs
        isCoreLibraryDesugaringEnabled = true
        encoding = "utf-8"
    }

    sourceSets {
        getByName("androidTest") {
            assets.directories += "$projectDir/schemas"
        }
    }

    androidResources {
        generateLocaleConfig = true
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        resValues = true
    }

    packaging {
        resources {
            // remove two files which belong to jsoup
            // no idea how they ended up in the META-INF dir...
            excludes += setOf(
                "META-INF/README.md",
                "META-INF/CHANGES",
                "META-INF/COPYRIGHT" // "COPYRIGHT" belongs to RxJava...
            )
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}


// Custom dependency configuration for ktlint
val ktlint by configurations.creating

checkstyle {
    configDirectory = rootProject.file("checkstyle")
    isIgnoreFailures = false
    isShowViolations = true
    toolVersion = libs.versions.checkstyle.get()
}

tasks.register<Checkstyle>("runCheckstyle") {
    source("src")
    include("**/*.java")
    exclude("**/gen/**")
    exclude("**/R.java")
    exclude("**/BuildConfig.java")
    exclude("main/java/us/shandian/giga/**")

    classpath = configurations.getByName("checkstyle")

    isShowViolations = true

    reports {
        xml.required = true
        html.required = true
    }
}

val outputDir = project.layout.buildDirectory.dir("reports/ktlint/")
val inputFiles = fileTree("src") { include("**/*.kt") }

tasks.register<JavaExec>("runKtlint") {
    inputs.files(inputFiles)
    outputs.dir(outputDir)
    mainClass.set("com.pinterest.ktlint.Main")
    classpath = configurations.getByName("ktlint")
    args = listOf("--editorconfig=../.editorconfig", "src/**/*.kt")
    jvmArgs = listOf("--add-opens", "java.base/java.lang=ALL-UNNAMED")
}

tasks.register<JavaExec>("formatKtlint") {
    inputs.files(inputFiles)
    outputs.dir(outputDir)
    mainClass.set("com.pinterest.ktlint.Main")
    classpath = configurations.getByName("ktlint")
    args = listOf("--editorconfig=../.editorconfig", "-F", "src/**/*.kt")
    jvmArgs = listOf("--add-opens", "java.base/java.lang=ALL-UNNAMED")
}

tasks.register<CheckDependenciesOrder>("checkDependenciesOrder") {
    tomlFile = layout.projectDirectory.file("../gradle/libs.versions.toml")
}

afterEvaluate {
    tasks.named("preDebugBuild").configure {
        if (!System.getProperties().containsKey("skipFormatKtlint")) {
            dependsOn("formatKtlint")
        }
        dependsOn("runCheckstyle", "runKtlint", "checkDependenciesOrder")
    }
}

sonar {
    properties {
        property("sonar.projectKey", "Winatra_URMIX")
        property("sonar.organization", "winatra")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}

dependencies {
    // Desugaring
    coreLibraryDesugaring(libs.android.desugar)

    // NewPipe libraries
    implementation(projects.shared)
    implementation(libs.newpipe.nanojson)
    implementation(libs.newpipe.extractor)
    implementation(libs.newpipe.filepicker)

    // Checkstyle
    checkstyle(libs.puppycrawl.checkstyle)
    ktlint(libs.pinterest.ktlint)

    // AndroidX
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.localbroadcastmanager)
    implementation(libs.androidx.media)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.rxjava3)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.work.rxjava3)
    implementation(libs.google.android.material)
    implementation(libs.androidx.webkit)

    // Coroutines interop
    implementation(libs.kotlinx.coroutines.rx3)

    // Kotlinx Serialization
    implementation(libs.kotlinx.serialization.json)

    // Third-party libraries
    implementation(libs.livefront.bridge)
    implementation(libs.evernote.statesaver.core)
    kapt(libs.evernote.statesaver.compiler)

    // HTML parser
    implementation(libs.jsoup)

    // Text similarity used by PreferenceFuzzySearchFunction. Was dropped during
    // the NewPipe graft; without it full javac builds fail.
    implementation(libs.apache.commons.text)

    // HTTP client
    implementation(libs.squareup.okhttp)

    // Media player
    implementation(libs.google.exoplayer.core)
    implementation(libs.google.exoplayer.dash)
    implementation(libs.google.exoplayer.database)
    implementation(libs.google.exoplayer.datasource)
    implementation(libs.google.exoplayer.hls)
    implementation(libs.google.exoplayer.mediasession)
    implementation(libs.google.exoplayer.smoothstreaming)
    implementation(libs.google.exoplayer.ui)

    // Manager for complex RecyclerView layouts
    implementation(libs.lisawray.groupie.core)
    implementation(libs.lisawray.groupie.viewbinding)

    // Image loading
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Markdown library for Android
    implementation(libs.noties.markwon.core)
    implementation(libs.noties.markwon.linkify)

    // Crash reporting
    implementation(libs.acra.core)
    compileOnly(libs.google.autoservice.annotations)
    ksp(libs.zacsweers.autoservice.compiler)

    // Properly restarting
    implementation(libs.jakewharton.phoenix)

    // Reactive extensions for Java VM
    implementation(libs.reactivex.rxjava)
    implementation(libs.reactivex.rxandroid)
    // RxJava binding APIs for Android UI widgets
    implementation(libs.jakewharton.rxbinding)

    // Date and time formatting
    implementation(libs.ocpsoft.prettytime)

    // Debugging and memory leak detection
    debugImplementation(libs.squareup.leakcanary.watcher)
    debugImplementation(libs.squareup.leakcanary.plumber)
    debugImplementation(libs.squareup.leakcanary.core)
    // Debug bridge for Android
    debugImplementation(libs.facebook.stetho.core)
    debugImplementation(libs.facebook.stetho.okhttp3)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.assertj.core)
}

aboutLibraries {
    collect {
        configPath = file("../config/aboutlibraries")
    }
    export {
        outputFile = file("../shared/src/androidMain/assets/aboutlibraries.json")
        prettyPrint = true
        excludeFields.addAll("organization", "scm", "funding")
    }
    library {
        exclusionPatterns = listOf(
            Pattern.compile("^com\\.github\\.TeamNewPipe:NewPipeExtractor$"),
            Pattern.compile("^com\\.evernote:android-state$")
        )
    }
}
