package com.habibi.financeslm.prompt

import com.habibi.financeslm.domain.model.DataPoint
import com.habibi.financeslm.domain.model.DataPointType
import com.habibi.financeslm.domain.model.ScreenData
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PromptBuilderTest {

    private val sampleScreenData = ScreenData(
        id = "test-1",
        sourcePackage = "com.dbs.dbsapp",
        sourceApp = "DBS digibank",
        textContent = "Account balance SGD 5,432.10",
        timestamp = 1_700_000_000_000L,
        dataPoints = listOf(
            DataPoint(DataPointType.BALANCE, "Savings Account", "5,432.10", "SGD"),
            DataPoint(DataPointType.DESCRIPTION, "Description", "Salary credited", null)
        )
    )

    @Test
    fun `build without lora produces basic prompt`() {
        val builder = PromptBuilder()
        val result = builder.build(sampleScreenData, loraInstruction = null)

        // Contains system prompt content (the default finance advisor)
        assertContains(result, "Singapore-based personal finance advisor")
        assertContains(result, "financial insights")

        // Does NOT contain lora instruction
        assertFalse(result.contains("Additional instruction:"))
    }

    @Test
    fun `build with lora includes instruction`() {
        val builder = PromptBuilder()
        val result = builder.build(sampleScreenData, loraInstruction = "Focus on emergency fund")

        // Contains system prompt and lora instruction
        assertContains(result, "Singapore-based personal finance advisor")
        assertContains(result, "Additional instruction: Focus on emergency fund")
    }

    @Test
    fun `build includes screen data source app and text content`() {
        val builder = PromptBuilder()
        val result = builder.build(sampleScreenData)

        assertContains(result, "Source App: DBS digibank")
        assertContains(result, "Text Content: Account balance SGD 5,432.10")
    }

    @Test
    fun `build includes screen data points`() {
        val builder = PromptBuilder()
        val result = builder.build(sampleScreenData)

        assertContains(result, "Extracted Data Points:")
        assertContains(result, "Savings Account: 5,432.10 (SGD)")
        assertContains(result, "Description: Salary credited")
    }

    @Test
    fun `build with chatTemplate qwen uses qwen format`() {
        val builder = PromptBuilder()
        val result = builder.build(sampleScreenData, chatTemplate = "qwen")

        assertContains(result, "<|im_start|>system")
        assertContains(result, "<|im_start|>assistant")
    }

    @Test
    fun `build with chatTemplate llama uses llama format`() {
        val builder = PromptBuilder()
        val result = builder.build(sampleScreenData, chatTemplate = "llama")

        assertContains(result, "<|begin_of_text|>")
        assertContains(result, "<|eot_id|>")
    }

    @Test
    fun `build with chatTemplate mistral uses mistral format`() {
        val builder = PromptBuilder()
        val result = builder.build(sampleScreenData, chatTemplate = "mistral")

        assertContains(result, "[INST]")
        assertContains(result, "[/INST]")
    }

    @Test
    fun `build with empty data points`() {
        val data = sampleScreenData.copy(dataPoints = emptyList())
        val builder = PromptBuilder()
        val result = builder.build(data)

        assertContains(result, "Source App: DBS digibank")
        assertFalse(result.contains("Extracted Data Points:"))
    }

    @Test
    fun `build default chatTemplate is qwen`() {
        val builder = PromptBuilder()
        val result = builder.build(sampleScreenData)

        // Default chat template is qwen
        assertContains(result, "<|im_start|>system")
    }
}