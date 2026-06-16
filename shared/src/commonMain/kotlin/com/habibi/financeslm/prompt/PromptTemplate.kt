package com.habibi.financeslm.prompt

class PromptTemplate {

    fun renderUserPrompt(screenContent: String): String {
        return """Analyze the following financial screen data and provide personalized financial tips:

$screenContent

Based on this data, what financial insights can you provide?"""
    }

    fun renderChatTemplate(systemPrompt: String, userPrompt: String): String {
        return """<|im_start|>system
$systemPrompt<|im_end|>
<|im_start|>user
$userPrompt<|im_end|>
<|im_start|>assistant
"""
    }
}