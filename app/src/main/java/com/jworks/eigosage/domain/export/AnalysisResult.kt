package com.jworks.eigosage.domain.export

import android.net.Uri
import com.jworks.eigosage.domain.models.ReadabilityMetrics

data class AnalysisResult(
    val uri: Uri,
    val fullText: String,
    val wordCount: Int,
    val readability: ReadabilityMetrics?,
    val timestamp: Long = System.currentTimeMillis()
)

sealed class ImportState {
    data object Idle : ImportState()
    data class Processing(val uri: Uri) : ImportState()
    data class Success(val result: AnalysisResult) : ImportState()
    data class Error(val message: String) : ImportState()
}

sealed class ExportState {
    data object Idle : ExportState()
    data object Exporting : ExportState()
    data class Success(val fileUri: Uri) : ExportState()
    data class Error(val message: String) : ExportState()
}
