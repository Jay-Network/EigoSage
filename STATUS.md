# EigoLens Cross-Platform Development Tracker

> **Shared status file** — updated by both jworks:46 (Android) and jworks:61 (iPad).
> Each agent updates its own column. Check this file before starting work to stay in sync.

**Last updated**: 2026-02-25

---

## Feature Parity Matrix

| Feature | Android (jworks:46) | iPad (jworks:61) |
|---------|:-------------------:|:-----------------:|
| **Camera capture** | DONE | SCAFFOLDED |
| **Gallery import** | DONE | SCAFFOLDED |
| **ML Kit / Vision OCR** | DONE | SCAFFOLDED |
| **Gemini OCR correction** | DONE | SCAFFOLDED |
| **Interactive image viewer (zoom/pan)** | DONE | SCAFFOLDED |
| **TAP mode (word lookup)** | DONE | SCAFFOLDED |
| **Long-press (sentence AI)** | DONE | SCAFFOLDED |
| **CIRCLE/SELECT mode (rectangle)** | DONE | SCAFFOLDED |
| **WordNet dictionary (offline)** | DONE | SCAFFOLDED |
| **AI integration (Claude)** | DONE | SCAFFOLDED |
| **AI integration (Gemini)** | DONE | SCAFFOLDED |
| **Definition panel** | DONE | SCAFFOLDED |
| **AI Analysis panel (4 tabs)** | DONE | SCAFFOLDED |
| **Portrait bottom panel** | DONE | SCAFFOLDED |
| **Landscape right panel** | DONE | SCAFFOLDED |
| **Panel drag-to-resize** | DONE | SCAFFOLDED |
| **Readability scoring (local)** | DONE | SCAFFOLDED |
| **Bookmark toggle** | DONE | SCAFFOLDED |
| **Lookup history** | DONE | SCAFFOLDED |
| **History screen (Recent + Saved)** | DONE | SCAFFOLDED |
| **Settings (API key config)** | DONE | SCAFFOLDED |
| **Feedback system** | DONE | - |
| **Dark mode** | DONE | SCAFFOLDED |

**Legend**: DONE | SCAFFOLDED (code written, not yet compiled) | IN PROGRESS | - (not started) | N/A

---

## Current Sprint

### Android (jworks:46)
- **Version**: 0.4.0
- **Current work**: Landscape mode polish, maintenance
- **Next**: Phase D (spaced repetition, study mode)
- **Blockers**: None

### iPad (jworks:61)
- **Version**: 0.1.0 (scaffolding complete)
- **Bundle ID**: com.jworks.eigolens (App Store Connect registered)
- **Current work**: 38 Swift source files across 6 phases created. Needs xcodegen on macOS to generate .xcodeproj, then compile + test.
- **Next**: Build on macOS, test camera + OCR pipeline, validate landscape panel
- **Blockers**: Requires macOS for xcodegen + Xcode build

---

## Architecture Decisions (Cross-Platform)

| Decision | Android | iPad |
|----------|---------|------|
| OCR engine | ML Kit Text Recognition | Apple Vision (VNRecognizeTextRequest) |
| Dictionary | Bundled WordNet SQLite | Bundled WordNet SQLite (same DB) |
| AI networking | OkHttp + manual JSON | URLSession + Codable |
| Local DB | Room (eigolens_user.db) | SwiftData / Core Data |
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
