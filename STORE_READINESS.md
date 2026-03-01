# EigoSage - Play Store Readiness Checklist

**App**: EigoSage v0.2.0 (versionCode 2)
**Package**: com.jworks.eigosage
**Track**: Internal Testing
**Last Updated**: 2026-02-24

---

## 1. Store Listing

| Item | Status | Notes |
|------|--------|-------|
| App name | DONE | EigoSage |
| Short description (80 char) | DONE | 79 chars - see store-listing.md |
| Full description (4000 char) | DONE | Updated for tap-to-define - see store-listing.md |
| App category | DONE | Education |
| Contact email | DONE | jay@jworks-ai.com |
| Developer website | DONE | https://jworks-ai.com |
| Privacy policy URL | NEEDS UPDATE | Old: jworks-ai.com/privacy/englishlens → Need: /privacy/eigosage |

## 2. Graphics / Assets

| Item | Status | Notes |
|------|--------|-------|
| App icon (adaptive) | DONE | Blue background (#1565C0), white magnifying glass + "A" |
| Phone screenshots (min 2) | PENDING | Need 2-8 screenshots, descriptions in store-listing.md |
| 7-inch tablet screenshots | PENDING | Optional but recommended |
| 10-inch tablet screenshots | PENDING | Optional but recommended |
| Feature graphic (1024x500) | PENDING | Spec in store-listing.md |

## 3. Content Rating (IARC)

| Item | Status | Notes |
|------|--------|-------|
| Violence | DONE | No |
| Sexual content | DONE | No |
| Language / profanity | DONE | No (dictionary is educational context) |
| Controlled substances | DONE | No |
| Gambling | DONE | No |
| User interaction | DONE | No UGC, feedback to dev only |
| Expected rating | DONE | Everyone (E) / PEGI 3 |

## 4. Target Audience & Content

| Item | Status | Notes |
|------|--------|-------|
| Target age group | DONE | 13+ |
| Designed for Families | DONE | No (requires camera, optional account) |
| Contains ads | DONE | No |
| In-app purchases | DONE | No (currently free) |

## 5. Data Safety

| Item | Status | Notes |
|------|--------|-------|
| Data collection documented | DONE | See store-listing.md |
| Data sharing | DONE | No third-party sharing |
| Data encryption | DONE | HTTPS via Supabase/Ktor |
| Data deletion | DONE | Sign out clears session |
| Security practices | DONE | No tracking SDKs, no ad SDKs |

## 6. Technical Readiness

| Item | Status | Notes |
|------|--------|-------|
| Release build compiles | DONE | compileReleaseKotlin passing |
| ProGuard / R8 rules | DONE | kotlinx.serialization, Ktor, Supabase, Firebase, Credentials |
| Signing config | DONE | Release keystore configured, gitignored |
| CI/CD workflow | DONE | GitHub Actions on master, debug+test+release jobs |
| Unit tests passing | DONE | 58/59 passing (1 pre-existing syllable heuristic) |
| Camera permission request | DONE | Accompanist gate with rationale |
| Firebase graceful degradation | DONE | Auto-init disabled, isFirebaseAvailable() guard |
| Session persistence | DONE | SharedPrefsSessionManager |
| Offline graceful degradation | DONE | All Supabase calls guarded, core features on-device |
| Null bitmap handling | DONE | Toast feedback on capture failure |
| Room schema validation | DONE | DB tables rebuilt with correct NOT NULL constraints |

## 7. Remaining Bugs / Polish

| Issue | Severity | Status | Notes |
|-------|----------|--------|-------|
| First capture sometimes 0 words | Medium | INVESTIGATING | Camera warm-up? Second capture works fine |
| Google Sign-In not working | Medium | BLOCKED | Needs Google provider in Supabase dashboard |
| Privacy policy URL needs update | Low | PENDING | Redirect /englishlens to /eigosage (jworks:17) |
| App icon needs designer review | Low | DEFERRED | Vector icon created, may want professional polish |
| Syllable count heuristic | Low | DEFERRED | "university" test expects 4, heuristic returns 5 |
| Light theme support | Low | DEFERRED | App is dark-only, acceptable for v0.2.0 |
| Supabase SDK version 2.1.5 | Low | DEFERRED | Behind current 2.6.x, works for now |

## 8. Release Checklist

### Before Internal Testing
- [x] Build compiles and runs
- [x] ProGuard rules verified
- [x] Camera permission handled
- [x] Offline mode works
- [x] Store listing text written
- [x] Content rating answers ready
- [x] Data safety answers ready
- [x] App icon created
- [x] Privacy policy URL live
- [x] Tap-to-define working
- [x] Overlay panel working
- [x] Room schema validated on device
- [ ] Screenshots captured
- [ ] Feature graphic created
- [ ] New Play Store listing (package changed)

### Before Closed Beta (v0.3.0)
- [ ] AI phrase/sentence analysis (Phase B)
- [ ] Privacy policy URL redirected to /eigosage
- [ ] All screenshots uploaded to Play Console
- [ ] Feature graphic uploaded
- [ ] Content rating questionnaire submitted
- [ ] Data safety form completed
- [ ] Internal testing feedback addressed
- [ ] First-capture 0-words bug fixed
- [ ] Google Sign-In working (Supabase dashboard)
- [ ] google-services.json added (Firebase/FCM)

### Before Production (v1.0.0)
- [ ] Reading context system (Phase D)
- [ ] Full-text AI analysis (Phase C)
- [ ] Closed beta feedback addressed
- [ ] Crash-free rate >99%
- [ ] Light theme support
- [ ] Supabase SDK updated to latest
- [ ] Comprehensive test coverage (>60%)
- [ ] Performance profiling
- [ ] Accessibility audit
- [ ] Professional app icon / feature graphic

---

## Dependencies on Other Agents

| Need | Owner | Status |
|------|-------|--------|
| Privacy policy redirect | jworks:17 | PENDING - /englishlens → /eigosage |
| Screenshots / feature graphic | jayhub:31 (Vision) | Not started |
| Google provider in Supabase | Jay (manual) | Pending dashboard access |
| Firebase google-services.json | Jay (manual) | Pending Firebase project setup |
| New Play Store listing | Jay (manual) | Needed (package changed from englishlens) |
| Git pushes | jworks:42 | On request |

---

*Generated by jworks:46 (EigoSage Dev Agent) - 2026-02-24*
