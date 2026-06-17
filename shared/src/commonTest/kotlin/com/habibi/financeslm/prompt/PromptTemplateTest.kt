package com.habibi.financeslm.prompt

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PromptTemplateTest {

    // -- ChatTemplateFormat tests -------------------------------------------------------

    @Test
    fun `qwen format key resolves to QWEN`() {
        assertEquals(ChatTemplateFormat.QWEN, ChatTemplateFormat.fromKey("qwen"))
    }

    @Test
    fun `llama format key resolves to LLAMA`() {
        assertEquals(ChatTemplateFormat.LLAMA, ChatTemplateFormat.fromKey("llama"))
    }

    @Test
    fun `mistral format key resolves to MISTRAL`() {
        assertEquals(ChatTemplateFormat.MISTRAL, ChatTemplateFormat.fromKey("mistral"))
    }

    @Test
    fun `fromKey is case-insensitive`() {
        assertEquals(ChatTemplateFormat.QWEN, ChatTemplateFormat.fromKey("QWEN"))
        assertEquals(ChatTemplateFormat.LLAMA, ChatTemplateFormat.fromKey("LLAMA"))
        assertEquals(ChatTemplateFormat.MISTRAL, ChatTemplateFormat.fromKey("MISTRAL"))
    }

    @Test
    fun `unknown format key defaults to QWEN`() {
        assertEquals(ChatTemplateFormat.QWEN, ChatTemplateFormat.fromKey("unknown"))
        assertEquals(ChatTemplateFormat.QWEN, ChatTemplateFormat.fromKey("gpt"))
        assertEquals(ChatTemplateFormat.QWEN, ChatTemplateFormat.fromKey(""))
    }

    // -- PromptTemplate.renderUserPrompt -------------------------------------------------

    @Test
    fun `renderUserPrompt includes screen content`() {
        val template = PromptTemplate()
        val result = template.renderUserPrompt("Balance: $1,000")
        assertContains(result, "Balance: $1,000")
        assertContains(result, "Analyze the following financial screen data")
        assertContains(result, "financial insights")
    }

    @Test
    fun `renderUserPrompt with empty screen content`() {
        val template = PromptTemplate()
        val result = template.renderUserPrompt("")
        assertContains(result, "Analyze the following financial screen data")
    }

    // -- QWEN template -------------------------------------------------------------------

    @Test
    fun `qwen template contains correct tokens`() {
        val template = PromptTemplate()
        val result = template.renderChatTemplate("System prompt", "User message", ChatTemplateFormat.QWEN)

        assertContains(result, "<|im_start|>system")
        assertContains(result, "<|im_end|>")
        assertContains(result, "<|im_start|>user")
        assertContains(result, "<|im_start|>assistant")
        assertTrue(result.startsWith("<|im_start|>system"))
        assertContains(result, "System prompt")
        assertContains(result, "User message")
    }

    // -- LLAMA template ------------------------------------------------------------------

    @Test
    fun `llama template contains correct tokens`() {
        val template = PromptTemplate()
        val result = template.renderChatTemplate("System prompt", "User message", ChatTemplateFormat.LLAMA)

        assertContains(result, "<|begin_of_text|>")
        assertContains(result, "<|start_header_id|>system<|end_header_id|>")
        assertContains(result, "<|start_header_id|>user<|end_header_id|>")
        assertContains(result, "<|start_header_id|>assistant<|end_header_id|>")
        assertContains(result, "<|eot_id|>")
        assertTrue(result.startsWith("<|begin_of_text|>"))
        assertContains(result, "System prompt")
        assertContains(result, "User message")
    }

    // -- MISTRAL template ----------------------------------------------------------------

    @Test
    fun `mistral template contains correct tokens`() {
        val template = PromptTemplate()
        val result = template.renderChatTemplate("System prompt", "User message", ChatTemplateFormat.MISTRAL)

        assertContains(result, "[INST]")
        assertContains(result, "[/INST]")
        assertContains(result, "<<SYS>>")
        assertContains(result, "<</SYS>>")
        assertTrue(result.startsWith("<s>[INST]"))
        assertContains(result, "System prompt")
        assertContains(result, "User message")
    }

    // -- renderChatTemplate with string key ----------------------------------------------

    @Test
    fun `renderChatTemplate with string key qwen`() {
        val template = PromptTemplate()
        val result = template.renderChatTemplate("S", "U", "qwen")
        assertContains(result, "<|im_start|>system")
    }

    @Test
    fun `renderChatTemplate with string key llama`() {
        val template = PromptTemplate()
        val result = template.renderChatTemplate("S", "U", "llama")
        assertContains(result, "<|begin_of_text|>")
    }

    @Test
    fun `renderChatTemplate with string key mistral`() {
        val template = PromptTemplate()
        val result = template.renderChatTemplate("S", "U", "mistral")
        assertContains(result, "[INST]")
    }

    @Test
    fun `renderChatTemplate with unknown string key defaults to qwen`() {
        val template = PromptTemplate()
        val result = template.renderChatTemplate("S", "U", "nonexistent")
        assertContains(result, "<|im_start|>system")
    }
}