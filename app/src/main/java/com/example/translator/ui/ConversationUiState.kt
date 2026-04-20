package com.example.translator.ui

import com.example.translator.model.ConversationEntry

data class ConversationUiState(
    val isActive: Boolean = false,
    val entries: List<ConversationEntry> = emptyList(),
    val insight: String = "",
    val isSummarizing: Boolean = false,
    val showNotice: Boolean = false,
    val hasShownNotice: Boolean = false
)
