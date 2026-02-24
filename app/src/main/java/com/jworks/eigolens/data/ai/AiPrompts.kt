package com.jworks.eigolens.data.ai

import com.jworks.eigolens.domain.ai.AnalysisContext
import com.jworks.eigolens.ui.capture.ScopeLevel

object AiPrompts {

    fun buildPrompt(context: AnalysisContext): String = when (context.scopeLevel) {
        is ScopeLevel.Phrase -> phrasePrompt(context)
        is ScopeLevel.Sentence -> sentencePrompt(context)
        is ScopeLevel.Paragraph -> paragraphPrompt(context)
        is ScopeLevel.FullSnapshot -> fullTextPrompt(context)
        is ScopeLevel.Word -> wordPrompt(context)
    }

    private fun phrasePrompt(context: AnalysisContext): String = """
Analyze this English phrase/sentence selected from a larger text.

Selected text: "${context.selectedText}"

Full surrounding text:
${context.fullSnapshotText}

Provide a concise analysis:
1. **Meaning**: What does this phrase/sentence mean in context?
2. **Grammar**: Identify the grammatical structure (e.g., clause type, tense, voice).
3. **Key vocabulary**: Define any notable or difficult words.
4. **Usage note**: How is this phrase typically used? Any idiomatic meaning?

Keep the response concise and helpful for an English language learner.
""".trimIndent()

    private fun sentencePrompt(context: AnalysisContext): String = """
Analyze this English sentence from a larger text. The user long-pressed a word in this sentence.

Sentence: "${context.selectedText}"

Full surrounding text:
${context.fullSnapshotText}

Provide a concise analysis:
1. **Meaning**: What does this sentence mean in plain English?
2. **Grammar**: Break down the sentence structure (subject, verb, objects, clauses).
3. **Key vocabulary**: Define any difficult or important words.
4. **Tone**: Is the sentence formal, casual, technical, etc.?

Keep the response concise and helpful for an English language learner.
""".trimIndent()

    private fun paragraphPrompt(context: AnalysisContext): String = """
Analyze this English paragraph selected from a larger text.

Selected text:
"${context.selectedText}"

Full surrounding text:
${context.fullSnapshotText}

Provide a structured analysis:
1. **Summary**: One-sentence summary of the paragraph's main point.
2. **Key ideas**: Bullet the 2-3 most important ideas.
3. **Difficult vocabulary**: List and briefly define any advanced or unusual words.
4. **Tone & style**: Describe the writing tone (formal, casual, academic, etc.).
5. **Context**: How does this paragraph relate to the surrounding text?

Keep the response concise and helpful for an English language learner.
""".trimIndent()

    private fun fullTextPrompt(context: AnalysisContext): String = """
Provide a comprehensive analysis of this English text.

Full text:
"${context.fullSnapshotText}"

Analyze the following:
1. **Summary**: 2-3 sentence overview of the entire text.
2. **Main themes**: What are the key topics or arguments?
3. **Vocabulary highlights**: List 5-8 notable vocabulary words with brief definitions.
4. **Reading level**: Estimate the difficulty level and target audience.
5. **Structure**: How is the text organized (narrative, expository, argumentative, etc.)?
6. **Key takeaways**: What should a reader remember from this text?

Keep the response well-structured and helpful for an English language learner.
""".trimIndent()

    private fun wordPrompt(context: AnalysisContext): String = """
Explain this English word in the context it appears.

Word: "${context.selectedText}"

Context: ${context.fullSnapshotText}

Provide:
1. **Definition** in this specific context
2. **Part of speech**
3. **Synonyms** (2-3)
4. **Example sentence**
""".trimIndent()

    /** System prompt shared across all scope levels */
    const val SYSTEM_PROMPT = "You are an English language analysis assistant embedded in a camera-based reading app called EigoLens. Your audience is ESL/EFL learners. Be concise, educational, and practical. Use simple English in your explanations. Format responses with markdown (bold, bullet points)."
}
