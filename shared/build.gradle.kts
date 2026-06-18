plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
                }
            }
        }
    }

    // iOS targets are always declared so the source set exists, but the
    // framework binary + llama.cpp cinterop + native link options are only
    // configured on a macOS host. Apple targets cannot be built on Linux anyway,
    // so this keeps the Android/Linux CI pipeline completely unaffected.
    val iosTargets = listOf(
        iosArm64(),
        iosSimulatorArm64()
    )
    if (org.jetbrains.kotlin.konan.target.HostManager.hostIsMac) {
        iosTargets.forEach { iosTarget ->
            iosTarget.binaries.framework {
                baseName = "Shared"
                isStatic = true
            }

            iosTarget.compilations.getByName("main").cinterops.create("llama") {
                defFile(project.file("src/nativeInterop/cinterop/llama.def"))
                includeDirs(
                    project.file("llama-cpp/include"),
                    project.file("llama-cpp/ggml/include")
                )
            }

            // Link the per-architecture llama.cpp static libs produced by
            // scripts/build-llama-ios.sh. (CPU-only first; Metal frameworks are
            // linked but unused until a Metal build is enabled.)
            val libSubdir = if (iosTarget.name == "iosArm64") "ios-arm64" else "ios-simulator-arm64"
            iosTarget.binaries.all {
                linkerOpts(
                    "-L${project.projectDir}/build/llama/$libSubdir/lib",
                    "-lllama", "-lggml", "-lggml-base", "-lggml-cpu",
                    "-framework", "Foundation",
                    "-framework", "Accelerate",
                    "-framework", "Metal",
                    "-framework", "MetalKit"
                )
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.multiplatform.settings)
            implementation("com.russhwolf:multiplatform-settings-no-arg:1.2.0")
            implementation(libs.okio)
            implementation(libs.koin.core)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.coroutines.test)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqldelight.android.driver)
            implementation(libs.kotlinx.coroutines.android)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native.driver)
        }
    }
}

sqldelight {
    databases {
        create("FinanceSlmDatabase") {
            packageName.set("com.habibi.financeslm.db")
            srcDirs.setFrom("src/commonMain/sqldelight")
        }
    }
}

android {
    namespace = "com.habibi.financeslm.shared"
    compileSdk = 34

    ndkVersion = "26.1.10909125"

    defaultConfig {
        minSdk = 26

        ndk {
            abiFilters += listOf("arm64-v8a")
        }

        externalNativeBuild {
            cmake {
                arguments += "-DCMAKE_BUILD_TYPE=Release"
                arguments += "-DANDROID_STL=c++_shared"
                cppFlags += "-std=c++17"
            }
        }
    }

    externalNativeBuild {
        cmake {
            path("src/androidMain/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/androidMain/jniLibs")
        }
    }
}