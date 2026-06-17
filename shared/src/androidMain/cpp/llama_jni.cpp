//
// llama_jni.cpp — JNI bridge between Kotlin (LlamaEngineAndroid) and llama.cpp
//
// Uses JNI_OnLoad + RegisterNatives (NOT name mangling) to register native methods.
// This decouples the JNI function names from the Kotlin package/class names.
//
#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <cstring>

#include "llama.h"

#include "crash_handler.h"

#define LOG_TAG "LlamaEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// ── Global state ──────────────────────────────────────────────────────────
// The inference engine holds a single model+context at a time.
static struct llama_model   * g_model   = nullptr;
static struct llama_context * g_context = nullptr;

// ── Helper: convert Java String to C++ std::string ────────────────────────
static std::string jstring_to_string(JNIEnv * env, jstring jstr) {
    if (!jstr) return "";
    const char * chars = env->GetStringUTFChars(jstr, nullptr);
    std::string result(chars);
    env->ReleaseStringUTFChars(jstr, chars);
    return result;
}

// ── JNI: loadModel ────────────────────────────────────────────────────────
// Java: external fun loadModel(path: String, contextSize: Int, batchSize: Int,
//                              threadCount: Int, gpuLayers: Int): Boolean
extern "C" jboolean JNICALL
Java_com_habibi_financeslm_inference_LlamaEngineAndroid_nativeLoadModel(
    JNIEnv * env, jobject /*thiz*/,
    jstring path,
    jint contextSize,
    jint batchSize,
    jint threadCount,
    jint gpuLayers)
{
    std::string modelPath = jstring_to_string(env, path);
    LOGI("loadModel: %s (ctx=%d, batch=%d, threads=%d, gpuLayers=%d)",
         modelPath.c_str(), contextSize, batchSize, threadCount, gpuLayers);

    // Free any previously loaded model
    if (g_context) { llama_free(g_context); g_context = nullptr; }
    if (g_model)   { llama_model_free(g_model); g_model = nullptr; }

    // Initialize backend (idempotent — safe to call multiple times)
    llama_backend_init();

    // Model params
    llama_model_params mparams = llama_model_default_params();
    mparams.n_gpu_layers = static_cast<int32_t>(gpuLayers);

    // Load model
    g_model = llama_model_load_from_file(modelPath.c_str(), mparams);
    if (!g_model) {
        LOGE("Failed to load model: %s", modelPath.c_str());
        return JNI_FALSE;
    }
    LOGI("Model loaded successfully");

    // Context params
    llama_context_params cparams = llama_context_default_params();
    cparams.n_ctx           = static_cast<uint32_t>(contextSize);
    cparams.n_batch         = static_cast<uint32_t>(batchSize);
    cparams.n_ubatch        = static_cast<uint32_t>(batchSize);
    cparams.n_threads       = static_cast<int32_t>(threadCount);
    cparams.n_threads_batch = static_cast<int32_t>(threadCount);

    // Create context
    g_context = llama_init_from_model(g_model, cparams);
    if (!g_context) {
        LOGE("Failed to create context");
        llama_model_free(g_model);
        g_model = nullptr;
        return JNI_FALSE;
    }

    LOGI("Context created: n_ctx=%d, n_batch=%d", contextSize, batchSize);
    return JNI_TRUE;
}

// ── JNI: freeModel ────────────────────────────────────────────────────────
// Java: external fun freeModel()
extern "C" void JNICALL
Java_com_habibi_financeslm_inference_LlamaEngineAndroid_nativeFreeModel(
    JNIEnv * /*env*/, jobject /*thiz*/)
{
    LOGI("freeModel");
    if (g_context) {
        llama_free(g_context);
        g_context = nullptr;
    }
    if (g_model) {
        llama_model_free(g_model);
        g_model = nullptr;
    }
}

// ── JNI: generateStreaming ────────────────────────────────────────────────
// Java: external fun nativeGenerateStreaming(
//     prompt: String, maxTokens: Int, temperature: Float, topP: Float,
//     topK: Int, repeatPenalty: Float, callback: (String) -> Unit
// )
//
// Runs text generation and calls the callback for each decoded token piece.
// This replaces the blocking nativeGenerate for streaming UIs.
extern "C" void JNICALL
Java_com_habibi_financeslm_inference_LlamaEngineAndroid_nativeGenerateStreaming(
    JNIEnv * env, jobject /*thiz*/,
    jstring prompt,
    jint maxTokens,
    jfloat temperature,
    jfloat topP,
    jint topK,
    jfloat repeatPenalty,
    jobject callback)
{
    if (!g_model || !g_context) {
        LOGE("generateStreaming: model not loaded");
        // Call callback with empty string to signal error
        jclass callbackClass = env->GetObjectClass(callback);
        jmethodID acceptMethod = env->GetMethodID(callbackClass, "accept", "(Ljava/lang/String;)V");
        if (acceptMethod) {
            jstring empty = env->NewStringUTF("[error] Model not loaded");
            env->CallVoidMethod(callback, acceptMethod, empty);
            env->DeleteLocalRef(empty);
        }
        env->DeleteLocalRef(callbackClass);
        return;
    }

    // Resolve the callback's accept(String) method once
    jclass callbackClass = env->GetObjectClass(callback);
    jmethodID acceptMethod = env->GetMethodID(callbackClass, "accept", "(Ljava/lang/String;)V");
    if (!acceptMethod) {
        LOGE("generateStreaming: callback has no accept(String) method");
        env->DeleteLocalRef(callbackClass);
        return;
    }

    std::string promptStr = jstring_to_string(env, prompt);
    LOGI("generateStreaming: prompt_len=%zu, max_tokens=%d, temp=%.2f",
         promptStr.size(), maxTokens, temperature);

    // Tokenize the prompt
    const struct llama_vocab * vocab = llama_model_get_vocab(g_model);
    if (!vocab) {
        LOGE("generateStreaming: no vocab");
        jstring err = env->NewStringUTF("[error] No vocabulary");
        env->CallVoidMethod(callback, acceptMethod, err);
        env->DeleteLocalRef(err);
        env->DeleteLocalRef(callbackClass);
        return;
    }

    int32_t n_tokens = static_cast<int32_t>(promptStr.size()) + 4;
    std::vector<llama_token> tokens(n_tokens);

    int32_t actual = llama_tokenize(
        vocab,
        promptStr.c_str(),
        static_cast<int32_t>(promptStr.size()),
        tokens.data(),
        static_cast<int32_t>(tokens.size()),
        true,   // add_bos
        false   // parse_special
    );

    if (actual < 0) {
        tokens.resize(-actual);
        actual = llama_tokenize(
            vocab,
            promptStr.c_str(),
            static_cast<int32_t>(promptStr.size()),
            tokens.data(),
            static_cast<int32_t>(tokens.size()),
            true, false);
    }
    tokens.resize(actual);

    LOGI("generateStreaming: tokenized to %zu tokens", tokens.size());

    // Eval the prompt
    llama_batch batch = llama_batch_get_one(tokens.data(), static_cast<int32_t>(tokens.size()));
    if (llama_decode(g_context, batch) != 0) {
        LOGE("generateStreaming: prompt eval failed");
        jstring err = env->NewStringUTF("[error] Prompt evaluation failed");
        env->CallVoidMethod(callback, acceptMethod, err);
        env->DeleteLocalRef(err);
        env->DeleteLocalRef(callbackClass);
        return;
    }

    // Prepare sampler chain
    auto sparams = llama_sampler_chain_default_params();
    llama_sampler * smpl = llama_sampler_chain_init(sparams);

    if (topK > 0) {
        llama_sampler_chain_add(smpl, llama_sampler_init_top_k(static_cast<int32_t>(topK)));
    }
    llama_sampler_chain_add(smpl, llama_sampler_init_top_p(topP, 1));
    llama_sampler_chain_add(smpl, llama_sampler_init_temp(temperature));
    llama_sampler_chain_add(smpl, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));

    // Generate tokens — call callback for each decoded piece
    for (int32_t i = 0; i < maxTokens; i++) {
        const llama_token id = llama_sampler_sample(smpl, g_context, -1);

        if (id == llama_vocab_eos(vocab)) {
            LOGI("generateStreaming: EOS at token %d", i);
            break;
        }

        // Convert token to piece
        char buf[128];
        int32_t len = llama_token_to_piece(vocab, id, buf, sizeof(buf), 0, false);
        if (len > 0) {
            // Convert C string to Java string and pass to callback
            jstring tokenStr = env->NewStringUTF(std::string(buf, static_cast<size_t>(len)).c_str());
            env->CallVoidMethod(callback, acceptMethod, tokenStr);
            env->DeleteLocalRef(tokenStr);
        }

        // Prepare next batch with single token
        llama_token next_tokens[] = { id };
        batch = llama_batch_get_one(next_tokens, 1);
        if (llama_decode(g_context, batch) != 0) {
            LOGE("generateStreaming: decode failed at token %d", i);
            break;
        }
    }

    llama_sampler_free(smpl);
    LOGI("generateStreaming: completed %d tokens", maxTokens);

    env->DeleteLocalRef(callbackClass);
}

// ── JNI: generate (blocking) ─────────────────────────────────────────────
// Java: external fun nativeGenerate(prompt: String, maxTokens: Int,
//                                    temperature: Float, topP: Float, topK: Int,
//                                    repeatPenalty: Float): String
//
// Kept for backward compatibility — returns the complete output.
// Prefer nativeGenerateStreaming for real-time UI updates.
extern "C" jstring JNICALL
Java_com_habibi_financeslm_inference_LlamaEngineAndroid_nativeGenerate(
    JNIEnv * env, jobject /*thiz*/,
    jstring prompt,
    jint maxTokens,
    jfloat temperature,
    jfloat topP,
    jint topK,
    jfloat repeatPenalty)
{
    if (!g_model || !g_context) {
        LOGE("generate: model not loaded");
        return env->NewStringUTF("");
    }

    std::string promptStr = jstring_to_string(env, prompt);
    LOGI("generate: prompt_len=%zu, max_tokens=%d, temp=%.2f",
         promptStr.size(), maxTokens, temperature);

    // Tokenize the prompt
    const struct llama_vocab * vocab = llama_model_get_vocab(g_model);
    if (!vocab) {
        LOGE("generate: no vocab");
        return env->NewStringUTF("");
    }

    int32_t n_tokens = promptStr.size() + 4; // estimate
    std::vector<llama_token> tokens(n_tokens);

    int32_t actual = llama_tokenize(
        vocab,
        promptStr.c_str(),
        static_cast<int32_t>(promptStr.size()),
        tokens.data(),
        static_cast<int32_t>(tokens.size()),
        true,   // add_bos
        false   // parse_special
    );

    if (actual < 0) {
        // Need larger buffer
        tokens.resize(-actual);
        actual = llama_tokenize(
            vocab,
            promptStr.c_str(),
            static_cast<int32_t>(promptStr.size()),
            tokens.data(),
            static_cast<int32_t>(tokens.size()),
            true, false);
    }
    tokens.resize(actual);

    LOGI("generate: tokenized to %zu tokens", tokens.size());

    // Eval the prompt
    llama_batch batch = llama_batch_get_one(tokens.data(), static_cast<int32_t>(tokens.size()));
    if (llama_decode(g_context, batch) != 0) {
        LOGE("generate: prompt eval failed");
        return env->NewStringUTF("");
    }

    // Prepare sampler chain
    auto sparams = llama_sampler_chain_default_params();
    llama_sampler * smpl = llama_sampler_chain_init(sparams);

    if (topK > 0) {
        llama_sampler_chain_add(smpl, llama_sampler_init_top_k(static_cast<int32_t>(topK)));
    }
    llama_sampler_chain_add(smpl, llama_sampler_init_top_p(topP, 1));
    llama_sampler_chain_add(smpl, llama_sampler_init_temp(temperature));
    llama_sampler_chain_add(smpl, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));

    // Generate tokens
    std::string output;
    output.reserve(maxTokens * 4); // rough estimate

    for (int32_t i = 0; i < maxTokens; i++) {
        // Sample from logits
        const llama_token id = llama_sampler_sample(smpl, g_context, -1);

        if (id == llama_vocab_eos(vocab)) {
            LOGI("generate: EOS at token %d", i);
            break;
        }

        // Convert token to piece
        char buf[128];
        int32_t len = llama_token_to_piece(vocab, id, buf, sizeof(buf), 0, false);
        if (len > 0) {
            output.append(buf, static_cast<size_t>(len));
        }

        // Prepare next batch with single token
        llama_token next_tokens[] = { id };
        batch = llama_batch_get_one(next_tokens, 1);
        if (llama_decode(g_context, batch) != 0) {
            LOGE("generate: decode failed at token %d", i);
            break;
        }
    }

    llama_sampler_free(smpl);
    LOGI("generate: output %zu chars", output.size());

    return env->NewStringUTF(output.c_str());
}

// ── JNI: tokenize ─────────────────────────────────────────────────────────
// Java: external fun nativeTokenize(text: String): IntArray
extern "C" jintArray JNICALL
Java_com_habibi_financeslm_inference_LlamaEngineAndroid_nativeTokenize(
    JNIEnv * env, jobject /*thiz*/,
    jstring text)
{
    if (!g_model) {
        LOGE("tokenize: model not loaded");
        return nullptr;
    }

    const struct llama_vocab * vocab = llama_model_get_vocab(g_model);
    if (!vocab) return nullptr;

    std::string textStr = jstring_to_string(env, text);

    // Estimate buffer size
    int32_t n_tokens = static_cast<int32_t>(textStr.size()) + 4;
    std::vector<llama_token> tokens(n_tokens);

    int32_t actual = llama_tokenize(
        vocab,
        textStr.c_str(),
        static_cast<int32_t>(textStr.size()),
        tokens.data(),
        n_tokens,
        false,  // add_special
        false   // parse_special
    );

    if (actual < 0) {
        tokens.resize(-actual);
        actual = llama_tokenize(
            vocab,
            textStr.c_str(),
            static_cast<int32_t>(textStr.size()),
            tokens.data(),
            static_cast<int32_t>(tokens.size()),
            false, false);
    }
    tokens.resize(actual);

    jintArray result = env->NewIntArray(static_cast<jsize>(tokens.size()));
    if (result) {
        env->SetIntArrayRegion(result, 0, static_cast<jsize>(tokens.size()),
                               reinterpret_cast<jint *>(tokens.data()));
    }
    return result;
}

// ── JNI: applyLora ────────────────────────────────────────────────────────
// Java: external fun nativeApplyLora(loraPath: String): Boolean
extern "C" jboolean JNICALL
Java_com_habibi_financeslm_inference_LlamaEngineAndroid_nativeApplyLora(
    JNIEnv * env, jobject /*thiz*/,
    jstring loraPath)
{
    if (!g_model || !g_context) {
        LOGE("applyLora: model not loaded");
        return JNI_FALSE;
    }

    std::string path = jstring_to_string(env, loraPath);
    LOGI("applyLora: %s", path.c_str());

    // Load the LoRA adapter
    struct llama_adapter_lora * adapter = llama_adapter_lora_init(g_model, path.c_str());
    if (!adapter) {
        LOGE("applyLora: failed to load adapter from %s", path.c_str());
        return JNI_FALSE;
    }

    // Apply to context
    struct llama_adapter_lora * adapters[] = { adapter };
    float scales[] = { 1.0f };
    int32_t ret = llama_set_adapters_lora(g_context, adapters, 1, scales);

    if (ret != 0) {
        LOGE("applyLora: llama_set_adapters_lora returned %d", ret);
        llama_adapter_lora_free(adapter);
        return JNI_FALSE;
    }

    LOGI("applyLora: success");
    // NOTE: adapter remains valid as long as the model is alive.
    // We don't free it here since llama_set_adapters_lora takes ownership.
    return JNI_TRUE;
}

// ── JNI: isLoaded ─────────────────────────────────────────────────────────
// Java: external fun nativeIsLoaded(): Boolean
extern "C" jboolean JNICALL
Java_com_habibi_financeslm_inference_LlamaEngineAndroid_nativeIsLoaded(
    JNIEnv * /*env*/, jobject /*thiz*/)
{
    return (g_model != nullptr && g_context != nullptr) ? JNI_TRUE : JNI_FALSE;
}

// ── JNI_OnLoad — Register all native methods ─────────────────────────────
// Following Pat's directive (G1): use JNI_OnLoad + RegisterNatives instead of
// name mangling, so renaming the Kotlin class/package won't break JNI.
static const JNINativeMethod gMethods[] = {
    {
        "nativeLoadModel",
        "(Ljava/lang/String;IIII)Z",
        reinterpret_cast<void *>(Java_com_habibi_financeslm_inference_LlamaEngineAndroid_nativeLoadModel)
    },
    {
        "nativeFreeModel",
        "()V",
        reinterpret_cast<void *>(Java_com_habibi_financeslm_inference_LlamaEngineAndroid_nativeFreeModel)
    },
    {
        "nativeGenerate",
        "(Ljava/lang/String;IFFFF)Ljava/lang/String;",
        reinterpret_cast<void *>(Java_com_habibi_financeslm_inference_LlamaEngineAndroid_nativeGenerate)
    },
    {
        "nativeGenerateStreaming",
        "(Ljava/lang/String;IFFFFLjava/util/function/Consumer;)V",
        reinterpret_cast<void *>(Java_com_habibi_financeslm_inference_LlamaEngineAndroid_nativeGenerateStreaming)
    },
    {
        "nativeTokenize",
        "(Ljava/lang/String;)[I",
        reinterpret_cast<void *>(Java_com_habibi_financeslm_inference_LlamaEngineAndroid_nativeTokenize)
    },
    {
        "nativeApplyLora",
        "(Ljava/lang/String;)Z",
        reinterpret_cast<void *>(Java_com_habibi_financeslm_inference_LlamaEngineAndroid_nativeApplyLora)
    },
    {
        "nativeIsLoaded",
        "()Z",
        reinterpret_cast<void *>(Java_com_habibi_financeslm_inference_LlamaEngineAndroid_nativeIsLoaded)
    },
};

extern "C" jint JNI_OnLoad(JavaVM * vm, void * /*reserved*/) {
    JNIEnv * env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        LOGE("JNI_OnLoad: GetEnv failed");
        return JNI_ERR;
    }

    // Install crash handlers for SIGSEGV and SIGABRT
    installCrashHandler(env);

    jclass clazz = env->FindClass(
        "com/habibi/financeslm/inference/LlamaEngineAndroid");
    if (!clazz) {
        LOGE("JNI_OnLoad: FindClass failed for LlamaEngineAndroid");
        return JNI_ERR;
    }

    jint ret = env->RegisterNatives(
        clazz, gMethods, sizeof(gMethods) / sizeof(gMethods[0]));
    if (ret != JNI_OK) {
        LOGE("JNI_OnLoad: RegisterNatives failed (ret=%d)", ret);
        return JNI_ERR;
    }

    LOGI("JNI_OnLoad: registered %zu native methods",
         sizeof(gMethods) / sizeof(gMethods[0]));

    // Also call llama_backend_init here to ensure it's initialized before first use
    // (it's idempotent, so repeated calls from loadModel are safe)
    llama_backend_init();

    return JNI_VERSION_1_6;
}

extern "C" void JNI_OnUnload(JavaVM * /*vm*/, void * /*reserved*/) {
    // Clean up backend resources and crash handlers
    llama_backend_free();
    uninstallCrashHandler();
    LOGI("JNI_OnUnload: backend freed, crash handlers removed");
}