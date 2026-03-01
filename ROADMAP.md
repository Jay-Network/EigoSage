# EigoSage Roadmap

**Current version**: v0.5.1 (Alpha)
**Last updated**: 2026-03-01

---

## Alpha (v0.x.x) — Feature Completeness

### v0.6.0 — Spaced Repetition & Study Mode
- **SRS engine**: Leitner box or SM-2 algorithm for difficult words
- **Study screen**: Flashcard UI with IPA, CEFR badge, definition, example sentence
- **Session stats**: Words reviewed, accuracy, streak
- **Auto-populate**: Difficult words from captures auto-feed into SRS deck
- **EigoJourney export**: Send word decks to EigoJourney for gamified mastery
  - **DEPENDS ON**: EigoJourney (jworks:45) import API

### v0.7.0 — Chat Intelligence & Context Memory
- **Cross-session chat context**: Persist chat history per captured text (Room)
- **Chat follow-up from history**: Re-open chat for previously captured texts
- **Smart suggestions**: AI-generated follow-up questions based on user's CEFR level
- **Vocabulary extraction from chat**: Auto-bookmark words discussed in chat
- **Chat export**: Share conversation as text/PDF

### v0.8.0 — Professional Scanning Modes
- **Document mode**: Multi-page capture with page navigation
- **Medical English mode**: Medical terminology dictionary overlay (ICD codes, anatomical terms)
- **Business English mode**: Conference/meeting vocabulary (formal register, idioms)
- **Batch scan**: Queue multiple captures for background AI analysis
- **PDF import**: Analyze PDF documents directly

### v0.9.0 — Polish & Beta Prep
- **Crash safety hardening**: Null guards, coroutine error boundaries, ANR prevention
- **Light theme**: Full light mode support
- **Accessibility**: TalkBack support, content descriptions, minimum touch targets
- **Performance profiling**: Startup time, memory, battery optimization
- **Onboarding flow**: First-launch tutorial (tap, circle, chat)
- **Fix all known bugs**: First-capture 0-words, Google Sign-In
- **Test coverage**: >60% unit test coverage on business logic
- **Supabase SDK update**: 2.1.5 → latest

---

## Beta (v1.x.x) — User Testing & Refinement

### v1.0.0 — Beta Launch
- **Stage transition**: Alpha → Beta
- **Play Store**: Internal testing → Closed beta track
- **Analytics**: Basic usage telemetry (opt-in) for crash-free rate, feature usage
- **Rate limiting**: Graceful handling of API rate limits and quota exhaustion
- **Offline AI fallback**: Cached responses, queue analysis for when online
- **Deep linking**: Share captured text analysis via link

### v1.1.0 — Interpreter Tools
- **Conference prep mode**: Import agenda/slides, pre-scan terminology, build vocab deck
- **Parallel text view**: Side-by-side EN/JP for scanned text (leveraging JDialogs expertise)
- **Terminology glossary**: User-built glossaries per domain (medical, legal, business)
- **Quick reference**: Pin frequently-needed terms to floating overlay
  - **DEPENDS ON**: Translation API or local model for JP output

### v1.2.0 — Social & Collaboration
- **Share word lists**: Export/import decks between users
- **Class mode**: Teacher creates a scan, students see same CEFR analysis
- **Leaderboard integration**: J Coin earnings visible in EigoJourney
  - **DEPENDS ON**: J Coin backend (jworks:35), EigoJourney (jworks:45)

### v1.3.0 — Advanced AI
- **Text-to-speech**: Pronunciation playback for words and sentences
- **Grammar diagram**: Visual sentence structure breakdown (subject, predicate, clauses)
- **Reading comprehension questions**: Auto-generated quiz from scanned text
- **Adaptive difficulty**: AI adjusts explanation complexity based on user's CEFR history

---

## Store Release (v2.x.x) — Production

### v2.0.0 — Production Launch
- **Stage transition**: Beta → Store release (public)
- **Play Store**: Open production track
- **Crash-free rate**: >99.5%
- **App Store optimization**: Screenshots, feature graphic, localized listing (EN/JP)
- **Privacy policy**: Final review, GDPR compliance
- **Professional app icon**: Designer-polished
- **Monetization**: Freemium model (core free, Pro for unlimited AI + professional modes)

### v2.1.0+ — Post-Launch
- **Widgets**: Home screen widget for word-of-the-day from SRS deck
- **Wear OS companion**: Quick word lookup on smartwatch
- **Tablet optimization**: Two-pane layout for large screens
- **Multi-language OCR**: Expand beyond English (French, Spanish, German)
- **API for schools**: B2B licensing for language school integration
  - **DEPENDS ON**: Backend infrastructure (jworks:56)

---

## Cross-App Dependencies

| Feature | Depends On | App/Agent |
|---------|-----------|-----------|
| EigoJourney word export | Import API ready | EigoJourney (jworks:45) |
| J Coin leaderboard | J Coin backend sync | J Coin (jworks:35) |
| Class mode sharing | Shared backend API | Backend (jworks:56) |
| B2B school API | Backend infrastructure | Backend (jworks:56) |
| iPad feature parity | Ongoing sync | iPad (jworks:61) |

---

## Sage Brand Vision

EigoSage embodies **scan → understand → dialog → mastery**:
- **Alpha**: Build the scanning + AI dialog foundation (DONE)
- **Beta**: Add professional interpreter tools + social features
- **Production**: Scale to B2B (schools, interpretation firms, corporations)

The "Sage" identity = an AI-powered wise companion that helps you deeply understand any English text through intelligent conversation, not just surface-level definitions.
