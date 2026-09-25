# Changelog

All notable changes to EigoSage will be documented in this file.

Format based on [Keep a Changelog](https://keepachangelog.com/).

## [Unreleased]

## v0.8.4 (2026-09-25) - Fix Release CI: Missing Keystore Directory

### Fixed
- **Release AAB build failing since 2026-07-23**: `.github/workflows/android-build.yml`'s "Decode keystore" step wrote to `keystore/eigosage-release.jks` without creating the `keystore/` directory first, failing every push-to-master release build at that step (`No such file or directory`). Debug build and unit tests were unaffected — only the release job was broken. Added `mkdir -p keystore` before the decode. Found and diagnosed by jworks:22/jworks-codex:22's CI ownership sweep; not a credential problem.

## v0.8.3 (2026-09-06) - Fix Broken "Send to EigoQuest" Word Export

### Fixed
- **Stale pre-rename `target_app`**: The "Send to EigoJourney" difficult-words export (shipped as "Send to EigoQuest," commit f1fd6aa era) wrote `target_app: "eigoquest"` to the shared `eq_received_words` Supabase table. EigoJourney's sync worker filters on `target_app='eigojourney'`, so every send silently succeeded (row inserted, UI showed "Sent N words") but was never picked up — confirmed against EigoJourney's own code by jworks:45. Renamed `EigoQuestTransferRepository` → `EigoJourneyTransferRepository`, fixed the `target_app` default, and updated all UI text/callback names across `CaptureFlowViewModel`, `AnnotationMode`, and `DifficultWordsPanel` to match.
- Not live-device-verified — needs both apps installed on one real Android phone to exercise the actual 15-minute pull cycle.

## v0.8.2 (2026-09-04) - Gemini Live Prototype (Protocol Layer)

### Added
- **`GeminiLiveClient`**: WebSocket client for the Gemini Live API (BidiGenerateContent) — setup/audio-chunk/text-turn message builders and server-event parsing (text delta, audio delta, turnComplete, interrupted), verified against current `ai.google.dev` Live API docs. 13 unit tests.
- **`ChatPersona.liveVoiceName`**: maps Sage/Lexicon/Tutor personas to Live API voice names (Charon/Kore/Puck) for future voice sessions.
- Added `ktor-client-okhttp` + `ktor-client-websockets` dependencies (isolated to the Live client — Ktor's Android engine has no WebSocket support, so REST clients are untouched).

Not yet wired to microphone/speaker or the UI — that requires a physical device and is tracked separately in TODO.md.

## v0.8.1 (2026-06-01) - Gemini Thinking Token Hardening

### Fixed
- **S-358: Thinking token truncation risk**: Added `thinkingConfig.thinkingBudget: 0` to all 4 Gemini REST API call sites (GeminiOcrCorrector, GeminiProvider, GeminiChatClient) to explicitly disable thinking tokens — prevents silent output truncation when upgrading to Gemini 3.5 Flash (which enables thinking by default)
- **Contextual insight token limit**: Bumped maxOutputTokens 256 → 512 on `getContextualInsight()` (was critically tight)
- **Chat token limit**: Bumped maxOutputTokens 1024 → 2048 on `GeminiChatClient.send()` for longer conversations with suggestion markup

## v0.8.0 (2026-04-24) - Professional Scanning Modes

### Added
- **Professional scanning modes**: 4-mode selector (Standard, Interpreter, Medical, Legal) in camera toolbar
  - ScanMode enum with display names, short labels, and descriptions
  - Mode-specific Gemini system prompts for analysis and chat
  - Interpreter mode: translation aids, cultural context, idiomatic expressions, false friends
  - Medical mode: clinical terminology, drug names, anatomical terms, safety-critical flagging (⚠️)
  - Legal mode: legal terms of art, clause extraction, obligations vs. permissions, ambiguity detection
- **Scan mode selector UI**: Compact pill row above CEFR slider with color-coded mode indicators
  - STD (indigo), INT (sky blue), MED (emerald), LAW (amber)
- **Mode badge in AI analysis header**: Non-standard modes display a colored chip next to provider name
- **Mode-aware chat**: Chat system prompt adapts to active scan mode with domain-specific guidance
- String resources for all scan mode labels

### Changed
- AnalysisContext now carries scanMode field (defaults to STANDARD for backward compatibility)
- GeminiProvider and ClaudeProvider use mode-specific system prompts
- buildCefrSystemPrompt accepts optional scanMode parameter for chat context

## v0.7.0 (2026-04-15) - Chat Intelligence & Context Memory

### Added
- **Cross-session chat persistence**: Chat history saved to Room DB per captured text, survives app restarts
  - ChatSessionEntity + ChatMessageEntity: Room entities with session metadata and foreign key cascading
  - ChatDao: Full CRUD with session listing, message flows, and count queries
  - ChatRepository: Business logic for session save/load/delete with auto-timestamping
- **Chat follow-up from history**: Resume any past chat session from the History screen Chats tab
  - HistoryScreen: New "Chats" tab showing all sessions with OCR preview, message count, CEFR level
  - CaptureFlowViewModel.resumeChatSession(): Reconstructs chat state from persisted messages
- **Smart suggestion chips**: AI-generated CEFR-adapted follow-up questions after each model response
  - GeminiChatClient: Parses `[SUGGESTIONS]` block from model output
  - ChatPanel: Dynamic suggestion chips replace static fallbacks when available
- **Vocabulary extraction from chat**: Auto-bookmark bold words (**term**) discussed in AI responses
  - BOLD_WORD_REGEX extraction in CaptureFlowViewModel
  - "Save N words" AssistChip in ChatPanel when terms detected
- **Chat export**: Share chat conversations as plain text or PDF
  - ChatExporter: Formats conversations as text or renders to A4 PDF with role-colored labels
  - Share menu in ChatPanel header and History Chats tab per session
  - Android share intent with FileProvider for PDF sharing
- **CEFR-adapted Gemini chat system prompt**: System prompt adapts explanations to user's CEFR level
- **Readability score in chat context**: Flesch-Kincaid + difficulty level seeded into chat context
- **Persona mode**: Three distinct AI personas with specialized system prompts
  - Sage (default): Reading comprehension, main ideas, context clues, text structure
  - Lexicon: Vocabulary focus — definitions, synonyms, etymology, word families
  - Tutor: Grammar practice, translation exercises, active drills with feedback
  - ChatPersona enum with persona-specific system prompt generation
  - Persona selector chip in ChatPanel header (locked after first message)

### Changed
- UserDatabase: Schema version bump with chat_sessions and chat_messages tables
- DatabaseModule: Provides ChatDao + ChatRepository via Hilt
- CaptureFlowViewModel: Injects ChatRepository + ChatExporter, auto-saves sessions on chat dismiss

## v0.6.0 (2026-04-07) - SRS Study System + Crash Safety

### Added
- **SRS flashcard system** (Phase D): SM-2 spaced repetition algorithm for vocabulary mastery
  - SrsCardEntity + StudySessionEntity: Room entities with SM-2 fields (ease factor, interval, repetitions)
  - SrsDao: Full CRUD with due-card queries, session tracking, per-card history
  - SrsRepository: Business logic layer for card reviews and session management
  - Sm2Algorithm: Standard SM-2 implementation with quality-based scheduling
  - StudyDashboardScreen: Overview with due-card count, streak, session history
  - FlashcardSessionScreen: Interactive flashcard UI with flip animation and quality rating
  - SessionCompleteScreen: Post-session summary with stats
- **Bookmark → Study deck integration**: Add individual or all saved words to SRS deck from History Saved tab
  - "Add to Study Deck" button per bookmark, "Add All" bulk action
  - HistoryViewModel: SRS integration with duplicate detection
- **Study screen navigation**: MenuBook sidebar entry + Compose Navigation route
- **Auto-populate SRS**: Difficult words from capture automatically seed study deck
- **AnnotationMode**: Extracted annotation rendering logic into dedicated composable
- **CameraPreviewMode**: Extracted camera preview logic into dedicated composable

### Changed
- **Crash safety hardening**: Eliminated all `!!` operators across codebase
  - Added global CoroutineExceptionHandler in EigoSageApplication
  - PdfExporter: Safe bitmap/canvas rendering with null checks
  - FeedbackDialog: Safe text field handling
  - InteractiveImageViewer: Safe gesture callbacks
- UserDatabase: Schema version bump with SRS tables, added migration support
- DatabaseModule: Provides SrsDao + SrsRepository via Hilt
- DifficultWordsPanel: Refactored layout, added "Add to Study" per-word action
- HistoryScreen: Added study deck controls in Saved tab
- CaptureFlowViewModel: Injects SrsRepository, auto-adds difficult words to deck
- BookmarkDao: Added query for fetching all bookmarked words for bulk SRS import

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
