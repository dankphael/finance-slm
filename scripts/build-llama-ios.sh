#!/usr/bin/env bash
#
# build-llama-ios.sh — build llama.cpp static libraries for iOS (device + simulator)
# from the llama-cpp/ git submodule, for the Kotlin/Native cinterop to link against.
#
# REQUIRES macOS + Xcode + CMake. This cannot run on Linux/CI.
#
# Outputs per-architecture static libs to:
#   shared/build/llama/ios-arm64/lib/*.a            (device, arm64)
#   shared/build/llama/ios-simulator-arm64/lib/*.a  (simulator, Apple Silicon)
# matching the linkerOpts paths in shared/build.gradle.kts. It also assembles an
# llama.xcframework for convenience / direct Xcode consumption.
#
# Starts CPU-only (GGML_METAL=OFF) to keep the first iOS bring-up simple; flip
# GGML_METAL=ON later for GPU acceleration (also requires bundling the Metal
# shader library in the app).
#
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SRC_DIR="${ROOT_DIR}/llama-cpp"
OUT_DIR="${ROOT_DIR}/shared/build/llama"
MIN_IOS="15.0"

if [[ "$(uname)" != "Darwin" ]]; then
  echo "ERROR: this script must run on macOS (needs Xcode + iphoneos/simulator SDKs)." >&2
  exit 1
fi

if [[ ! -f "${SRC_DIR}/CMakeLists.txt" ]]; then
  echo "ERROR: llama.cpp submodule missing at ${SRC_DIR}." >&2
  echo "Run: git submodule update --init --depth 1 llama-cpp" >&2
  exit 1
fi

# Shared CMake feature flags (mirror the Android CMakeLists: libraries only).
COMMON_FLAGS=(
  -DBUILD_SHARED_LIBS=OFF
  -DLLAMA_BUILD_TESTS=OFF
  -DLLAMA_BUILD_EXAMPLES=OFF
  -DLLAMA_BUILD_TOOLS=OFF
  -DLLAMA_BUILD_SERVER=OFF
  -DLLAMA_BUILD_COMMON=OFF
  -DLLAMA_CURL=OFF
  -DGGML_METAL=OFF
  -DGGML_OPENMP=OFF
  -DGGML_NATIVE=OFF
  -DCMAKE_BUILD_TYPE=Release
)

build_arch() {
  local label="$1"        # ios-arm64 | ios-simulator-arm64
  local sysroot="$2"      # iphoneos | iphonesimulator
  local arch="$3"         # arm64

  local build_dir="${OUT_DIR}/${label}/build"
  local install_dir="${OUT_DIR}/${label}"
  echo "==> Building llama.cpp for ${label} (${sysroot}, ${arch})"
  rm -rf "${build_dir}"
  mkdir -p "${build_dir}"

  cmake -S "${SRC_DIR}" -B "${build_dir}" -G Xcode \
    "${COMMON_FLAGS[@]}" \
    -DCMAKE_SYSTEM_NAME=iOS \
    -DCMAKE_OSX_SYSROOT="${sysroot}" \
    -DCMAKE_OSX_ARCHITECTURES="${arch}" \
    -DCMAKE_OSX_DEPLOYMENT_TARGET="${MIN_IOS}" \
    -DCMAKE_XCODE_ATTRIBUTE_ONLY_ACTIVE_ARCH=NO

  cmake --build "${build_dir}" --config Release -- -quiet

  # Collect the produced .a files into <install_dir>/lib
  mkdir -p "${install_dir}/lib"
  find "${build_dir}" -name "*.a" -exec cp {} "${install_dir}/lib/" \;
  echo "    -> $(ls "${install_dir}/lib")"
}

build_arch "ios-arm64"            "iphoneos"        "arm64"
build_arch "ios-simulator-arm64"  "iphonesimulator" "arm64"

# Assemble an xcframework from the device lib (handy for direct Xcode use).
echo "==> Creating llama.xcframework"
XCF_DIR="${ROOT_DIR}/shared/src/iosMain/libs"
rm -rf "${XCF_DIR}/llama.xcframework"
mkdir -p "${XCF_DIR}"
xcodebuild -create-xcframework \
  -library "${OUT_DIR}/ios-arm64/lib/libllama.a" \
  -library "${OUT_DIR}/ios-simulator-arm64/lib/libllama.a" \
  -output "${XCF_DIR}/llama.xcframework" || \
  echo "NOTE: xcframework assembly is optional; cinterop links the per-arch .a files directly."

echo "Done. Static libs are under ${OUT_DIR}/<arch>/lib."
