package com.habibi.financeslm.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** Cancellation handle handed to Swift to stop an in-flight [SwiftFlow] subscription. */
class Cancellation(private val job: Job) {
    fun cancel() {
        job.cancel()
    }
}

/**
 * Bridges a Kotlin [Flow] to Swift, which cannot collect a Flow directly.
 * Collection runs on the main dispatcher and reports via plain closures.
 *
 *   let sub = SwiftFlow(flow).subscribe(onEach: { ... }, onComplete: { ... }, onError: { ... })
 *   sub.cancel()
 */
class SwiftFlow<T : Any>(private val flow: Flow<T>) {
    fun subscribe(
        onEach: (T) -> Unit,
        onComplete: () -> Unit,
        onError: (Throwable) -> Unit
    ): Cancellation {
        val job = CoroutineScope(Dispatchers.Main).launch {
            try {
                flow.collect { onEach(it) }
                onComplete()
            } catch (e: Throwable) {
                onError(e)
            }
        }
        return Cancellation(job)
    }
}
