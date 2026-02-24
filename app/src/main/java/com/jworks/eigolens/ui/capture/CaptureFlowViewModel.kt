package com.jworks.eigolens.ui.capture

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jworks.eigolens.data.ai.AiProviderManager
import com.jworks.eigolens.data.ai.ContextualInsight
import com.jworks.eigolens.data.ai.GeminiOcrCorrector
import com.jworks.eigolens.data.ai.OcrTextMerger
import com.jworks.eigolens.data.repository.DefinitionRepository
import com.jworks.eigolens.domain.ai.AiResponse
import com.jworks.eigolens.domain.ai.AnalysisContext
import com.jworks.eigolens.domain.analysis.ReadabilityCalculator
import com.jworks.eigolens.domain.models.Definition
import com.jworks.eigolens.domain.models.ReadabilityMetrics
import com.jworks.eigolens.domain.usecases.ProcessCameraFrameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CaptureState {
    data object Camera : CaptureState()
    data class Annotate(val capturedImage: CapturedImage) : CaptureState()
}

data class CapturedImage(
    val bitmap: Bitmap,
    val ocrResult: com.jworks.eigolens.domain.models.OCRResult,
    val timestamp: Long
)

enum class InteractionMode { TAP, CIRCLE }

sealed class PanelState {
    data object Idle : PanelState()
    data object Loading : PanelState()
    data class WordDefinition(
        val definition: Definition,
        val contextualInsight: ContextualInsight? = null
    ) : PanelState()
    data class ReadabilityResult(val metrics: ReadabilityMetrics) : PanelState()
    data class AiLoading(val scopeLevel: ScopeLevel, val selectedText: String) : PanelState()
    data class AiAnalysis(val scopeLevel: ScopeLevel, val selectedText: String, val response: AiResponse) : PanelState()
    data class NotFound(val word: String) : PanelState()
    data class Error(val message: String) : PanelState()
}

@HiltViewModel
class CaptureFlowViewModel @Inject constructor(
    private val processCameraFrame: ProcessCameraFrameUseCase,
    private val definitionRepository: DefinitionRepository,
    private val readabilityCalculator: ReadabilityCalculator,
    private val aiProviderManager: AiProviderManager,
    private val geminiOcrCorrector: GeminiOcrCorrector
) : ViewModel() {

    companion object {
        private const val TAG = "CaptureFlowVM"
        private const val MAX_BITMAP_DIMENSION = 2048
    }

    private val _captureState = MutableStateFlow<CaptureState>(CaptureState.Camera)
    val captureState: StateFlow<CaptureState> = _captureState.asStateFlow()

    private val _panelState = MutableStateFlow<PanelState>(PanelState.Idle)
    val panelState: StateFlow<PanelState> = _panelState.asStateFlow()

    private val _interactionMode = MutableStateFlow(InteractionMode.TAP)
    val interactionMode: StateFlow<InteractionMode> = _interactionMode.asStateFlow()

    private val _tappedWord = MutableStateFlow<TapResult?>(null)
    val tappedWord: StateFlow<TapResult?> = _tappedWord.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _isCorrectingOcr = MutableStateFlow(false)
    val isCorrectingOcr: StateFlow<Boolean> = _isCorrectingOcr.asStateFlow()

    private val _isFlashOn = MutableStateFlow(false)
    val isFlashOn: StateFlow<Boolean> = _isFlashOn.asStateFlow()

    init {
        viewModelScope.launch {
            definitionRepository.preloadCommonWords(100)
        }
    }

    fun toggleFlash() {
        _isFlashOn.value = !_isFlashOn.value
    }

    fun setInteractionMode(mode: InteractionMode) {
        _interactionMode.value = mode
    }

    fun onPhotoCapture(bitmap: Bitmap) {
        _isProcessing.value = true
        viewModelScope.launch {
            try {
                val scaled = downscaleBitmap(bitmap)
                val ocrResult = processCameraFrame.processStaticImage(scaled)
                Log.d(TAG, "Captured: ${ocrResult.texts.size} lines, " +
                        "${ocrResult.texts.sumOf { it.elements.size }} words, " +
                        "${ocrResult.processingTimeMs}ms")

                // Show ML Kit results immediately
                val capturedImage = CapturedImage(scaled, ocrResult, System.currentTimeMillis())
                _captureState.value = CaptureState.Annotate(capturedImage)
                _isProcessing.value = false

                // Run Gemini Vision correction in background
                if (geminiOcrCorrector.isAvailable && ocrResult.texts.isNotEmpty()) {
                    correctOcrWithGemini(scaled, ocrResult, capturedImage.timestamp)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Capture failed", e)
                _isProcessing.value = false
            }
        }
    }

    private fun correctOcrWithGemini(
        bitmap: Bitmap,
        mlKitResult: com.jworks.eigolens.domain.models.OCRResult,
        timestamp: Long
    ) {
        _isCorrectingOcr.value = true
        viewModelScope.launch {
            try {
                geminiOcrCorrector.extractText(bitmap)
                    .onSuccess { geminiLines ->
                        val corrected = OcrTextMerger.merge(mlKitResult, geminiLines)
                        val wordsBefore = mlKitResult.texts.sumOf { it.elements.size }
                        val wordsAfter = corrected.texts.sumOf { it.elements.size }
                        Log.d(TAG, "OCR corrected: $wordsBefore -> $wordsAfter words")

                        // Update the capture state with corrected OCR
                        _captureState.value = CaptureState.Annotate(
                            CapturedImage(bitmap, corrected, timestamp)
                        )
                    }
                    .onFailure { e ->
                        Log.w(TAG, "Gemini OCR correction failed, keeping ML Kit result", e)
                    }
            } finally {
                _isCorrectingOcr.value = false
            }
        }
    }

    fun onGalleryImport(bitmap: Bitmap) {
        onPhotoCapture(bitmap)
    }

    /** Called when user taps a word in TAP mode */
    fun onWordTapped(tapResult: TapResult) {
        _tappedWord.value = tapResult
        // Strip punctuation from word before lookup
        val cleanWord = tapResult.word.replace(Regex("[^a-zA-Z'-]"), "").trim()
        if (cleanWord.isEmpty()) return

        // Local WordNet lookup (fast, immediate)
        lookupWord(cleanWord)

        // Parallel: get AI contextual insight if available
        if (geminiOcrCorrector.isAvailable) {
            fetchContextualInsight(cleanWord)
        }
    }

    private fun fetchContextualInsight(word: String) {
        val state = _captureState.value
        if (state !is CaptureState.Annotate) return

        val fullText = state.capturedImage.ocrResult.texts.joinToString(" ") { it.text }
        if (fullText.isBlank()) return

        viewModelScope.launch {
            geminiOcrCorrector.getContextualInsight(word, fullText)
                .onSuccess { insight ->
                    // Only update if we're still showing this word's definition
                    val current = _panelState.value
                    if (current is PanelState.WordDefinition &&
                        current.definition.word.equals(word, ignoreCase = true)) {
                        _panelState.value = current.copy(contextualInsight = insight)
                    }
                }
                .onFailure { e ->
                    Log.d(TAG, "Contextual insight skipped: ${e.message}")
                }
        }
    }

    /** Called when user circles words in CIRCLE mode (lasso selection) */
    fun selectWords(words: List<String>) {
        if (words.isEmpty()) return
        _tappedWord.value = null

        if (words.size == 1) {
            // Single word → local WordNet lookup
            lookupWord(words.first())
        } else {
            // Multi-word selection → determine scope and route to AI
            val selectedText = words.joinToString(" ")
            val scopeLevel = if (words.size <= 8) {
                ScopeLevel.Phrase(selectedText, words)
            } else {
                ScopeLevel.Paragraph(selectedText)
            }

            if (aiProviderManager.isAiAvailable) {
                analyzeWithAi(selectedText, scopeLevel)
            } else {
                // Fallback: try local lookup on first word when no AI available
                lookupWord(selectedText, fallbackWord = words.first())
            }
        }
    }

    /** Called when user long-presses a word — analyze its containing sentence */
    fun analyzeSentenceForWord(tapResult: TapResult) {
        val state = _captureState.value
        if (state !is CaptureState.Annotate) return

        _tappedWord.value = tapResult

        val fullText = state.capturedImage.ocrResult.texts.joinToString(" ") { it.text }
        val word = tapResult.word.replace(Regex("[^a-zA-Z'-]"), "").trim()
        if (word.isEmpty() || fullText.isBlank()) return

        // Find the sentence containing this word
        val sentence = extractSentence(fullText, word)

        if (aiProviderManager.isAiAvailable && sentence.split(" ").size > 1) {
            analyzeWithAi(sentence, ScopeLevel.Sentence(sentence))
        } else {
            // Fallback to word definition if no AI
            lookupWord(word)
        }
    }

    /** Extract the sentence containing a word from full text */
    private fun extractSentence(fullText: String, word: String): String {
        // Split on sentence boundaries
        val sentences = fullText.split(Regex("(?<=[.!?])\\s+"))
        // Find sentence containing the word (case-insensitive)
        val match = sentences.find { it.contains(word, ignoreCase = true) }
        return match?.trim() ?: fullText.take(200)
    }

    /** Analyze full snapshot text with AI */
    fun analyzeFullText() {
        val state = _captureState.value
        if (state !is CaptureState.Annotate) return
        val fullText = state.capturedImage.ocrResult.texts.joinToString(" ") { it.text }
        if (fullText.isBlank()) return

        analyzeWithAi(fullText, ScopeLevel.FullSnapshot)
    }

    private fun analyzeWithAi(selectedText: String, scopeLevel: ScopeLevel) {
        val state = _captureState.value
        if (state !is CaptureState.Annotate) return

        val fullText = state.capturedImage.ocrResult.texts.joinToString(" ") { it.text }

        _panelState.value = PanelState.AiLoading(scopeLevel, selectedText)

        viewModelScope.launch {
            val context = AnalysisContext(
                selectedText = selectedText,
                fullSnapshotText = fullText,
                scopeLevel = scopeLevel
            )

            aiProviderManager.analyze(context)
                .onSuccess { response ->
                    _panelState.value = PanelState.AiAnalysis(scopeLevel, selectedText, response)
                }
                .onFailure { error ->
                    Log.e(TAG, "AI analysis failed", error)
                    _panelState.value = PanelState.Error(
                        error.message ?: "AI analysis failed"
                    )
                }
        }
    }

    fun analyzeReadability() {
        val state = _captureState.value
        if (state !is CaptureState.Annotate) return
        val text = state.capturedImage.ocrResult.texts.joinToString(" ") { it.text }
        if (text.isBlank()) return

        val metrics = readabilityCalculator.calculate(text) ?: return
        _panelState.value = PanelState.ReadabilityResult(metrics)
    }

    fun switchToWordLookup() {
        _panelState.value = PanelState.Idle
    }

    fun resetToCamera() {
        _captureState.value = CaptureState.Camera
        _panelState.value = PanelState.Idle
        _interactionMode.value = InteractionMode.TAP
        _tappedWord.value = null
    }

    private fun lookupWord(query: String, fallbackWord: String? = null) {
        viewModelScope.launch {
            _panelState.value = PanelState.Loading

            definitionRepository.getDefinition(query)
                .onSuccess { _panelState.value = PanelState.WordDefinition(it) }
                .onFailure {
                    if (fallbackWord != null) {
                        definitionRepository.getDefinition(fallbackWord)
                            .onSuccess { _panelState.value = PanelState.WordDefinition(it) }
                            .onFailure { err -> setFailureState(fallbackWord, err) }
                    } else {
                        setFailureState(query, it)
                    }
                }
        }
    }

    private fun setFailureState(word: String, error: Throwable) {
        _panelState.value = if (error is NoSuchElementException) {
            PanelState.NotFound(word)
        } else {
            PanelState.Error(error.message ?: "An error occurred")
        }
    }

    private fun downscaleBitmap(bitmap: Bitmap): Bitmap {
        val maxDim = maxOf(bitmap.width, bitmap.height)
        if (maxDim <= MAX_BITMAP_DIMENSION) return bitmap
        val scale = MAX_BITMAP_DIMENSION.toFloat() / maxDim
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt(),
            (bitmap.height * scale).toInt(),
            true
        )
    }
}
