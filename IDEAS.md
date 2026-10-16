# EigoSage Ideas

## Active

- **Deep-link URL scheme** — `eigosage://vocab/{word}` to open app and trigger word lookup. Enables Hermes digest CTAs linking EN news items to EigoSage vocab. Blocked: awaiting Jay's greenlight on coordinated cross-app funnel (KanjiSage/KanjiJourney/EigoSage/JayBoard uniform path convention). jworks-vm:42 owns coordination.
- **Proactive vocabulary flagging** — Flag difficult words based on user's CEFR level before they ask. Auto-highlight words above threshold in scanned text.
- **Gemini Live voice reading tutor** — Voice + camera agent: read text aloud, discuss content via voice. Natural fit for Live API bidirectional audio. Persona system (Lexicon/Sage/Tutor) already designed.
- **Pronunciation coach** — User reads aloud, AI provides feedback via Gemini Live API. Gemini TTS evaluated 2026-09-06 (REST `v1beta/interactions`, 30 voices, style-tag pacing only, no phoneme control); a one-shot "hear this word slowly" playback is the small first step. Parked 2026-10-16 by the app owner: no user demand yet. If revived, ask jdialogs:0 who owns the S-181 TTS pipeline before writing a new client.
- **Multi-scan session memory** — Adapt BookSage Live's cross-page memory pattern for multi-scan sessions (remember context across multiple camera captures).
- **Dual-output comparison** — Test same prompts on Claude API vs Gemini API for text analysis quality benchmarking.

## Completed

(None yet)
