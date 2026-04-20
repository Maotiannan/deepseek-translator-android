package com.example.translator.model

data class ConversationEntry(
    val id: Long = System.currentTimeMillis(),
    val sourceLanguage: Language,
    val sourceText: String,
    val translations: Map<Language, String>,
    val timestamp: Long = System.currentTimeMillis()
)
