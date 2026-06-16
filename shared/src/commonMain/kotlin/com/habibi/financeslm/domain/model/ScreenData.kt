package com.habibi.financeslm.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ScreenData(
    val id: String,
    val sourcePackage: String,
    val sourceApp: String,
    val textContent: String,
    val timestamp: Long,
    val dataPoints: List<DataPoint> = emptyList()
)

@Serializable
data class DataPoint(
    val type: DataPointType,
    val label: String,
    val value: String,
    val currency: String? = null
)

enum class DataPointType {
    AMOUNT,
    BALANCE,
    DESCRIPTION,
    DATE,
    MERCHANT,
    CONFIRMATION
}