cmake_minimum_required(VERSION 3.22.1)
project("llamajni" C CXX)

# ── llama.cpp source (git submodule at the project root) ────────────────────
set(LLAMA_CPP_SRC_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../../../llama-cpp")

if(NOT EXISTS "${LLAMA_CPP_SRC_DIR}/CMakeLists.txt")
    message(FATAL_ERROR
        "llama.cpp submodule not found at ${LLAMA_CPP_SRC_DIR}.\n"
        "Run: git submodule update --init --depth 1 llama-cpp")
endif()

# ── Build llama.cpp from source ─────────────────────────────────────────────
# These options must be set BEFORE add_subdirectory so they win over the
# defaults declared inside llama.cpp's CMakeLists. We build only the libraries
# we need (no tests/examples/tools/server) and disable host-only features that
# do not apply to an Android NDK cross-compile.
set(BUILD_SHARED_LIBS   ON  CACHE BOOL "" FORCE)  # produce libllama.so + libggml*.so
set(LLAMA_BUILD_TESTS   OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TOOLS   OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER  OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_COMMON  OFF CACHE BOOL "" FORCE)
set(LLAMA_CURL          OFF CACHE BOOL "" FORCE)
set(LLAMA_OPENSSL       OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP         OFF CACHE BOOL "" FORCE)  # NDK has no libomp by default
set(GGML_LLAMAFILE      OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE         OFF CACHE BOOL "" FORCE)  # no -march=native when cross-compiling

add_subdirectory("${LLAMA_CPP_SRC_DIR}" "${CMAKE_CURRENT_BINARY_DIR}/llama-build")

# ── JNI wrapper library ─────────────────────────────────────────────────────
add_library(llamajni SHARED
    llama_jni.cpp
    crash_handler.cpp
)

target_link_libraries(llamajni
    llama               # built from source above (transitively pulls in ggml)
    log                 # Android logging (__android_log_print)
)

set_target_properties(llamajni PROPERTIES
    LIBRARY_OUTPUT_NAME "llamajni"
    POSITION_INDEPENDENT_CODE ON
)
