package com.jworks.eigolens.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jworks.eigolens.data.local.entities.BookmarkedWordEntity
import com.jworks.eigolens.data.local.entities.LookupHistoryEntity
import com.jworks.eigolens.data.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    val recentHistory: StateFlow<List<LookupHistoryEntity>> = historyRepository.getRecentHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarks: StateFlow<List<BookmarkedWordEntity>> = historyRepository.getAllBookmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val historyCount: StateFlow<Int> = historyRepository.getHistoryCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val bookmarkCount: StateFlow<Int> = historyRepository.getBookmarkCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun clearHistory() {
        viewModelScope.launch { historyRepository.clearHistory() }
    }

    fun removeBookmark(word: String) {
        viewModelScope.launch { historyRepository.removeBookmark(word) }
    }
}
