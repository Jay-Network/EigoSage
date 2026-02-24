# EigoLens - Google Play Store Listing

## App Name
EigoLens

## Short Description (80 characters max)
Tap words for definitions. Circle phrases for AI analysis. Offline + AI powered.

(80 characters)

## Full Description (4000 characters max)

EigoLens turns your camera into a powerful English reading assistant. Capture any text, tap a word, and get instant definitions - all offline, all on-device.

CORE FEATURES

Tap Any Word for Instant Definitions
Capture text with your camera, then simply tap any word to look it up. No menus, no typing - just tap. EigoLens includes a comprehensive offline dictionary with over 147,000 words and 207,000 definitions powered by WordNet. Works without internet.

Circle Phrases for AI-Powered Analysis
Need to understand a phrase or sentence? Switch to circle mode and draw around multiple words. EigoLens uses AI (powered by Gemini or Claude) to explain meaning, grammar, vocabulary, and usage in context. Circle 2-8 words for phrase analysis, or 9+ words for paragraph-level summaries with key ideas and tone analysis. After selection, you're automatically back in tap mode.

Full-Screen Image with Overlay Panel
Your captured text stays full-screen while the definition panel slides up from the bottom. Drag the panel to resize it. Pinch to zoom and pan the image with two fingers - even while in tap mode.

Smart Word Analysis
Every word lookup includes:
- Multiple definitions organized by part of speech (noun, verb, adjective, etc.)
- Synonyms and antonyms
- Word frequency indicators (Top 100, Common, Moderate, Rare)
- Automatic lemmatization - inflected words like "running", "went", or "mice" resolve to their base forms

Reading Level Analysis
Understand how difficult a text is with four standard readability formulas:
- Flesch-Kincaid Grade Level
- Flesch Reading Ease Score
- SMOG Index
- Coleman-Liau Index

Each text gets a difficulty rating from Very Easy to Very Difficult, with the equivalent U.S. grade level and target audience.

Natural Language Processing
EigoLens includes a built-in NLP pipeline that identifies:
- Parts of speech (nouns, verbs, adjectives, adverbs, and more)
- Named entities (dates, numbers, proper nouns)
- Root word forms through lemmatization

WHO IS EIGOLENS FOR?

- ESL/EFL students building vocabulary while reading
- Language learners understanding real-world English text
- Students preparing for standardized English tests (TOEFL, IELTS, SAT)
- Teachers assessing text difficulty for their students
- Anyone encountering unfamiliar English words in daily life
- Parents helping children with English reading

AI-Enhanced OCR
EigoLens can use Gemini Vision to double-check and correct OCR results in the background, improving accuracy on handwritten or difficult text. Your API key stays encrypted on your device.

KEY HIGHLIGHTS

- Works offline: Dictionary and NLP features work without internet
- AI-powered: Optional AI analysis for phrases and paragraphs (bring your own API key)
- Fast: OCR processing in under 200ms, word lookup in under 30ms
- Privacy-focused: API keys stored encrypted on-device, no data shared with third parties
- Natural interaction: Tap words, pinch to zoom, drag to resize - no learning curve
- Free to use: Core features available without an account, AI features with free Gemini API key

EigoLens is developed by JWorks as part of our language learning toolkit.

---

## App Category
Primary: Education
Sub-category: Language Learning (if available, otherwise Education)

## Tags/Keywords
english dictionary, text scanner, OCR reader, vocabulary builder, reading level, ESL, English learner, word definitions, text analysis, readability, TOEFL, IELTS

## Privacy Policy URL
https://jworks-ai.com/privacy/eigolens

## Contact Details
- Developer name: JWorks
- Contact email: jay@jworks-ai.com
- Website: https://jworks-ai.com

---

## Content Rating Questionnaire (IARC)

### Violence
- Does the app contain violence? **No**
- Does the app depict violence against characters? **No**
- Does the app contain graphic violence? **No**

### Sexual Content
- Does the app contain sexual content or nudity? **No**
- Does the app contain sexual themes? **No**

### Language
- Does the app contain profanity or crude humor? **No**
- Note: The dictionary contains standard English words which may include some that could be considered mature vocabulary, but these are presented in an educational/dictionary context only.

### Controlled Substances
- Does the app reference or depict drug use, alcohol, or tobacco? **No**

### Gambling
- Does the app contain gambling or simulated gambling? **No**

### User-Generated Content
- Does the app allow users to communicate or share content with each other? **No**
- Users can submit feedback to developers only (not visible to other users).

### Personal Information
- Does the app collect personal information? **Limited**
- Optional email for account creation (Supabase auth)
- Optional email for feedback submission
- No data is shared with third parties
- Camera is used only for text recognition (images are not stored or transmitted)

### Ads
- Does the app contain ads? **No**

### Recommended Rating: **Everyone (E)** or **PEGI 3**
- Educational content only
- No objectionable material
- Dictionary definitions are presented in academic context

---

## Target Audience & Content Settings

### Target Age Group
- **Primary**: 13 and older
- Note: App does NOT target children under 13. Not a "Designed for Families" app.
- Reason: Requires camera permission and optional account creation.

### Content Settings
- App is suitable for all audiences
- No mature content
- Educational purpose

---

## Screenshot Descriptions

Recommended 8 phone screenshots showing:

1. **Camera Preview** - Clean camera interface with capture button
2. **Text Capture** - Captured text with OCR bounding boxes showing detected words
3. **Tap Word** - User tapping a word, blue highlight pulse on the word
4. **Definition Panel** - Overlay panel with word definition, POS tags, synonyms, frequency badge
5. **Circle Selection** - Circle mode active, user drawing lasso around multiple words
6. **Readability Panel** - Reading level scores with difficulty grade and statistics
7. **Gallery Import** - Importing a photo from gallery for text analysis
8. **Login Screen** - Clean sign-in with Google, email, and guest options

## Feature Graphic (1024x500)
- App name "EigoLens" prominently displayed
- Visual showing camera viewfinder + text overlay + definition popup
- Color scheme: Dark background with blue (#2196F3) accents matching app theme
- Tagline: "Tap. Define. Learn."

---

## Data Safety Section (Google Play Console)

### Data Collection Overview

| Data Type | Collected | Shared | Purpose |
|-----------|-----------|--------|---------|
| Email address | Optional | No | Account creation, feedback |
| Auth tokens | Yes (if signed in) | No | Session management |
| Feedback text | Optional | No | User-submitted feedback to developer |
| Device info | Yes (with feedback) | No | Bug diagnosis (OS version, device model) |
| App settings | Yes | No | User preferences |
| FCM token | Future | No | Push notifications |

### Data NOT Collected
- Camera images (processed on-device only, never transmitted except optional Gemini Vision OCR)
- Dictionary lookup history (in-memory cache only, cleared on app close)
- Location data
- Contacts
- Files (beyond user-selected gallery photos)

### AI Provider Data
- When AI analysis is enabled, selected text is sent to Claude (Anthropic) or Gemini (Google) APIs
- API keys are stored encrypted on-device using Android Keystore
- No API keys or text are sent to JWorks servers
- AI features are opt-in (require user to enter their own API key)

### Detailed Answers for Play Console Form

**Is any of the collected data encrypted in transit?**
Yes - all network communication uses HTTPS via Supabase/Ktor.

**Can users request that their data be deleted?**
Yes - users can sign out (clears session) and contact developer for account deletion.

**Is this data processed ephemerally?**
Camera images: Yes - processed for OCR and discarded (not stored).
Dictionary lookups: Yes - LRU cache in memory, cleared on app close.

**Security practices:**
- Data encrypted in transit (HTTPS)
- Auth sessions stored in encrypted SharedPreferences
- No third-party analytics or tracking SDKs
- No ad SDKs

---

## Release Notes (v0.3.0 - Phase B: AI Integration)

### What's New
AI-powered analysis for phrases and paragraphs!

- Circle 2+ words for AI phrase/paragraph analysis (Claude or Gemini)
- Scope-aware routing: single words use offline dictionary, multi-word uses AI
- Encrypted API key storage for Claude and Gemini
- Background Gemini Vision OCR correction for improved accuracy
- Contextual word insights alongside definitions
- Settings screen with AI provider configuration
- ESL-optimized AI prompts for clear, learner-friendly explanations

### Previous (v0.2.0 - Phase A)
- Tap any word on captured text for instant offline definitions
- Full-screen image with draggable overlay panel (replaces split view)
- Two-finger zoom/pan works alongside word tapping
- Circle mode for multi-word selection (toggle via FAB)
- Pulsing blue highlight on tapped words
- Landscape support: panel anchors to right side

### Previous (v0.1.0 - Internal Testing)
- Camera text capture with ML Kit OCR
- Offline dictionary with 147,000+ words (WordNet)
- Reading level analysis (Flesch-Kincaid, SMOG, Coleman-Liau)
- NLP analysis (parts of speech, named entities, lemmatization)
- Gallery photo import
- Google Sign-In and email authentication
- In-app feedback system
