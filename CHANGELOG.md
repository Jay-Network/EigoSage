# Changelog

All notable changes to EigoSage will be documented in this file.

## v0.5.1 (2026-03-01) - Rename EigoLens → EigoSage

### Changed
- Renamed app from EigoLens to EigoSage (package, strings, branding, docs)
- Package: com.jworks.eigolens → com.jworks.eigosage
- All SharedPreferences/DataStore/DB identifiers updated (secure prefs, session, settings, jcoin, user DB)
- J Coin SOURCE_BUSINESS: eigolens → eigosage
- EigoQuest cross-app transfer sourceApp updated
- All docs updated (README, STATUS, CLAUDE.md, store-listing, STORE_READINESS, data-pipeline)
- Privacy policy URL refs: /eigolens → /eigosage
- J Coin APP_KEY: updated to new EigoSage-specific key (backend migration)
- Applied vMAJOR.MINOR.PATCH versioning standard

### Added
- VERSION file (single source of truth for semver)

## v0.5.0 (2026-03-01) - In-App AI Chat + Phase E

### Added
- **In-app Gemini chat**: Multi-turn conversational AI replacing Chrome Custom Tab approach
  - GeminiChatClient: standalone multi-turn Gemini API client (gemini-2.5-flash)
  - ChatPanel UI: message bubbles, suggestion chips, text input bar
  - PanelState.Chat + ChatMessage/ChatRole data models
  - Context-seeded: auto-includes OCR text + current analysis/definition/word list
  - Entry points: AiAnalysisPanel footer, DefinitionPanel header, DifficultWordsPanel header
  - In-memory only, resets on new capture
- **IPA Pronunciation Overlay** (Phase E):
  - IPA phonetic transcription via CMU Pronouncing Dictionary (ARPAbet→IPA)
  - CEFR difficulty detection via Brown Corpus frequency ranks
  - WordNet DB v2: added phonetic + cefr_level columns
  - CEFR-colored boxes + IPA text overlay on Canvas
  - DifficultWordsPanel: auto-shown after capture, CEFR badges, IPA, definitions
  - CEFR threshold slider (6 discrete stops, persisted)
  - AiAnalysisPanel: 5th "Words" tab when difficult words exist
  - DefinitionPanel: IPA + CEFR badge in WordHeader
- **Word enrichment pipeline**: `data-pipeline/enhance_wordnet.py`
- **WordEnrichmentRepository**: batch enrichment with LRU cache, lemma fallback

### Removed
- `androidx.browser:browser` dependency (Chrome Custom Tabs no longer needed)

## v0.4.0 (2026-02-24) - Phase C: Word History & Bookmarks

### Added
- **Word history tracking**: All lookups and AI analyses recorded with timestamps and scope level
- **Bookmark words**: Tap bookmark icon in definition panel header to save words
- **History screen**: New screen with two tabs (Recent lookups + Saved bookmarks)
- **Separate user database**: `eigosage_user.db` for user data (keeps wordnet.db read-only)
- **HistoryRepository**: Centralized data access for lookup history and bookmarks
- **HistoryViewModel**: Dedicated ViewModel for history screen
- **History access button**: MenuBook icon in camera preview
- `material-icons-extended` dependency

### Changed
- DefinitionPanel: WordHeader includes bookmark toggle icon
- CaptureFlowViewModel: Injects HistoryRepository, records lookups on success
- DatabaseModule: Provides UserDatabase, HistoryDao, BookmarkDao via Hilt

## v0.3.1 (2026-02-24) - Phase B+: UI Polish & Interaction Modes

### Added
- **Long-press sentence analysis**: Long-press word → AI analyzes containing sentence
- **Full-text AI FAB**: Star FAB triggers AI analysis of all captured text
- **ScopeLevel.Sentence**: New scope level with dedicated prompt template
- **Segmented provider selector**: Settings screen provider toggle
- **Card-based key editors**: API keys in cards with Save/Clear buttons
- **Full M3 color scheme**: Indigo primary, teal secondary, amber tertiary (light+dark)
- **Custom EigoTypography**: Consistent scale across all panels
- **Synonym/antonym chips**: FlowRow of SuggestionChips in DefinitionPanel
- **Sectioned AI panel**: Header/LazyColumn/footer architecture

### Changed
- All hardcoded colors → MaterialTheme.colorScheme tokens (dark mode ready)
- OCR overlay: top gradient scrim (72dp), darker word-count chip
- Labeled FABs: "AI Analyze" / "Reading Level"
- Back button: 40dp circle with black scrim
- Feedback modal: padding, chip spacing, 48dp CTA, HorizontalDivider

## v0.3.0 (2026-02-24) - Phase B: AI Integration

### Added
- **AI phrase/paragraph analysis**: Circle words for AI-powered explanations
- **Claude provider**: Anthropic API (Haiku 4.5, 1024 token max) via Ktor
- **Gemini provider**: Google Gemini API (2.0 Flash, temperature 0.3)
- **AiProviderManager**: Provider registry with fallback (Claude → Gemini)
- **AiPrompts**: ESL-optimized prompt templates per scope level
- **SecureKeyStore**: Encrypted API key storage (Android Keystore + EncryptedSharedPreferences)
- **AiAnalysisPanel**: AI response with scope badges, markdown, timing/token footer
- **GeminiOcrCorrector**: Background Gemini Vision pass for OCR error correction
- **OcrTextMerger**: Smart word alignment for Gemini corrections
- **Settings: AI section**: API key management with encrypted storage

### Changed
- Scope routing: 1 word → local WordNet, 2-8 → AI phrase, 9+ → AI paragraph

## v0.2.0 (2026-02-24) - Phase A: Tap-to-Define

### Added
- **Tap-to-define**: Tap any word on captured text for instant definitions
- **WordTapDetector**: Screen-to-image coordinate transform with 20px tolerance
- **ScopeLevel**: Foundation for progressive analysis (Word, Phrase, Paragraph, FullSnapshot)
- **Overlay panel**: Draggable results panel over full-screen image
- **Pulse highlight**: Blue rounded rect animation on tapped words
- **Landscape support**: Panel anchors to right side
- **Robolectric**: Unit test support, 6 WordTapDetector tests

### Changed
- InteractionMode: VIEW/DRAW → TAP/CIRCLE (TAP default)
- Two-finger zoom/pan works in both TAP and CIRCLE modes
- Circle mode auto-switches back to TAP after selection
- PanelState: Unified sealed class (was LookupState + AnalysisMode)

### Fixed
- Room schema validation (NOT NULL on primary keys)
- Room entity annotations (indices, foreign keys)
- Room identity hash matching
- Nullable frequency column

## v0.1.1 (2026-02-20) - Build Stability & Rebrand

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

## v0.1.0 (2026-02-15) - Initial Release

### Added
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
