cmake_minimum_required(VERSION 3.22.1)
project("financeslm")

# C++17
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Source files
set(LLAMA_SOURCES
    llama_engine_jni.cpp
    crash_handler.cpp
)

# Build shared library
add_library(
    llama_engine
    SHARED
    ${LLAMA_SOURCES}
)

# Find JNI
find_package(JNI REQUIRED)

# Include directories
target_include_directories(
    llama_engine
    PRIVATE
        ${JNI_INCLUDE_DIRS}
        ${CMAKE_CURRENT_SOURCE_DIR}
)

# Link libraries
target_link_libraries(
    llama_engine
    ${JNI_LIBRARIES}
    log
    dl
)

# ARM NEON optimizations
target_compile_options(
    llama_engine
    PRIVATE
        -Wall -Werror
        -fvisibility=hidden
        -fstack-protector-strong
)

# Platform-specific
if(ANDROID_ABI STREQUAL "arm64-v8a")
    target_compile_options(llama_engine PRIVATE -march=armv8.2-a+fp16+rcpc+dotprod)
endif()
