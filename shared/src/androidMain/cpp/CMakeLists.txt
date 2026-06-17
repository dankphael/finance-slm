cmake_minimum_required(VERSION 3.22.1)
project("llamajni" CXX)

# ── Find llama.cpp headers ─────────────────────────────────────────────────
# The llama-cpp submodule is at the project root.
set(LLAMA_CPP_SRC_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../../../llama-cpp")
set(LLAMA_INCLUDE_DIR "${LLAMA_CPP_SRC_DIR}/include")
set(LLAMA_GGML_INCLUDE_DIR "${LLAMA_CPP_SRC_DIR}/ggml/include")

# ── Prebuilt libllama.so from jniLibs ──────────────────────────────────────
set(JNI_LIBS_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}")

add_library(llama SHARED IMPORTED)
set_target_properties(llama PROPERTIES
    IMPORTED_LOCATION "${JNI_LIBS_DIR}/libllama.so"
    INTERFACE_INCLUDE_DIRECTORIES "${LLAMA_INCLUDE_DIR};${LLAMA_GGML_INCLUDE_DIR}"
)

# ── JNI wrapper library ────────────────────────────────────────────────────
add_library(llamajni SHARED
    llama_jni.cpp
    crash_handler.cpp
)

target_include_directories(llamajni PRIVATE
    ${LLAMA_INCLUDE_DIR}
    ${LLAMA_GGML_INCLUDE_DIR}
)

target_link_libraries(llamajni
    llama               # link against prebuilt libllama.so
    log                 # Android logging (__android_log_print)
)

# Ensure the output is libllamajni.so
set_target_properties(llamajni PROPERTIES
    LIBRARY_OUTPUT_NAME "llamajni"
    POSITION_INDEPENDENT_CODE ON
)