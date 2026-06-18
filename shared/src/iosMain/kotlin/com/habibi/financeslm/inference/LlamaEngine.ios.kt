@file:OptIn(ExperimentalForeignApi::class)

package com.habibi.financeslm.inference

import com.habibi.financeslm.util.Logger
import com.habibi.financeslm.util.SingleThreadDispatcher
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.FloatVar
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.set
import kotlinx.cinterop.useContents
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import llama.cpp.*

/**
 * iOS LlamaEngine — Kotlin/Native cinterop binding to llama.cpp's C API.
 *
 * This is a faithful port of the Android JNI bridge (shared/src/androidMain/cpp/
 * llama_jni.cpp): backend init -> model/context load -> tokenize -> decode loop
 * with a top-k/top-p/temp/dist sampler chain. All native calls are confined to a
 * single-thread dispatcher because llama.cpp is NOT thread-safe.
 *
 * NOTE: only compiles on a Mac (needs the llama.xcframework from
 * scripts/build-llama-ios.sh and the cinterop klib). A few C symbol shapes
 * (notably the LoRA adapter API) vary across llama.cpp versions and may need
 * minor Mac-side adjustment.
 */
class IosLlamaEngine : LlamaEngine {

    private var model: CPointer<llama_model>? = null
    private var context: CPointer<llama_context>? = null
    private var loadedModelPath: String? = null

    private val dispatcher = SingleThreadDispatcher.dispatcher

    override suspend fun loadModel(path: String, config: LlamaConfig): Boolean = withContext(dispatcher) {
        freeNative()
        llama_backend_init()

        val loadedModel = llama_model_default_params().useContents {
            n_gpu_layers = config.gpuLayers
            llama_model_load_from_file(path, this.readValue())
        }
        if (loadedModel == null) {
            Logger.e("LlamaEngine(iOS)", "Failed to load model: $path")
            return@withContext false
        }
        model = loadedModel

        val createdContext = llama_context_default_params().useContents {
            n_ctx = config.contextSize.toUInt()
            n_batch = config.batchSize.toUInt()
            n_ubatch = config.batchSize.toUInt()
            n_threads = config.threadCount
            n_threads_batch = config.threadCount
            llama_init_from_model(loadedModel, this.readValue())
        }
        if (createdContext == null) {
            llama_model_free(loadedModel)
            model = null
            Logger.e("LlamaEngine(iOS)", "Failed to create context")
            return@withContext false
        }
        context = createdContext
        loadedModelPath = path
        Logger.d("LlamaEngine(iOS)", "Model loaded: $path")
        true
    }

    override suspend fun unloadModel() = withContext(dispatcher) {
        freeNative()
        loadedModelPath = null
    }

    override suspend fun isLoaded(): Boolean = model != null && context != null

    override suspend fun infer(prompt: String, params: InferenceParams): Flow<String> = flow {
        val output = StringBuilder()
        withContext(dispatcher) {
            runGeneration(prompt, params) { output.append(it) }
        }
        emit(output.toString())
    }

    override suspend fun inferStreaming(prompt: String, params: InferenceParams): Flow<String> = callbackFlow {
        withContext(dispatcher) {
            runGeneration(prompt, params) { token -> trySend(token) }
        }
        close()
    }

    override suspend fun tokenize(text: String): List<Int> = withContext(dispatcher) {
        val m = model ?: return@withContext emptyList()
        val vocab = llama_model_get_vocab(m) ?: return@withContext emptyList()
        tokenizeToArray(vocab, text, addBos = false).toList()
    }

    override suspend fun applyLora(loraPath: String): Boolean = withContext(dispatcher) {
        val m = model
        val c = context
        if (m == null || c == null) return@withContext false

        val adapter = llama_adapter_lora_init(m, loraPath)
        if (adapter == null) {
            Logger.e("LlamaEngine(iOS)", "Failed to load LoRA: $loraPath")
            return@withContext false
        }
        memScoped {
            val adapters = allocArray<CPointerVar<llama_adapter_lora>>(1)
            adapters[0] = adapter
            val scales = allocArray<FloatVar>(1)
            scales[0] = 1.0f
            val ret = llama_set_adapters_lora(c, adapters, 1.convert(), scales)
            ret == 0
        }
    }

    override fun getLoadedModelPath(): String? = loadedModelPath

    // ── internals ────────────────────────────────────────────────────────────

    private fun freeNative() {
        context?.let { llama_free(it) }
        model?.let { llama_model_free(it) }
        context = null
        model = null
    }

    private fun tokenizeToArray(
        vocab: CPointer<llama_vocab>,
        text: String,
        addBos: Boolean
    ): IntArray = memScoped {
        val byteLen = text.encodeToByteArray().size
        val capacity = byteLen + 4
        val buffer = allocArray<IntVar>(capacity)
        var n = llama_tokenize(vocab, text, byteLen, buffer, capacity, addBos, false)
        if (n < 0) {
            val needed = -n
            val bigger = allocArray<IntVar>(needed)
            n = llama_tokenize(vocab, text, byteLen, bigger, needed, addBos, false)
            IntArray(if (n > 0) n else 0) { bigger[it] }
        } else {
            IntArray(n) { buffer[it] }
        }
    }

    private fun runGeneration(prompt: String, params: InferenceParams, onToken: (String) -> Unit) {
        val m = model
        val c = context
        if (m == null || c == null) {
            onToken("[error] Model not loaded")
            return
        }
        val vocab = llama_model_get_vocab(m)
        if (vocab == null) {
            onToken("[error] No vocabulary")
            return
        }

        val promptTokens = tokenizeToArray(vocab, prompt, addBos = true)
        if (promptTokens.isEmpty()) {
            onToken("[error] Tokenization failed")
            return
        }

        memScoped {
            // Evaluate the prompt.
            val promptBuf = allocArray<IntVar>(promptTokens.size)
            for (i in promptTokens.indices) promptBuf[i] = promptTokens[i]
            if (llama_decode(c, llama_batch_get_one(promptBuf, promptTokens.size)) != 0) {
                onToken("[error] Prompt evaluation failed")
                return
            }

            // Sampler chain: top-k -> top-p -> temperature -> distribution.
            val smpl = llama_sampler_chain_init(llama_sampler_chain_default_params())
            if (params.topK > 0) {
                llama_sampler_chain_add(smpl, llama_sampler_init_top_k(params.topK))
            }
            llama_sampler_chain_add(smpl, llama_sampler_init_top_p(params.topP, 1.convert()))
            llama_sampler_chain_add(smpl, llama_sampler_init_temp(params.temperature))
            llama_sampler_chain_add(smpl, llama_sampler_init_dist(LLAMA_DEFAULT_SEED))

            val eos = llama_vocab_eos(vocab)
            val one = allocArray<IntVar>(1)
            val pieceBuf = allocArray<ByteVar>(256)

            for (i in 0 until params.maxTokens) {
                val id = llama_sampler_sample(smpl, c, -1)
                if (id == eos) break

                val len = llama_token_to_piece(vocab, id, pieceBuf, 256, 0, false)
                if (len > 0) {
                    onToken(pieceBuf.readBytes(len).decodeToString())
                }

                one[0] = id
                if (llama_decode(c, llama_batch_get_one(one, 1)) != 0) break
            }

            llama_sampler_free(smpl)
        }
    }
}

actual fun createLlamaEngine(): LlamaEngine = IosLlamaEngine()
