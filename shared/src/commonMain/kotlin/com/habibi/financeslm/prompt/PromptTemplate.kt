package com.habibi.financeslm.prompt

/**
 * Supported chat template formats for different model families.
 * Each format renders system/user/assistant messages using the correct
 * chat tokens for that model architecture.
 */
enum class ChatTemplateFormat(val key: String) {
    QWEN("qwen"),
    LLAMA("llama"),
    MISTRAL("mistral");

    companion object {
        fun fromKey(key: String): ChatTemplateFormat {
            return entries.find { it.key == key.lowercase() } ?: QWEN
        }
    }
}

class PromptTemplate {

    fun renderUserPrompt(screenContent: String): String {
        return """Analyze the following financial screen data and provide personalized financial tips:

$screenContent

Based on this data, what financial insights can you provide?"""
    }

    /**
     * Render a chat template using the specified format.
     *
     * Supported formats:
     *  - qwen:    <|im_start|>system ... <|im_end|>  <|im_start|>user ... <|im_end|>  <|im_start|>assistant
     *  - llama:   <|begin_of_text|><|start_header_id|>system<|end_header_id|> ... <|eot_id|>
     *             <|start_header_id|>user<|end_header_id|> ... <|eot_id|>
     *             <|start_header_id|>assistant<|end_header_id|>
     *  - mistral: [INST] ... [/INST]
     */
    fun renderChatTemplate(systemPrompt: String, userPrompt: String, format: ChatTemplateFormat = ChatTemplateFormat.QWEN): String {
        return when (format) {
            ChatTemplateFormat.QWEN -> renderQwen(systemPrompt, userPrompt)
            ChatTemplateFormat.LLAMA -> renderLlama(systemPrompt, userPrompt)
            ChatTemplateFormat.MISTRAL -> renderMistral(systemPrompt, userPrompt)
        }
    }

    fun renderChatTemplate(systemPrompt: String, userPrompt: String, formatKey: String): String {
        return renderChatTemplate(systemPrompt, userPrompt, ChatTemplateFormat.fromKey(formatKey))
    }

    private fun renderQwen(systemPrompt: String, userPrompt: String): String {
        return """<|im_start|>system
$systemPrompt<|im_end|>
<|im_start|>user
$userPrompt<|im_end|>
<|im_start|>assistant
"""
    }

    private fun renderLlama(systemPrompt: String, userPrompt: String): String {
        return """<|begin_of_text|><|start_header_id|>system<|end_header_id|>

$systemPrompt<|eot_id|><|start_header_id|>user<|end_header_id|>

$userPrompt<|eot_id|><|start_header_id|>assistant<|end_header_id|>

"""
    }

    private fun renderMistral(systemPrompt: String, userPrompt: String): String {
        return """<s>[INST] <<SYS>>
$systemPrompt
<</SYS>>

$userPrompt [/INST]"""
    }
}