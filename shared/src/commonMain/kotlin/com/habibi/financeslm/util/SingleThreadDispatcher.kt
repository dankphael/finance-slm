package com.habibi.financeslm.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.newSingleThreadContext

/**
 * Single-thread dispatcher for ALL llama.cpp inference calls.
 *
 * llama.cpp is NOT thread-safe — concurrent calls on the same model context
 * cause data corruption or SIGSEGV. Every invocation of native JNI methods
 * MUST go through this dispatcher.
 *
 * This creates exactly one platform thread (newSingleThreadContext), so even
 * if multiple coroutines invoke engine methods concurrently, they queue up
 * and execute serially.
 */
object SingleThreadDispatcher {
    val dispatcher: CoroutineDispatcher = newSingleThreadContext("llama-inference")
}