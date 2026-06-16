package com.habibi.financeslm.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Named coroutine dispatchers for different types of work.
 */
object CoroutineDispatchers {
    val IO: CoroutineDispatcher get() = Dispatchers.IO
    val Computation: CoroutineDispatcher get() = Dispatchers.Default
    val Inference: CoroutineDispatcher get() = Dispatchers.Default // Overridden per-platform
    val Main: CoroutineDispatcher get() = Dispatchers.Main
}