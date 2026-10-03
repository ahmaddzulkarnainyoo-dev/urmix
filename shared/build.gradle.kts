/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.multiplatform)
    alias(libs.plugins.jetbrains.kotlin.compose)
    alias(libs.plugins.jetbrains.compose.multiplatform)
    alias(libs.plugins.koin)
    alias(libs.plugins.jetbrains.kotlinx.serialization)
    alias(libs.plugins.about.libraries)
}

// Writes the BuildConfig.kt that commonMain compiles. Deliberately NOT a
// Copy/Sync of `resources.text.fromString(...)`: that text resource is
// materialised inside a build directory, so running `clean` in the same
// invocation (what release.yml does: `clean assembleRelease`) wipes it and the
// task silently resolves NO-SOURCE, leaving `BuildConfig` unresolved in the
// release variant only. Writing the file from doLast is deterministic and
// survives `clean`.
val buildConfigGenerator by tasks.registering {
    val buildConfigPackage = URMIX_APPLICATION_ID_SHARED
    val rawClass = """
        package $buildConfigPackage

        object BuildConfig {
            const val VERSION_NAME = "$URMIX_VERSION_NAME"
            const val APP_NAME = "URMIX"
        }
    """.trimIndent()
    val outputDir = layout.buildDirectory.dir("generated/kotlin")
    outputs.dir(outputDir)
    doLast {
        val target = outputDir.get().asFile.resolve(buildConfigPackage.replace(".", "/"))
        target.mkdirs()
        target.resolve("BuildConfig.kt").writeText(rawClass)
    }
}

kotlin {
    jvmToolchain(21)

    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xexpect-actual-classes",
            "-Xexplicit-backing-fields"
        )
        optIn.addAll(
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
            "androidx.compose.foundation.layout.ExperimentalLayoutApi"
        )
    }
}

// Assign the generated compose resources Res class to the URMIX namespace
// (the default would otherwise be derived from the (renamed) root project name).
compose.resources {
    packageOfResClass = "com.winatra.urmix.shared.generated.resources"
}

kotlin {

    android {
        namespace = URMIX_APPLICATION_ID_SHARED
        compileSdk {
            version = release(URMIX_VERSION_SDK_COMPILE_MAJOR) {
                minorApiLevel = URMIX_VERSION_SDK_COMPILE_MINOR
            }
        }
        minSdk {
            version = release(URMIX_VERSION_SDK_MIN)
        }
        androidResources {
            enable = true
        }

        optimization {
            consumerKeepRules.apply {
                publish = true
                file("consumer-proguard-rules.pro")
            }
        }

        withHostTest {
            isIncludeAndroidResources = true
        }
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    jvm()

    sourceSets {
        commonMain {
            kotlin.srcDir(buildConfigGenerator.map { it.outputs.files.singleFile })
            dependencies {
                implementation(libs.jetbrains.compose.runtime)
                implementation(libs.jetbrains.compose.foundation)
                implementation(libs.jetbrains.compose.material3)
                implementation(libs.jetbrains.compose.ui)
                implementation(libs.jetbrains.compose.resources)
                implementation(libs.jetbrains.compose.preview)

                implementation(libs.jetbrains.lifecycle.viewmodel)

                // Use API as java compiler cannot see NavKey for some reason
                api(libs.jetbrains.navigation3.ui)
                implementation(libs.jetbrains.lifecycle.navigation3)
                implementation(libs.kotlinx.serialization.json)

                implementation(libs.koin.compose.navigation3)
                implementation(libs.koin.compose.viewmodel)
                implementation(libs.koin.annotations)

                implementation(libs.russhwolf.settings.core)
                implementation(libs.touchlab.kermit)
            }
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test.core)
            implementation(libs.jetbrains.compose.test.ui)
            implementation(libs.russhwolf.settings.test)
        }
        androidMain.dependencies {
            implementation(libs.jetbrains.compose.preview)
            implementation(libs.androidx.activity)
            implementation(libs.androidx.preference)
            implementation(libs.androidx.browser)
        }
        val androidDeviceTest by getting {
            dependencies {
                implementation(libs.androidx.compose.test.ui.manifest)
                implementation(libs.androidx.compose.test.ui.junit)

                // Needed because androidx.compose.test.ui.junit pulls an older dependency
                // which crashes on new Android versions
                implementation(libs.androidx.test.espresso.core)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.jetbrains.compose.tooling)
}

aboutLibraries {
    export {
        outputFile = file("src/iosMain/resources/aboutlibraries.json")
        prettyPrint = true
        variant = "metadataIosMain"
        excludeFields.addAll("organization", "scm", "funding")
    }
}
