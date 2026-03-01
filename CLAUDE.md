# EigoSage - Claude Code Agent Instructions

**Project**: EigoSage - English Text Analyzer with OCR
**Owner**: JWorks (Jay's Technology & Manufacturing Business)
**Created**: 2026-02-08
**Status**: Initial Setup

---

## Project Overview

EigoSage is a camera-based English text analysis app that provides real-time word definitions, grammar analysis, and reading comprehension assistance. It's the English counterpart to KanjiLens, targeting ESL students and language learners.

**Core Value**: Point your camera at any English text and get instant definitions, synonyms, grammar explanations, and reading level analysis.

---

## Key Documents

**Development Resources**:
1. `/home/takuma/1_jworks/A_ai/4_Apps/EigoLens/README.md` - Project overview and status (if exists)
2. Source code: `/home/takuma/Data_ubuntu/GitHub/Jay-Network/apps/EigoLens/`
3. **Reference**: KanjiLens CLAUDE.md for similar camera + OCR architecture

---

## Development Phases

### Current Phase: Setup & Planning
**Goal**: Define MVP scope and establish project structure

**Key Questions to Address**:
- Primary use case (definitions, grammar, reading level, all three?)
- Target audience (ESL students, native speakers, academics?)
- Offline vs online dictionary (bundled vs API?)
- Grammar analysis depth (basic vs advanced?)
- Reading comprehension features (summaries, questions?)

---

## Technology Stack

| Component | Technology | Notes |
|-----------|-----------|-------|
| Language | Kotlin | Modern Android standard |
| UI | Jetpack Compose | Declarative UI |
| Camera | CameraX | Lifecycle-aware |
| OCR | ML Kit (Text Recognition) | Offline-first |
| Dictionary | WordNet / Oxford API | Local + cloud fallback |
| Database | Room + SQLite | Word cache and history |
| Backend | Node.js (optional) | Advanced grammar analysis |
| DI | Hilt | Dependency injection |

---

## Directory Structure

### Agent Workspace (This Directory)
**Location**: `/home/takuma/1_jworks/A_ai/4_Apps/EigoLens/`

```
~/1_jworks/A_ai/4_Apps/EigoLens/
├── README.md              # Project overview
├── CLAUDE.md              # This file (agent instructions)
├── STATUS.md              # Project status tracker
├── docs/                  # Documentation
└── planning/              # Project planning
```

### Android App Project (GitHub Repository)
**Location**: `/home/takuma/Data_ubuntu/GitHub/Jay-Network/apps/EigoLens/`

```
~/Data_ubuntu/GitHub/Jay-Network/apps/EigoLens/
├── app/                   # Main Android module
│   ├── src/main/
│   │   ├── java/com/jworks/eigosage/
│   │   │   ├── MainActivity.kt
│   │   │   ├── domain/    # Business logic
│   │   │   ├── data/      # Data layer
│   │   │   └── ui/        # UI components
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts       # Root build file
└── settings.gradle.kts    # Gradle settings
```

**IMPORTANT**: When working on Android code, change to the GitHub directory:
```bash
cd ~/Data_ubuntu/GitHub/Jay-Network/apps/EigoLens/
```

---

## Development Guidelines

### Code Quality Standards

1. **Kotlin Best Practices**:
   - Use data classes for models
   - Leverage sealed classes for states
   - Use coroutines for async operations
   - Prefer immutability

2. **Jetpack Compose**:
   - Keep composables small and focused
   - Use ViewModels for state management
   - Follow unidirectional data flow
   - Extract reusable components

3. **Camera + OCR Architecture**:
   - **Reference KanjiLens** for camera preview and OCR integration
   - Optimize OCR processing for English (simpler than Japanese)
   - Handle text overlay with coordinate mapping
   - Support both printed and handwritten text recognition

4. **Testing**:
   - Unit tests for business logic (80%+ coverage)
   - Integration tests for critical paths
   - UI tests for key user flows
   - Manual testing on real devices

### Git Workflow

1. Work on feature branches
2. Commit frequently with clear messages
3. Use conventional commit format:
   - `feat: Add word definition overlay`
   - `fix: Fix OCR accuracy bug`
   - `docs: Update README`
   - `test: Add camera tests`

---

## Versioning (MANDATORY)

Format: vMAJOR.MINOR.PATCH
- PATCH (last): Every deployed change
- MINOR (middle): Major feature addition (resets PATCH)
- MAJOR (first): Release stage transition (resets MINOR+PATCH)
  - v0.x.x = Alpha
  - v1.x.x = Beta
  - v2.x.x = Store release
Full spec: ~/1_jworks/A_ai/4_Apps/VERSIONING.md

## App Family Naming

| Suffix | Meaning | Apps |
|--------|---------|------|
| **Sage** | Scan + AI dialog | KanjiSage, EigoSage |
| **Journey** | Mastery + gamification | KanjiJourney, EigoJourney |

---

## Performance Targets

| Metric | Target | Acceptable |
|--------|--------|------------|
| OCR Processing | 50-100ms | 200ms |
| Dictionary Lookup (local) | 10-30ms | 50ms |
| Canvas Render | 8-16ms | 32ms |
| Total E2E Latency | 100-200ms | 300ms |
| Battery Drain | 15-20%/hr | 25%/hr |

---

## Sub-Agent Information

**Identity**: [Claude-JWorks | EigoSage-Dev] (jworks:46)
**Launcher**: `claude-jworks-eigosage`
**Working Directory**: `/home/takuma/1_jworks/A_ai/4_Apps/EigoLens/`
**tmux Window**: jworks:46

**Purpose**: Dedicated Android development agent for EigoSage project

**Parent Agent**: Window 42 (Apps Division) - `jworks:42`

---

## Business Context

### Target Market

- **Primary**: ESL students (intermediate to advanced)
- **Secondary**: Native speakers (vocabulary expansion)
- **Future**: Academic researchers, language teachers

### Strategic Value

EigoSage is part of JWorks' **language learning app portfolio**:
1. Complements EigoJourney (mastery + gamification)
2. Shares tech foundation with KanjiSage (camera + OCR architecture)
3. Potential B2B licensing to language schools and universities
4. Integration with TutoringJay English curriculum
5. Data insights for reading comprehension strategies

---

## Related Projects

**Sibling Apps**:
- **KanjiLens** (jworks:43) - Japanese reading assistant (same architecture!)
- **KanjiQuest** (jworks:44) - Japanese kanji learning (gamified)
- **VocabQuest** (jworks:45) - English vocabulary learning (gamified)

**Technology Sharing**:
- Camera + OCR architecture (shared with KanjiLens)
- Text overlay rendering (coordinate mapping)
- Offline dictionary architecture
- TutoringJay curriculum integration

---

## Feature Ideas (Future)

1. **Reading Level Analysis**: Flesch-Kincaid grade level for scanned text
2. **Grammar Explanations**: Identify parts of speech, sentence structure
3. **Synonym Suggestions**: Context-aware vocabulary alternatives
4. **Translation Mode**: Real-time translation overlay (ESL support)
5. **Study Mode**: Save words to VocabQuest for spaced repetition
6. **Text-to-Speech**: Pronunciation assistance
7. **Comprehension Questions**: Auto-generate reading comprehension quizzes

---

## Next Steps

1. [ ] Define MVP scope and feature set
2. [ ] Set up Android Studio project
3. [ ] Study KanjiLens camera + OCR implementation
4. [ ] Implement CameraX preview
5. [ ] Integrate ML Kit English OCR
6. [ ] Create word definition overlay
7. [ ] Add offline dictionary (WordNet)

---

## Resources

- **ML Kit Text Recognition**: https://developers.google.com/ml-kit/vision/text-recognition
- **CameraX Docs**: https://developer.android.com/training/camerax
- **Jetpack Compose**: https://developer.android.com/jetpack/compose
- **WordNet**: https://wordnet.princeton.edu/
- **Oxford Dictionaries API**: https://developer.oxforddictionaries.com/
- **KanjiLens CLAUDE.md**: Reference for camera + OCR architecture

---

**Last Updated**: 2026-02-08
**Status**: Initial setup - ready for planning
**Next Milestone**: Define MVP scope and study KanjiLens architecture
