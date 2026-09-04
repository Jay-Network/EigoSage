package com.jworks.eigosage.data.ai

/**
 * Chat personas for GeminiChatClient. Each persona has a distinct teaching focus
 * and system prompt style that adapts to the user's CEFR level.
 */
enum class ChatPersona(
    val displayName: String,
    val shortDescription: String,
    // Gemini Live prebuiltVoiceConfig.voiceName for this persona. Only "Kore" is confirmed
    // against current Gemini docs (2026-09-04); Charon/Puck carried over from earlier research
    // in docs/gemini-development-log.md — re-check the TTS voices list before device wiring.
    val liveVoiceName: String
) {
    SAGE(
        displayName = "Sage",
        shortDescription = "Comprehension & context",
        liveVoiceName = "Charon"
    ),
    LEXICON(
        displayName = "Lexicon",
        shortDescription = "Vocabulary & definitions",
        liveVoiceName = "Kore"
    ),
    TUTOR(
        displayName = "Tutor",
        shortDescription = "Grammar & practice",
        liveVoiceName = "Puck"
    );

    companion object {
        val DEFAULT = SAGE
    }
}
