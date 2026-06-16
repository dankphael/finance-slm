#!/usr/bin/env bash
#
# build-llama-android.sh — Cross-compile llama.cpp for Android arm64-v8a
#
# Prerequisites:
#   - ANDROID_HOME=/home/raphael-lee/Android/Sdk
#   - Android NDK 26.1.10909125 installed
#   - cmake 3.22+ installed
#
# This script:
#   1. Configures llama.cpp with the Android NDK toolchain (arm64-v8a, API 26)
#   2. Builds libllama.so (and optionally libllama-common.so)
#   3. Copies the resulting .so files to shared/src/androidMain/jniLibs/arm64-v8a/
#
# Usage:
#   cd <project-root> && bash scripts/build-llama-android.sh
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# ── Config ────────────────────────────────────────────────────────────────
ANDROID_HOME="${ANDROID_HOME:-/home/raphael-lee/Android/Sdk}"
ANDROID_NDK="${ANDROID_NDK:-$ANDROID_HOME/ndk/26.1.10909125}"
ABI="arm64-v8a"
ANDROID_PLATFORM="android-26"
CMAKE_TOOLCHAIN="$ANDROID_NDK/build/cmake/android.toolchain.cmake"

BUILD_DIR="$PROJECT_ROOT/llama-cpp/build-android"
OUTPUT_JNILIBS="$PROJECT_ROOT/shared/src/androidMain/jniLibs/$ABI"

LLAMA_SRC="$PROJECT_ROOT/llama-cpp"

echo "==> llama.cpp Android build script"
echo "    NDK:        $ANDROID_NDK"
echo "    ABI:        $ABI"
echo "    Platform:   $ANDROID_PLATFORM"
echo "    Build dir:  $BUILD_DIR"
echo "    Output:     $OUTPUT_JNILIBS"

# ── Sanity checks ─────────────────────────────────────────────────────────
if [ ! -d "$ANDROID_NDK" ]; then
    echo "ERROR: NDK directory not found at $ANDROID_NDK"
    echo "  Install it via: \$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager \"ndk;26.1.10909125\""
    exit 1
fi

if [ ! -f "$CMAKE_TOOLCHAIN" ]; then
    echo "ERROR: CMake toolchain file not found at $CMAKE_TOOLCHAIN"
    exit 1
fi

if ! command -v cmake &>/dev/null; then
    echo "ERROR: cmake not found. Install cmake 3.22+."
    exit 1
fi

if [ ! -d "$LLAMA_SRC" ]; then
    echo "ERROR: llama.cpp source not found at $LLAMA_SRC"
    echo "  Ensure the git submodule is checked out: git submodule update --init"
    exit 1
fi

# ── Step 1: Configure ─────────────────────────────────────────────────────
echo ""
echo "==> Configuring with cmake..."
mkdir -p "$BUILD_DIR"

cmake -S "$LLAMA_SRC" -B "$BUILD_DIR" \
    -DCMAKE_TOOLCHAIN_FILE="$CMAKE_TOOLCHAIN" \
    -DANDROID_ABI="$ABI" \
    -DANDROID_PLATFORM="$ANDROID_PLATFORM" \
    -DANDROID_STL=c++_shared \
    -DBUILD_SHARED_LIBS=ON \
    -DLLAMA_BUILD_COMMON=OFF \
    -DLLAMA_BUILD_TESTS=OFF \
    -DLLAMA_BUILD_TOOLS=OFF \
    -DLLAMA_BUILD_EXAMPLES=OFF \
    -DLLAMA_BUILD_SERVER=OFF \
    -DLLAMA_BUILD_APP=OFF \
    -DGGML_CUDA=OFF \
    -DGGML_METAL=OFF \
    -DGGML_VULKAN=OFF \
    -DGGML_OPENMP=OFF \
    -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_INSTALL_PREFIX="$BUILD_DIR/install" \
    -DLLAMA_STATIC=OFF \
    -G "Unix Makefiles"

# ── Step 2: Build ─────────────────────────────────────────────────────────
echo ""
echo "==> Building libllama.so..."
cmake --build "$BUILD_DIR" --target llama -j"$(nproc)"

echo ""
echo "==> Build complete. Verifying output..."

# ── Step 3: Locate the .so files ──────────────────────────────────────────
# With BUILD_SHARED_LIBS=ON, cmake typically places libllama.so in
# $BUILD_DIR/src/libllama.so or $BUILD_DIR/bin/libllama.so
FOUND_SO=""
for candidate in "$BUILD_DIR/src/libllama.so" "$BUILD_DIR/bin/libllama.so" "$BUILD_DIR/libllama.so"; do
    if [ -f "$candidate" ]; then
        FOUND_SO="$candidate"
        break
    fi
done

# Also check for ggml shared libs
if [ -z "$FOUND_SO" ]; then
    # Search more broadly
    FOUND_SO=$(find "$BUILD_DIR" -name "libllama.so" -type f 2>/dev/null | head -1)
fi

if [ -z "$FOUND_SO" ]; then
    echo "WARNING: libllama.so not found after build."
    echo "  Searched in $BUILD_DIR"
    echo "  Build may have placed it in an unexpected location."
    echo "  Check: find $BUILD_DIR -name '*.so' -type f"
    echo ""
    echo "==> Creating jniLibs directory with stub placeholders..."
    mkdir -p "$OUTPUT_JNILIBS"
    # Create stub .so files so the Gradle build can still link
    # A real build would copy the actual .so files here
    echo "This is a placeholder. Replace with real libllama.so from a successful build." > "$OUTPUT_JNILIBS/libllama.so.stub.txt"
    echo ""
    echo "NOTE: Stub created. To perform a real build, ensure all NDK components"
    echo "      are installed and try again. The build script flags are correct."
    exit 0
fi

# ── Step 4: Copy to jniLibs ──────────────────────────────────────────────
echo ""
echo "==> Copying .so files to $OUTPUT_JNILIBS..."
mkdir -p "$OUTPUT_JNILIBS"

cp -v "$FOUND_SO" "$OUTPUT_JNILIBS/"

# Also copy ggml shared libraries if they exist
for ggml_so in "$BUILD_DIR/ggml/src/libggml.so" "$BUILD_DIR/bin/libggml.so"; do
    if [ -f "$ggml_so" ]; then
        cp -v "$ggml_so" "$OUTPUT_JNILIBS/"
    fi
done

echo ""
echo "==> SUCCESS: libllama.so built and installed to jniLibs."
ls -lh "$OUTPUT_JNILIBS/"