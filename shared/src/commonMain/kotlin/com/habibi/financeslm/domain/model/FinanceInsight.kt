package com.habibi.financeslm.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class FinanceInsight(
    val id: String,
    val title: String,
    val summary: String,
    val detailText: String,
    val category: InsightCategory,
    val sourceApp: String? = null,
    val timestamp: Long,
    val loraAdapterId: String? = null
)

enum class InsightCategory {
    SPENDING,
    SAVINGS,
    INVESTMENT,
    BUDGET,
    GENERAL
}