package com.jworks.eigolens.ui.capture

sealed class ScopeLevel {
    data class Word(val word: String) : ScopeLevel()
    data class Phrase(val text: String, val words: List<String>) : ScopeLevel()
    data class Sentence(val text: String) : ScopeLevel()
    data class Paragraph(val text: String) : ScopeLevel()
    data object FullSnapshot : ScopeLevel()
}
