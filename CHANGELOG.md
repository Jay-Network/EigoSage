# Changelog

All notable changes to EigoLens will be documented in this file.

## [0.3.1] - 2026-02-24 (Phase B+ - UI Polish & Interaction Modes)

### Added
- **Long-press sentence analysis**: Long-press any word → AI analyzes the containing sentence (meaning, grammar, vocabulary, tone)
- **Full-text AI FAB**: Small star FAB in bottom-left triggers AI analysis of all captured text
- **ScopeLevel.Sentence**: New scope level with dedicated prompt template
- **Segmented provider selector**: Settings screen has segmented button row to choose preferred AI provider
- **Card-based key editors**: Settings API keys in cards with explicit Save/Clear buttons and saved status
- **Full M3 color scheme**: Light/dark theme with indigo primary, teal secondary, amber tertiary
- **Custom typography**: EigoTypography with consistent scale across all panels
- **Synonym/antonym chips**: FlowRow of SuggestionChips replacing plain text rows in DefinitionPanel
- **Sectioned AI panel**: AiAnalysisPanel with sticky header, scrollable section cards, and footer chips
- **Slide-in animation**: DefinitionPanel slide offset now applied (was animated but unused)

### Changed
- All hardcoded colors replaced with MaterialTheme.colorScheme tokens (dark mode ready)
- AiAnalysisPanel: flat column → header/LazyColumn/footer architecture
- DefinitionPanel: headlineMedium → headlineSmall for word header
- Settings: "Tap to save" links → explicit Save/Clear buttons in card layout
- Panel backgrounds use `surface.copy(alpha = 0.97f)` instead of hardcoded `#F8F9FA`
- Scope badges: each scope level gets its own M3 color (primary/secondary/tertiary)
- **OCR overlay readability**: Top gradient scrim (72dp), darker word-count chip (0.7 alpha), labelMedium text
- **Labeled FABs**: Icon-only SmallFABs → ExtendedFloatingActionButton with "AI Analyze" / "Reading Level" labels
- **Back button**: Larger 40dp circle with black scrim background for discoverability
- **Feedback modal**: 20dp padding, wider chip spacing, 48dp CTA button, HorizontalDivider before history section

## [0.3.0] - 2026-02-24 (Phase B - AI Integration)

### Added
- **AI phrase analysis**: Circle 2-8 words → AI-powered phrase explanation (meaning, grammar, vocabulary, usage)
- **AI paragraph analysis**: Circle 9+ words → AI summary, key ideas, vocabulary, tone analysis
- **Full-text analysis**: `analyzeFullText()` method for complete snapshot AI analysis
- **Claude provider**: Anthropic API integration (Haiku 4.5, 1024 token max) via Ktor HTTP client
- **Gemini provider**: Google Gemini API integration (2.0 Flash, temperature 0.3)
- **AiProviderManager**: Provider registry with automatic fallback (Claude → Gemini)
- **AiPrompts**: ESL-optimized prompt templates per scope level (Word, Phrase, Paragraph, FullText)
- **SecureKeyStore**: Encrypted API key storage via Android Keystore + EncryptedSharedPreferences
- **AiAnalysisPanel**: AI response overlay with scope badges, markdown rendering, timing/token footer
- **AiLoadingPanel**: Loading spinner with selected text preview and scope badge
- **SimpleMarkdownText**: Basic markdown renderer (bold, bullets, headings)
- **GeminiOcrCorrector**: Background Gemini Vision pass to fix ML Kit OCR text errors
- **OcrTextMerger**: Smart word alignment merging Gemini Vision corrections with ML Kit bounding boxes
- **Contextual word insights**: Parallel Gemini-powered meaning/POS/note for tapped words
- **Settings: AI section**: API key inputs for Claude and Gemini with encrypted storage
- **Settings: Provider status**: Active provider display and available providers list
- **AiModule**: Hilt DI module for AI providers, HTTP client, SecureKeyStore
- **security-crypto dependency**: `androidx.security:security-crypto:1.1.0-alpha06`

### Changed
- **Scope routing**: 1 word → local WordNet, 2-8 words → AI phrase, 9+ → AI paragraph
- **CaptureFlowViewModel**: Injected AiProviderManager, added AI loading/analysis panel states
- **PanelState**: Added `AiLoading` and `AiAnalysis` states
- **AnnotationMode**: Routes new AI panel states to AiAnalysisPanel/AiLoadingPanel
- **SettingsViewModel**: Exposes API key management, provider state, dynamic provider registration
- **Settings screen**: Scrollable, AI section with key inputs, version bumped to 0.2.0
- **ProcessCameraFrameUseCase**: Background Gemini OCR correction after capture

## [0.2.0] - 2026-02-24 (Phase A - Tap-to-Define)

### Added
- **Tap-to-define**: Tap any word on captured text for instant definitions (replaces lasso-only)
- **WordTapDetector**: Screen-to-image coordinate transform with 20px tolerance for imprecise taps
- **ScopeLevel**: Foundation for progressive analysis (Word, Phrase, Paragraph, FullSnapshot)
- **Overlay panel**: Draggable results panel over full-screen image (replaces 40/60 split layout)
- **Pulse highlight**: Blue rounded rect with animation on tapped words
- **Landscape support**: Panel anchors to right side in landscape orientation
- **ic_tap / ic_circle drawables**: New icons for interaction mode toggle
- **Robolectric**: Unit test support for Android classes
- 6 unit tests for WordTapDetector (roundtrip transforms, tap detection, tolerance, zoom/pan)

### Changed
- **InteractionMode**: `VIEW/DRAW` → `TAP/CIRCLE`. TAP is default, CIRCLE for lasso
- **Two-finger zoom/pan**: Now works in both TAP and CIRCLE modes (was VIEW-only)
- **Auto-return**: Circle mode auto-switches back to TAP after selection
- **PanelState**: Unified `LookupState` + `AnalysisMode` into single `PanelState` sealed class
- **InteractiveImageViewer**: State hoisted to parent, receives interaction mode + callbacks
- Idle message: "Tap any word to look it up, or switch to circle mode to select phrases"
- FAB icon: Shows opposite mode (tap icon when in circle, circle icon when in tap)

### Fixed
- **Room schema validation**: Rebuilt WordNet DB tables with `NOT NULL` on primary keys
- **Room entity annotations**: Added indices and foreign key to match bundled DB schema
- **Room identity hash**: Updated to match entity definitions
- **Nullable frequency**: `WordEntry.frequency` now `Int?` to match DB column

## [0.1.1] - 2026-02-20

### Changed
- Renamed from EnglishLens to EigoLens (full rebrand, package rename)

### Fixed
- ProGuard rules for release build stability
- Firebase graceful degradation (auto-init disabled)
- Session persistence with SharedPrefsSessionManager
- Camera null bitmap handling
- Runtime camera permission request
- Theme parent mismatch
- Misleading UI text for guest mode

### Added
- App icon (blue background, white magnifying glass vector)
- Store readiness checklist
- Store listing draft

## [0.1.0] - 2026-02-15

### Added
- Initial release
- Camera text capture with CameraX + ML Kit OCR
- Offline WordNet dictionary (147K words, 207K definitions)
- Lasso word selection with coordinate transforms
- Definition panel with POS tags, synonyms, antonyms, frequency badges
- Readability analysis (Flesch-Kincaid, Flesch RE, SMOG, Coleman-Liau)
- NLP pipeline (POS tagger, NER detector, lemmatizer)
- Gallery photo import
- Supabase auth (Google Sign-In, email/password)
- In-app feedback system
- Settings screen
