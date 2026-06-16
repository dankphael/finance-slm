package com.habibi.financeslm.inference

/**
 * Wrapper around llama.cpp token callback for streaming token-by-token output.
 */
class TokenStream {
    private val buffer = StringBuilder()

    fun append(token: String) {
        buffer.append(token)
    }

    fun reset() {
        buffer.clear()
    }

    fun currentText(): String = buffer.toString()

    fun isEmpty(): Boolean = buffer.isEmpty()
}