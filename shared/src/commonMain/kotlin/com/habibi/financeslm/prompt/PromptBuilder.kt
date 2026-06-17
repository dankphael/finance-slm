package com.habibi.financeslm.prompt

import com.habibi.financeslm.domain.model.ScreenData

class PromptBuilder {
    private val template = PromptTemplate()
    private val systemPrompts = SystemPrompts()

    fun build(screenData: ScreenData, loraInstruction: String? = null, chatTemplate: String = "qwen"): String {
        val systemPrompt = if (loraInstruction != null) {
            "${systemPrompts.defaultFinanceAdvisor()}\n\nAdditional instruction: $loraInstruction"
        } else {
            systemPrompts.defaultFinanceAdvisor()
        }

        val screenContent = formatScreenData(screenData)
        val userPrompt = template.renderUserPrompt(screenContent)

        return template.renderChatTemplate(systemPrompt, userPrompt, chatTemplate)
    }

    private fun formatScreenData(data: ScreenData): String {
        val sb = StringBuilder()
        sb.appendLine("Source App: ${data.sourceApp}")
        sb.appendLine("Text Content: ${data.textContent}")
        if (data.dataPoints.isNotEmpty()) {
            sb.appendLine("Extracted Data Points:")
            data.dataPoints.forEach { dp ->
                sb.appendLine("  - ${dp.label}: ${dp.value}${dp.currency?.let { " ($it)" } ?: ""}")
            }
        }
        return sb.toString()
    }
}