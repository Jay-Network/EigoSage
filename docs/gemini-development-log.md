# Gemini Development Log — EigoSage

## Purpose

Track Gemini integration development for EigoSage. Claude (jworks:46) is the lead development agent; Gemini powers in-app AI features (chat tutor, OCR correction, text analysis). This log documents prompt iterations, Claude vs Gemini comparisons, ADK patterns, and TODOs.

Reference: jworks:104 (SheetMusicReader) for dual output pattern.

---

## Architecture

```
                  ┌──────────────┐
  Camera Frame ──▶│  ML Kit OCR  │──▶ Fast text extraction (offline, ~50ms)
                  └──────────────┘
                         │
                         ▼
                  ┌──────────────┐
                  │ GeminiOCR    │──▶ Vision-corrected text (online, ~1-2s)
                  │ Corrector    │    Merges with ML Kit via OcrTextMerger
                  └──────────────┘
                         │
              ┌──────────┴──────────┐
              ▼                     ▼
     ┌──────────────┐      ┌──────────────┐
     │ GeminiProvider│      │GeminiChat    │
     │ (Analysis)    │      │Client (Tutor)│
     └──────────────┘      └──────────────┘
     Word definitions,      Multi-turn English
     grammar, reading       tutor dialog with
     level, CEFR tags       context from scan
```

- **Claude (jworks:46)**: Lead developer — architecture, complex features, code review
- **Gemini (in-app)**: Three roles — OCR correction, text analysis, chat tutor
- **Model**: `gemini-2.5-flash` across all three clients

## SDK & Tools

- **In-app SDK**: Direct REST API via Ktor (`generativelanguage.googleapis.com/v1beta`)
- **Model**: `gemini-2.5-flash` (vision + text)
- **ADK**: `@google/adk` v0.5.0 (available for Live agent — not yet integrated)
- **Potential Live SDK**: `@google/genai` for Gemini Live voice agent

---

## Current Gemini Features (v0.8.0)

### 1. OCR Correction (`GeminiOcrCorrector`)
- **What**: Sends camera frame bitmap to Gemini Vision for text extraction
- **Why**: Supplements ML Kit for difficult text (handwriting, stylized fonts, poor lighting)
- **Merge strategy**: `OcrTextMerger` combines ML Kit + Gemini results
- **Prompt**: Minimal — "extract all visible English text, return line-by-line"

### 2. Text Analysis (`GeminiProvider`)
- **What**: Analyzes scanned text for definitions, grammar, reading level
- **Why**: Core value prop — point camera at text, get instant analysis
- **Prompt**: Uses `AiPrompts.systemPromptForMode(scanMode)` + `AiPrompts.buildPrompt(context)`
- **Context**: Includes scope (word/phrase/page), surrounding text, user CEFR level, scan mode
- **Scan modes** (v0.8.0): STANDARD, INTERPRETER, MEDICAL, LEGAL — each has a specialized system prompt

### 3. Chat Tutor (`GeminiChatClient`)
- **What**: Multi-turn English tutor dialog seeded with scanned text context
- **Why**: Allows follow-up questions ("What does this idiom mean?", "Translate to Japanese")
- **System prompt**: CEFR-adapted persona + scan mode overlay via `buildCefrSystemPrompt(level, persona, scanMode)`
- **Context**: Full conversation history passed as message pairs
- **Scan mode integration** (v0.8.0): Chat adapts to active mode (e.g., Medical mode prioritizes clinical terms)

---

## Gemini Live Agent Candidates

Features suitable for a standalone Gemini Live agent (voice + camera):

| Feature | Priority | Rationale |
|---------|----------|-----------|
| **Voice reading tutor** | High | Read text aloud, discuss content via voice — natural Live API fit |
| **OCR + dialog** | High | Camera sees text, AI explains via voice — already have the pipeline |
| **Pronunciation coach** | Medium | User reads aloud, AI provides feedback — Live API bidirectional audio |
| **Vocabulary quiz** | Medium | AI asks about words from scanned text — voice Q&A natural |
| **Reading companion** | High | Adapted from BookSage Live patterns — cross-page memory + personas |

### Prototype Status (2026-09-04)

`GeminiLiveClient` (`data/ai/GeminiLiveClient.kt`) implements the BidiGenerateContent WebSocket
protocol layer, verified against the current `ai.google.dev/gemini-api/docs/live-api` pages
(the ADK/`@google/genai` notes above predate this check and were superseded — that SDK is
JS-only, so EigoSage talks the WebSocket protocol directly via Ktor, matching how the three
REST clients already work):

- Endpoint: `wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent`
- Model: `gemini-3.1-flash-live-preview` (current Live model name as of this check)
- Input audio: 16-bit PCM, 16kHz, little-endian, mimeType `audio/pcm;rate=16000`
- Output audio: 16-bit PCM, 24kHz, little-endian, mimeType `audio/pcm;rate=24000`
- `ChatPersona.liveVoiceName` maps Sage/Lexicon/Tutor to Charon/Kore/Puck for `speechConfig.voiceConfig.prebuiltVoiceConfig.voiceName`
- 13 unit tests cover message building (setup/audio chunk/text turn) and server-event parsing (text delta, audio delta, turnComplete, interrupted, setupComplete, malformed input) — `app/src/test/java/com/jworks/eigosage/data/ai/GeminiLiveClientTest.kt`
- Ktor's Android engine has no WebSocket support, so this client owns a separate OkHttp-based `HttpClient`, isolated from the shared REST client in `AiModule`

**Not done yet** (needs a physical device, can't be verified from this session): microphone
capture via `AudioRecord`, audio playback via `AudioTrack`, `RECORD_AUDIO` permission, and
Hilt/UI wiring to an actual chat/voice screen. The client is unwired — nothing calls `connect()`
outside its own tests.

### Persona System (adapted from BookSage Live)

| Persona | Role | Voice (proposed) |
|---------|------|-------------------|
| **Lexicon** | Vocabulary focus — definitions, synonyms, etymology | Kore |
| **Sage** | Comprehension — reading level, main ideas, context | Charon |
| **Tutor** | Practice — grammar drills, translation exercises | Puck |

---

## Prompt Iterations

### v1 (v0.5.1) — Initial

**GeminiChatClient system prompt:**
> You are an English language tutor helping a user understand text they captured with EigoSage (an English reading assistant app). Be concise, helpful, and friendly. Use simple English when possible. If asked to translate, provide the translation along with brief notes on nuance. Format responses with markdown bold for key terms and bullet points for lists.

**Observations:**
- Works well for basic Q&A about scanned text
- No CEFR-level adaptation (same response regardless of user level)
- No proactive vocabulary flagging
- No persona differentiation

### v2 (v0.6.0) — Current Production

**Changes implemented:**
- ✅ CEFR-adapted system prompt via `GeminiChatClient.buildCefrSystemPrompt(cefrLevel)` — 6 distinct level-specific instruction sets (A1→C2)
- ✅ Readability score (Flesch-Kincaid grade, Flesch RE, difficulty) included in chat context seed
- ✅ User's CEFR level passed in seed message for grounded responses
- ✅ `systemPrompt` parameter on `send()` — system prompt stored in `PanelState.Chat` and reused across the session

**Observations:**
- CEFR adaptation untested end-to-end (needs device testing across all 6 levels)
- No persona differentiation yet
- No proactive vocabulary flagging yet

### v3 (v0.7.0) — Current Production

**Changes implemented:**
- ✅ Smart follow-up suggestion chips — AI generates 3 CEFR-adapted suggestions via `[SUGGESTIONS]` block, parsed by `GeminiChatClient.parseSuggestions()`
- ✅ Auto-bookmark words discussed in chat — bold terms (`**word**`) extracted via regex, save chip in ChatPanel
- ✅ Chat persistence — sessions saved to Room DB, resume from History Chats tab
- ✅ Chat export — share as text or PDF via `ChatExporter`
- ✅ Persona mode — Sage (comprehension), Lexicon (vocabulary), Tutor (practice) with distinct system prompts per persona, CEFR-adapted. Persona selector chip in ChatPanel header, locked once chat starts.

**Planned v4 improvements:**
- Proactive flagging of difficult words based on user's level

---

## Comparison Log

| Feature | Claude (dev) | Gemini (in-app) | Notes |
|---------|-------------|-----------------|-------|
| OCR accuracy | N/A (not in-app) | Good for printed, weak on handwriting | ML Kit handles fast path |
| Text analysis | Gold standard prompts | v1 system prompt | Need structured comparison |
| Chat quality | N/A | Adequate for basic Q&A | Needs CEFR adaptation |

---

## ADK Patterns Learned

1. **ADK v0.5.0 does NOT have `runLive()`** — use `@google/genai` directly for Live API WebSocket (from PianoQuest Live, BookSage Live)
2. **FunctionTool** — wraps callable functions with Zod schemas for type-safe tool declarations
3. **LlmAgent** — agent registration with tools, system instructions, health metadata
4. **AutoFlow routing** — BookSage Live is migrating to this for persona sub-agent routing (watch for results)
5. **Context seeding** — inject scanned text + readability metrics at session start for grounded responses
6. **Periodic context injection** — refresh AI memory every N messages with updated scan context (BookSage pattern)

## Cross-Project References

- **SheetMusicReader** (jworks:104): Dual output pattern — Claude gold standard vs Gemini automated
- **BookSage Live** (jworks:102): Cross-page memory, persona system, ADK multi-agent with AutoFlow
- **KanjiSage** (jworks:43): Sibling app — same camera+OCR architecture for Japanese

---

## TODOs

- [x] Add CEFR level to system prompt for difficulty-appropriate responses (v0.6.0)
- [x] Add readability score to chat context seed (v0.6.0)
- [ ] Test CEFR-adapted system prompts (v2) — compare response quality across levels
- [x] Smart follow-up suggestion chips (CEFR-adapted) (v0.7.0)
- [x] Auto-bookmark words discussed in chat (v0.7.0)
- [x] Implement persona mode in GeminiChatClient (Lexicon/Sage/Tutor) (v0.7.0)
- [ ] Add proactive vocabulary flagging based on user's CEFR level
- [x] Create Gemini Live agent prototype — protocol layer only (2026-09-04, see below)
- [x] Explore Live API WebSocket integration — went with a native Ktor/OkHttp WebSocket client instead of `@google/genai` (that SDK is JS-only; EigoSage is Kotlin), see below
- [ ] Wire `GeminiLiveClient` to microphone capture (AudioRecord, 16kHz PCM) and audio playback (AudioTrack, 24kHz PCM) — needs a device, RECORD_AUDIO permission, and DI/UI wiring
- [ ] Confirm `Charon`/`Puck` voice names against the current TTS voices list before shipping (only `Kore` verified against live docs so far)
- [ ] Benchmark Gemini 2.5 Flash vs Pro for text analysis quality
- [ ] Adapt BookSage Live's cross-page memory pattern for multi-scan sessions
- [ ] Track BookSage's ADK AutoFlow migration results for persona routing
- [ ] Set up dual output comparison: test same prompts on Claude API vs Gemini API
