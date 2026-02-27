# EigoLens

**Camera-based English text analyzer for language learners**

Point your camera at any English text, capture it, then tap any word for instant definitions. Circle phrases for multi-word analysis. Get reading level scores for any text.

## Features

- **Tap-to-define**: Tap any word on a captured snapshot for instant offline definitions
- **Circle selection**: Switch to circle mode to lasso multiple words or phrases
- **AI phrase/paragraph analysis**: Circle 2+ words for AI-powered explanations (Claude or Gemini)
- **Scope-aware routing**: 1 word → local WordNet, 2-8 words → AI phrase analysis, 9+ → AI paragraph analysis
- **Offline dictionary**: 147,000+ words, 207,000+ definitions powered by WordNet
- **Smart lemmatization**: Inflected forms ("running", "went", "mice") resolve to base words
- **Readability analysis**: Flesch-Kincaid, Flesch Reading Ease, SMOG, Coleman-Liau scores
- **NLP pipeline**: Parts of speech, named entities, word frequency
- **Gemini OCR correction**: Background Gemini Vision pass to fix ML Kit OCR errors
- **Overlay panel**: Draggable results panel over full-screen image - resize by dragging
- **Two-finger zoom/pan**: Works alongside tap/circle without mode switching
- **Encrypted API key storage**: Android Keystore-backed encrypted SharedPreferences
- **Word history**: Automatic tracking of all lookups with timestamps and scope levels
- **Bookmarks**: Save words with definitions and context for later review
- **History screen**: Recent lookups and saved words in a tabbed interface
- **Gallery import**: Analyze photos from your gallery
- **Guest mode**: Core features work without an account

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose |
| Camera | CameraX |
| OCR | Google ML Kit (Text Recognition) |
| Dictionary | WordNet (bundled SQLite, offline) |
| NLP | Rule-based lemmatizer, POS tagger, NER |
| AI Providers | Claude (Haiku 4.5), Gemini (2.5 Flash) |
| AI Client | Ktor (Android engine) |
| Key Storage | EncryptedSharedPreferences (security-crypto) |
| Database | Room |
| DI | Hilt |
| Auth | Supabase (optional) |
| Min SDK | Android 8.0+ (API 26) |

## Architecture

```
app/src/main/java/com/jworks/eigolens/
├── data/
│   ├── ai/             # AI providers (Claude, Gemini), prompts, OCR correction
│   ├── auth/           # Supabase auth repository
│   ├── local/          # Room DBs (WordNet read-only + user data), DAOs, entities
│   ├── preferences/    # DataStore settings, SecureKeyStore (encrypted API keys)
│   └── repository/     # Definition repository, HistoryRepository
├── di/                 # Hilt modules (AppModule, AiModule)
├── domain/
│   ├── ai/             # AiProvider interface, AiResponse, AnalysisContext
│   ├── analysis/       # Readability calculator
│   ├── auth/           # Auth interfaces
│   ├── models/         # OCRResult, Definition, DetectedText, ScopeLevel, etc.
│   ├── nlp/            # POS tagger, NER, lemmatizer
│   └── usecases/       # ProcessCameraFrame
└── ui/
    ├── auth/           # Login screen
    ├── camera/         # DefinitionPanel, ReadabilityPanel
    ├── capture/        # CaptureFlow, AnnotationMode, AiAnalysisPanel
    ├── feedback/       # In-app feedback
    ├── gallery/        # Gallery import
    ├── history/        # History screen (recent lookups + bookmarks)
    ├── settings/       # Settings screen (API key management)
    └── theme/          # Material3 theme
```

### Interaction Model

EigoLens uses a **snapshot-first** approach:

1. **Capture**: Take a photo or import from gallery. ML Kit OCR detects all text.
2. **Tap** (default): Single-tap any word → definition panel slides up. Two-finger zoom/pan works simultaneously.
3. **Long-press**: Long-press any word → AI analyzes the containing sentence (grammar, meaning, vocabulary).
4. **Circle** (toggle): Switch to circle mode via FAB → draw around words → AI phrase/paragraph analysis. Auto-returns to tap mode.
5. **Full Text AI**: Star FAB triggers AI analysis of the entire captured text.
6. **Readability**: Book FAB provides reading level scores (Flesch-Kincaid, SMOG, etc.).

The results panel overlays the image (draggable height) so the snapshot stays full-screen.

## Build

```bash
# Debug build
./gradlew assembleDebug

# Run tests
./gradlew testDebugUnitTest

# Release build (requires signing config in local.properties)
./gradlew assembleRelease
```

### local.properties

```properties
# Supabase (optional - app works without)
AUTH_SUPABASE_URL=https://your-project.supabase.co
AUTH_SUPABASE_ANON_KEY=your-anon-key

# Signing (required for release builds)
RELEASE_STORE_FILE=../keystore/eigolens-release.jks
RELEASE_STORE_PASSWORD=...
RELEASE_KEY_ALIAS=eigolens
RELEASE_KEY_PASSWORD=...
```

## Roadmap

- **Phase A** (done): Tap-to-define, overlay panel, zoom/pan coexistence
- **Phase B** (done): AI-powered phrase/sentence analysis (Claude + Gemini), encrypted key storage, scope routing, Gemini OCR correction
- **Phase B+** (done): UI polish, M3 theming, long-press sentence analysis, full-text AI FAB, labeled FABs, overlay readability, provider selection UX
- **Phase C** (done): Word history & bookmarks - automatic lookup tracking, bookmark toggle in definition panel, history screen with Recent/Saved tabs
- **Phase D** (next): Spaced repetition, study mode, VocabQuest export, cross-session AI context

## License

Proprietary - JWorks

---

**Developer**: JWorks | **Contact**: jay@jworks-ai.com | **Website**: https://jworks-ai.com
