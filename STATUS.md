# EigoSage Cross-Platform Development Tracker

> **Shared status file** — updated by both jworks:46 (Android) and jworks:61 (iPad).
> Each agent updates its own column. Check this file before starting work to stay in sync.

**Last updated**: 2026-03-01

---

## Feature Parity Matrix

| Feature | Android (jworks:46) | iPad (jworks:61) |
|---------|:-------------------:|:-----------------:|
| **Camera capture** | DONE | DONE |
| **Gallery import** | DONE | DONE |
| **ML Kit / Vision OCR** | DONE | DONE |
| **Gemini OCR correction** | DONE | DONE |
| **Interactive image viewer (zoom/pan)** | DONE | DONE |
| **TAP mode (word lookup)** | DONE | DONE |
| **Long-press (sentence AI)** | DONE | DONE |
| **CIRCLE/SELECT mode (rectangle)** | DONE | DONE |
| **WordNet dictionary (offline)** | DONE | DONE |
| **AI integration (Claude)** | DONE | DONE |
| **AI integration (Gemini)** | DONE | DONE |
| **Definition panel** | DONE | DONE |
| **AI Analysis panel (4 tabs)** | DONE | DONE |
| **Portrait bottom panel** | DONE | DONE |
| **Landscape right panel** | DONE | DONE |
| **Panel drag-to-resize** | DONE | DONE |
| **Readability scoring (local)** | DONE | DONE |
| **Bookmark toggle** | DONE | DONE |
| **Lookup history** | DONE | DONE |
| **History screen (Recent + Saved)** | DONE | DONE |
| **Settings (API key config)** | DONE | DONE |
| **Sign-in (Email/Password)** | DONE | DONE |
| **Sign-in (Google)** | DONE | - |
| **Guest mode** | DONE | DONE |
| **Session persistence (Keychain)** | DONE | DONE |
| **Sign out** | DONE | DONE |
| **Token usage tracking (Settings)** | DONE | DONE |
| **Lasso selection persistence** | DONE | DONE |
| **Lasso toggle in AI panel** | DONE | DONE |
| **Feedback system** | DONE | DONE |
| **Dark mode** | DONE | DONE |
| **J Coin integration** | DONE | - |
| **J Coin Spend Store (rewards)** | DONE | - |
| **Send to EigoJourney (cross-app)** | DONE | - |
| **In-app Gemini chat** | DONE | - |
| **Word enrichment (IPA/CEFR)** | DONE | DONE |
| **CEFR difficulty filter** | DONE | DONE |
| **Difficult words panel** | DONE | DONE |
| **Glass UI (translucent cards)** | DONE | DONE |
| **Crash safety hardening** | - | DONE |

**Legend**: DONE | SCAFFOLDED (code written, not yet compiled) | IN PROGRESS | - (not started) | N/A

---

## Current Sprint

### Android (jworks:46)
- **Version**: v0.5.1
- **Current work**: Renamed EigoLens → EigoSage, in-app Gemini chat, IPA/CEFR overlay
- **Next**: Phase D (spaced repetition, study mode)
- **Blockers**: None

### iPad (jworks:61)
- **Version**: 0.3.0 (Glass UI + Word Enrichment)
- **Bundle ID**: com.jworks.eigosage (App Store Connect registered)
- **Current work**: Feature parity port — Glass UI, CEFR/IPA, difficult words, feedback all DONE.
- **Next**: J Coin integration, Google Sign-In, Send to EigoQuest
- **Blockers**: None (GitHub secrets set, CI/CD working, TestFlight deployed)

---

## Architecture Decisions (Cross-Platform)

| Decision | Android | iPad |
|----------|---------|------|
| OCR engine | ML Kit Text Recognition | Apple Vision (VNRecognizeTextRequest) |
| Dictionary | Bundled WordNet SQLite | Bundled WordNet SQLite (same DB) |
| AI networking | OkHttp + manual JSON | URLSession + Codable |
| Local DB | Room (eigosage_user.db) | SwiftData / Core Data |
| DI | Hilt | Swift environment / manual |
| UI framework | Jetpack Compose | SwiftUI |
| Min target | Android 7.0 (API 24) | iPadOS 16+ |

---

## Shared Assets

- WordNet SQLite database (can share the same .db file)
- AI prompt templates (same prompts, different language wrappers)
- Readability formulas (same math: Flesch-Kincaid, SMOG, Coleman-Liau)
- App icon / branding (shared design, platform-specific assets)

---

## Update Protocol

1. When you complete a feature, update the parity matrix above
2. When starting new work, update the "Current Sprint" section
3. If you make an architecture decision that affects the other platform, note it in "Architecture Decisions"
4. Prefix your commit with the platform: `[Android]` or `[iPad]`
