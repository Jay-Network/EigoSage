package com.jworks.eigosage.ui.gallery

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jworks.eigosage.domain.analysis.ReadabilityCalculator
import com.jworks.eigosage.domain.export.AnalysisResult
import com.jworks.eigosage.domain.export.ExportState
import com.jworks.eigosage.domain.export.ImportState
import com.jworks.eigosage.domain.export.PdfExporter
import com.jworks.eigosage.domain.usecases.ProcessCameraFrameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GalleryImportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val processFrame: ProcessCameraFrameUseCase,
    private val readabilityCalculator: ReadabilityCalculator,
    private val pdfExporter: PdfExporter
) : ViewModel() {

    companion object {
        private const val TAG = "GalleryVM"
    }

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    private val _analysisResults = MutableStateFlow<List<AnalysisResult>>(emptyList())
    val analysisResults: StateFlow<List<AnalysisResult>> = _analysisResults.asStateFlow()

    fun importImage(uri: Uri) {
        viewModelScope.launch {
            _importState.value = ImportState.Processing(uri)

            try {
                val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                } ?: throw IllegalStateException("Cannot read image from URI")

                val ocrResult = processFrame.processStaticImage(bitmap)
                val fullText = ocrResult.texts.joinToString(" ") { it.text }

                if (fullText.isBlank()) {
                    _importState.value = ImportState.Error("No text detected in image")
                    return@launch
                }

                val words = fullText.split(Regex("\\s+")).filter { it.any { c -> c.isLetter() } }
                val readability = readabilityCalculator.calculate(fullText)

                val result = AnalysisResult(
                    uri = uri,
                    fullText = fullText,
                    wordCount = words.size,
                    readability = readability
                )

                _analysisResults.value = _analysisResults.value + result
                _importState.value = ImportState.Success(result)
                Log.d(TAG, "Imported: ${words.size} words, grade=${readability?.averageGrade ?: "N/A"}")
            } catch (e: Exception) {
                Log.e(TAG, "Import failed", e)
                _importState.value = ImportState.Error(e.message ?: "Import failed")
            }
        }
    }

    fun exportToPdf(result: AnalysisResult) {
        viewModelScope.launch {
            _exportState.value = ExportState.Exporting

            pdfExporter.export(result)
                .onSuccess { file ->
                    val fileUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    _exportState.value = ExportState.Success(fileUri)
                    Log.d(TAG, "PDF exported: ${file.absolutePath}")
                }
                .onFailure { e ->
                    Log.e(TAG, "PDF export failed", e)
                    _exportState.value = ExportState.Error(e.message ?: "Export failed")
                }
        }
    }

    fun sharePdf(fileUri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share PDF").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun clearExportState() {
        _exportState.value = ExportState.Idle
    }

    fun clearImportState() {
        _importState.value = ImportState.Idle
    }

    fun removeResult(result: AnalysisResult) {
        _analysisResults.value = _analysisResults.value - result
    }
}
