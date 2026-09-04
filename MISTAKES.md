# EigoSage Mistakes

## Lessons Learned

- **S-358: Gemini thinking tokens can silently truncate output** (2026-06-01) — When upgrading to Gemini 3.5 Flash (which enables thinking by default), output was being silently truncated. Fix: explicitly set `thinkingBudget: 0` on all REST API call sites. Always check model defaults when upgrading.
- **OcrTextMerger word-to-box misalignment** (pre-v0.8.0) — Early merge strategy tried to align words even when ML Kit and Gemini had different word counts per line. This caused taps to look up the wrong word. Fix: only replace text when word counts match exactly; otherwise keep ML Kit's original text.
- **kotlinx.serialization `JsonArrayBuilder.add()` has no String overload** (2026-09-04) — Unlike `JsonObjectBuilder.put("key", "value")` which accepts raw strings, `putJsonArray("x") { add("literal") }` fails to compile ("actual type String, expected JsonElement"). Must wrap: `add(JsonPrimitive("literal"))`. Caught building GeminiLiveClient's setup message (`responseModalities`).
- **Ktor's Android engine (`ktor-client-android`) has no WebSocket support** — the `WebSockets` plugin requires CIO, Java, or OkHttp. Don't add `install(WebSockets)` to the shared REST `HttpClient` in AiModule; give the Live client its own OkHttp-based client instead.
