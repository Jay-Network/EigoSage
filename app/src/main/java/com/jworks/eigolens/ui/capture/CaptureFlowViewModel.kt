package com.jworks.eigolens.ui.capture

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import android.util.Size
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.jworks.eigolens.data.ai.AiProviderManager
import com.jworks.eigolens.data.ai.ContextualInsight
import com.jworks.eigolens.data.ai.GeminiOcrCorrector
import com.jworks.eigolens.data.ai.OcrTextMerger
import com.jworks.eigolens.data.jcoin.DeviceAuthRepository
import com.jworks.eigolens.data.jcoin.JCoinClient
import com.jworks.eigolens.data.jcoin.JCoinEarnRules
import com.jworks.eigolens.data.jcoin.JCoinSpendRules
import com.jworks.eigolens.data.repository.DefinitionRepository
import com.jworks.eigolens.data.repository.HistoryRepository
import com.jworks.eigolens.data.repository.WordEnrichmentRepository
import com.jworks.eigolens.domain.models.CefrLevel
import com.jworks.eigolens.domain.models.EnrichedWord
import com.jworks.eigolens.domain.models.OCRResult
import com.jworks.eigolens.domain.ai.AiResponse
import com.jworks.eigolens.domain.ai.AnalysisContext
import com.jworks.eigolens.domain.analysis.ReadabilityCalculator
import com.jworks.eigolens.domain.models.Definition
import com.jworks.eigolens.domain.models.ReadabilityMetrics
import com.jworks.eigolens.domain.repository.SettingsRepository
import com.jworks.eigolens.domain.usecases.ProcessCameraFrameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
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
    data class AiLoading(val scopeLevel: ScopeLevel, val selectedText: String, val readability: ReadabilityMetrics? = null) : PanelState()
    data class AiAnalysis(val scopeLevel: ScopeLevel, val selectedText: String, val response: AiResponse, val readability: ReadabilityMetrics? = null) : PanelState()
    data class DifficultWordsList(
        val words: List<EnrichedWord>,
        val threshold: CefrLevel
    ) : PanelState()
    data class NotFound(val word: String) : PanelState()
    data class Error(val message: String) : PanelState()
}

@HiltViewModel
class CaptureFlowViewModel @Inject constructor(
    private val application: Application,
    private val processCameraFrame: ProcessCameraFrameUseCase,
    private val definitionRepository: DefinitionRepository,
    private val readabilityCalculator: ReadabilityCalculator,
    private val aiProviderManager: AiProviderManager,
    private val geminiOcrCorrector: GeminiOcrCorrector,
    private val historyRepository: HistoryRepository,
    private val wordEnrichmentRepository: WordEnrichmentRepository,
    private val jCoinClient: JCoinClient,
    private val jCoinEarnRules: JCoinEarnRules,
    private val jCoinSpendRules: JCoinSpendRules,
    private val deviceAuthRepository: DeviceAuthRepository,
    private val settingsRepository: SettingsRepository
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

    private val _enrichedWords = MutableStateFlow<List<EnrichedWord>>(emptyList())
    val enrichedWords: StateFlow<List<EnrichedWord>> = _enrichedWords.asStateFlow()

    private val _cefrThreshold = MutableStateFlow(CefrLevel.B2)
    val cefrThreshold: StateFlow<CefrLevel> = _cefrThreshold.asStateFlow()

    private val _isFlashOn = MutableStateFlow(false)
    val isFlashOn: StateFlow<Boolean> = _isFlashOn.asStateFlow()

    private val _coinBalance = MutableStateFlow<Int?>(null)
    val coinBalance: StateFlow<Int?> = _coinBalance.asStateFlow()

    // Live camera overlay state
    private val _liveEnrichedWords = MutableStateFlow<List<EnrichedWord>>(emptyList())
    val liveEnrichedWords: StateFlow<List<EnrichedWord>> = _liveEnrichedWords.asStateFlow()

    private val _liveImageSize = MutableStateFlow(Size(0, 0))
    val liveImageSize: StateFlow<Size> = _liveImageSize.asStateFlow()

    private val _liveRotationDegrees = MutableStateFlow(0)
    val liveRotationDegrees: StateFlow<Int> = _liveRotationDegrees.asStateFlow()

    private val _showIpaOverlay = MutableStateFlow(true)
    val showIpaOverlay: StateFlow<Boolean> = _showIpaOverlay.asStateFlow()

    private val _ipaFontScale = MutableStateFlow(0.6f)
    val ipaFontScale: StateFlow<Float> = _ipaFontScale.asStateFlow()

    private var _isLiveProcessing = false
    private var _liveFrameCount = 0
    private var _prevLiveEnriched: List<EnrichedWord> = emptyList()
    private var _emptyFrameCount = 0

    private var hasEarnedFirstCefrScan = false
    private var hasSetNonDefaultThreshold = false
    private val reviewedWordsThisScan = mutableSetOf<String>()
    private var allDifficultClearedThisScan = false

    private val _currentWord = MutableStateFlow<String?>(null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val isCurrentWordBookmarked: StateFlow<Boolean> = _currentWord
        .flatMapLatest { word ->
            if (word != null) historyRepository.isBookmarked(word) else flowOf(false)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun toggleBookmark() {
        val state = _panelState.value
        if (state !is PanelState.WordDefinition) return

        val word = state.definition.word
        val definition = state.definition.meanings.firstOrNull()?.definition ?: ""
        val contextSnippet = getContextSnippet()

        viewModelScope.launch {
            if (isCurrentWordBookmarked.value) {
                historyRepository.removeBookmark(word)
            } else {
                historyRepository.addBookmark(word, definition, contextSnippet)
                // J Coin: save_definition earn
                val earn = jCoinEarnRules.checkSaveDefinition(application.applicationContext)
                if (earn != null) earnCoins(earn.sourceType, earn.coins)
            }
        }
    }

    private fun getContextSnippet(): String? {
        val state = _captureState.value
        if (state !is CaptureState.Annotate) return null
        val fullText = state.capturedImage.ocrResult.texts.joinToString(" ") { it.text }
        return if (fullText.isNotBlank()) fullText.take(200) else null
    }

    init {
        viewModelScope.launch {
            definitionRepository.preloadCommonWords(100)
        }
        // Continuously observe IPA overlay settings
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _showIpaOverlay.value = settings.showIpaOverlay
                _ipaFontScale.value = settings.ipaFontScale
            }
        }
        // J Coin: establish anonymous session + daily login earn
        viewModelScope.launch {
            try {
                deviceAuthRepository.ensureSession()
                // Fetch balance
                refreshCoinBalance()
                // Daily login earn
                val ctx = application.applicationContext
                val dailyLogin = jCoinEarnRules.checkDailyLogin(ctx)
                if (dailyLogin != null) {
                    earnCoins(dailyLogin.sourceType, dailyLogin.coins)
                }
            } catch (e: Exception) {
                Log.d(TAG, "J Coin init skipped: ${e.message}")
            }
        }
    }

    fun toggleFlash() {
        _isFlashOn.value = !_isFlashOn.value
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    fun processLiveFrame(imageProxy: ImageProxy) {
        // Skip if already in annotate mode
        if (_captureState.value is CaptureState.Annotate) {
            imageProxy.close()
            return
        }
        // Frame skip: process 1 of every 4 frames
        _liveFrameCount++
        if (_liveFrameCount % 4 != 0) {
            imageProxy.close()
            return
        }
        // Backpressure guard
        if (_isLiveProcessing) {
            imageProxy.close()
            return
        }
        _isLiveProcessing = true
        viewModelScope.launch {
            try {
                val mediaImage = imageProxy.image
                if (mediaImage == null) {
                    imageProxy.close()
                    return@launch
                }
                val rotation = imageProxy.imageInfo.rotationDegrees
                _liveRotationDegrees.value = rotation
                val inputImage = InputImage.fromMediaImage(mediaImage, rotation)
                val imageSize = if (rotation == 90 || rotation == 270) {
                    Size(mediaImage.height, mediaImage.width)
                } else {
                    Size(mediaImage.width, mediaImage.height)
                }
                _liveImageSize.value = imageSize

                val ocrResult = processCameraFrame.execute(inputImage, imageSize)

                // Persist previous results if current frame has fewer detections
                val currentWordCount = ocrResult.texts.sumOf { it.elements.size }
                if (currentWordCount == 0 && _prevLiveEnriched.isNotEmpty()) {
                    _emptyFrameCount++
                    if (_emptyFrameCount <= 3) {
                        return@launch
                    }
                }
                _emptyFrameCount = 0

                val enriched = wordEnrichmentRepository.enrichOcrWords(ocrResult)
                _prevLiveEnriched = enriched
                _liveEnrichedWords.value = enriched
            } catch (e: Exception) {
                Log.d(TAG, "Live frame processing error: ${e.message}")
            } finally {
                imageProxy.close()
                _isLiveProcessing = false
            }
        }
    }

    fun toggleIpaOverlay() {
        val newValue = !_showIpaOverlay.value
        _showIpaOverlay.value = newValue
        viewModelScope.launch {
            val current = settingsRepository.settings.first()
            settingsRepository.updateSettings(current.copy(showIpaOverlay = newValue))
        }
    }

    fun setInteractionMode(mode: InteractionMode) {
        _interactionMode.value = mode
    }

    fun onPhotoCapture(bitmap: Bitmap) {
        _isProcessing.value = true
        reviewedWordsThisScan.clear()
        allDifficultClearedThisScan = false
        _liveEnrichedWords.value = emptyList()
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

                // Enrich words with IPA + CEFR (fast, <100ms)
                enrichWords(ocrResult)

                // J Coin: scan_text earn + milestones
                fireJCoinScanEarns()

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
                        // Re-enrich with corrected text
                        enrichWords(corrected)
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
            // J Coin: sentence_analysis earn
            val earn = jCoinEarnRules.checkSentenceAnalysis(application.applicationContext)
            if (earn != null) earnCoins(earn.sourceType, earn.coins)
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

        // Compute readability immediately (available during loading)
        val metrics = readabilityCalculator.calculate(selectedText)
        _panelState.value = PanelState.AiLoading(scopeLevel, selectedText, metrics)

        viewModelScope.launch {
            // Gate: check free quota, then coin balance
            val ctx = application.applicationContext
            if (!jCoinSpendRules.checkFreeAiAnalysis(ctx)) {
                // Free quota exhausted — check coin balance
                val balance = _coinBalance.value ?: 0
                if (balance < 8) {
                    _panelState.value = PanelState.Error(
                        "Not enough J Coins (need 8, have $balance). Visit the Coin Shop to earn more!"
                    )
                    return@launch
                }
                // Deduct coins
                val token = deviceAuthRepository.getAccessToken()
                jCoinClient.spend(token, "ai_deep_analysis", 8)
                    .onSuccess { _coinBalance.value = it.newBalance.toInt() }
                    .onFailure {
                        _panelState.value = PanelState.Error("Coin spend failed: ${it.message}")
                        return@launch
                    }
            }
            jCoinSpendRules.consumeFreeAiAnalysis(ctx)

            val context = AnalysisContext(
                selectedText = selectedText,
                fullSnapshotText = fullText,
                scopeLevel = scopeLevel
            )

            aiProviderManager.analyze(context)
                .onSuccess { response ->
                    _panelState.value = PanelState.AiAnalysis(scopeLevel, selectedText, response, metrics)
                    val scopeName = when (scopeLevel) {
                        is ScopeLevel.Word -> "word"
                        is ScopeLevel.Phrase -> "phrase"
                        is ScopeLevel.Sentence -> "sentence"
                        is ScopeLevel.Paragraph -> "paragraph"
                        is ScopeLevel.FullSnapshot -> "full"
                    }
                    launch {
                        historyRepository.recordLookup(
                            word = selectedText.take(100),
                            scopeLevel = scopeName,
                            aiProvider = response.provider
                        )
                    }
                    // Accumulate token usage
                    val input = response.inputTokens ?: 0
                    val output = response.outputTokens ?: (response.tokensUsed ?: 0)
                    if (input + output > 0) {
                        launch { settingsRepository.addTokenUsage(response.provider, input, output) }
                    }
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

    fun setCefrThreshold(level: CefrLevel) {
        _cefrThreshold.value = level
        // J Coin: repeatable threshold adjusted earn (+1, 3/day cap)
        val thresholdEarn = jCoinEarnRules.checkThresholdAdjusted(application.applicationContext)
        if (thresholdEarn != null) earnCoins(thresholdEarn.sourceType, thresholdEarn.coins)
        // J Coin: first threshold set milestone (on non-default)
        if (level != CefrLevel.B2) {
            fireFirstThresholdSetMilestone()
        }
        // Re-filter difficult words if currently showing the panel
        val current = _panelState.value
        if (current is PanelState.DifficultWordsList) {
            val filtered = _enrichedWords.value.filter { word ->
                word.cefr != null && word.cefr.ordinalIndex >= level.ordinalIndex
            }
            _panelState.value = PanelState.DifficultWordsList(filtered, level)
        }
    }

    fun showDifficultWordsPanel() {
        val threshold = _cefrThreshold.value
        val filtered = _enrichedWords.value.filter { word ->
            word.cefr != null && word.cefr.ordinalIndex >= threshold.ordinalIndex
        }
        // Deduplicate by word text
        val unique = filtered.distinctBy { it.text }
        _panelState.value = PanelState.DifficultWordsList(unique, threshold)
    }

    private fun enrichWords(ocrResult: com.jworks.eigolens.domain.models.OCRResult) {
        viewModelScope.launch {
            val enriched = wordEnrichmentRepository.enrichOcrWords(ocrResult)
            _enrichedWords.value = enriched

            // Auto-show difficult words panel
            val threshold = _cefrThreshold.value
            val difficult = enriched.filter { word ->
                word.cefr != null && word.cefr.ordinalIndex >= threshold.ordinalIndex
            }.distinctBy { it.text }

            if (difficult.isNotEmpty() && _panelState.value is PanelState.Idle) {
                _panelState.value = PanelState.DifficultWordsList(difficult, threshold)
                // J Coin: first CEFR scan milestone
                fireFirstCefrScanMilestone()
            }
        }
    }

    fun switchToWordLookup() {
        _panelState.value = PanelState.Idle
        _currentWord.value = null
    }

    fun resetToCamera() {
        _captureState.value = CaptureState.Camera
        _panelState.value = PanelState.Idle
        _interactionMode.value = InteractionMode.TAP
        _tappedWord.value = null
        _currentWord.value = null
        _enrichedWords.value = emptyList()
        _liveEnrichedWords.value = emptyList()
        _prevLiveEnriched = emptyList()
        _emptyFrameCount = 0
        reviewedWordsThisScan.clear()
        allDifficultClearedThisScan = false
    }

    private fun lookupWord(query: String, fallbackWord: String? = null) {
        viewModelScope.launch {
            _panelState.value = PanelState.Loading

            definitionRepository.getDefinition(query)
                .onSuccess {
                    _panelState.value = PanelState.WordDefinition(it)
                    _currentWord.value = it.word
                    launch { historyRepository.recordLookup(it.word, "word", getContextSnippet()) }
                    fireDifficultWordReviewedEarn(it.word)
                }
                .onFailure {
                    if (fallbackWord != null) {
                        definitionRepository.getDefinition(fallbackWord)
                            .onSuccess {
                                _panelState.value = PanelState.WordDefinition(it)
                                _currentWord.value = it.word
                                launch { historyRepository.recordLookup(it.word, "word", getContextSnippet()) }
                            }
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

    // -- J Coin helpers --

    private suspend fun refreshCoinBalance() {
        val token = deviceAuthRepository.getAccessToken()
        jCoinClient.getBalance(token)
            .onSuccess { balance -> _coinBalance.value = balance.balance }
            .onFailure { Log.d(TAG, "Balance fetch skipped: ${it.message}") }
    }

    private fun earnCoins(sourceType: String, baseAmount: Int, metadata: Map<String, String> = emptyMap()) {
        viewModelScope.launch {
            val token = deviceAuthRepository.getAccessToken()
            jCoinClient.earn(token, sourceType, baseAmount, metadata)
                .onSuccess { response ->
                    _coinBalance.value = response.newBalance.toInt()
                    Log.d(TAG, "J Coin earned: +${response.coinsAwarded} ($sourceType), balance=${response.newBalance}")
                }
                .onFailure { Log.d(TAG, "J Coin earn skipped ($sourceType): ${it.message}") }
        }
    }

    private fun fireJCoinScanEarns() {
        val ctx = application.applicationContext
        val scanEarn = jCoinEarnRules.checkScanText(ctx)
        if (scanEarn != null) {
            earnCoins(scanEarn.sourceType, scanEarn.coins)
        }
        // Scan milestones
        for (milestone in jCoinEarnRules.checkScanMilestones(ctx)) {
            earnCoins(milestone.sourceType, milestone.coins)
        }
        // Streak milestones
        for (milestone in jCoinEarnRules.checkStreakMilestones(ctx)) {
            earnCoins(milestone.sourceType, milestone.coins)
        }
    }

    private fun fireDifficultWordReviewedEarn(word: String) {
        val threshold = _cefrThreshold.value
        val difficultWords = _enrichedWords.value.filter { w ->
            w.cefr != null && w.cefr.ordinalIndex >= threshold.ordinalIndex
        }.map { it.text.lowercase() }.distinct()

        // Only earn if this word is in the difficult list and not already reviewed this scan
        val lowerWord = word.lowercase()
        if (lowerWord !in difficultWords) return
        if (!reviewedWordsThisScan.add(lowerWord)) return // already reviewed

        val ctx = application.applicationContext
        // difficult_word_reviewed: +1 coin, 15/day cap
        val earn = jCoinEarnRules.checkDifficultWordReviewed(ctx)
        if (earn != null) {
            earnCoins(earn.sourceType, earn.coins)
        }
        // CEFR word milestones (50/200/500 lifetime)
        for (milestone in jCoinEarnRules.checkCefrWordMilestones(ctx)) {
            earnCoins(milestone.sourceType, milestone.coins)
        }

        // Check if all difficult words have been reviewed
        if (!allDifficultClearedThisScan && reviewedWordsThisScan.containsAll(difficultWords)) {
            allDifficultClearedThisScan = true
            val clearEarn = jCoinEarnRules.checkAllDifficultCleared(ctx)
            earnCoins(clearEarn.sourceType, clearEarn.coins)
        }
    }

    private fun fireFirstCefrScanMilestone() {
        if (hasEarnedFirstCefrScan) return
        hasEarnedFirstCefrScan = true
        val ctx = application.applicationContext
        val milestone = jCoinEarnRules.checkFirstCefrScan(ctx)
        if (milestone != null) {
            earnCoins(milestone.sourceType, milestone.coins)
        }
    }

    private fun fireFirstThresholdSetMilestone() {
        if (hasSetNonDefaultThreshold) return
        hasSetNonDefaultThreshold = true
        val ctx = application.applicationContext
        val milestone = jCoinEarnRules.checkFirstThresholdSet(ctx)
        if (milestone != null) {
            earnCoins(milestone.sourceType, milestone.coins)
        }
    }
}
