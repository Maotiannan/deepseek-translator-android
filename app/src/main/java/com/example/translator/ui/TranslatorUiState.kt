package com.example.translator.ui

import com.example.translator.model.Language

data class TranslatorUiState(
    val chineseText: String = "",
    val englishText: String = "",
    val frenchText: String = "",
    val activeLanguage: Language = Language.CHINESE,
    val isTranslating: Boolean = false,
    val errorMessage: String? = null
) {
    fun textOf(language: Language): String = when (language) {
        Language.CHINESE -> chineseText
        Language.ENGLISH -> englishText
        Language.FRENCH -> frenchText
    }

    fun copyWith(language: Language, value: String): TranslatorUiState = when (language) {
        Language.CHINESE -> copy(chineseText = value)
        Language.ENGLISH -> copy(englishText = value)
        Language.FRENCH -> copy(frenchText = value)
    }
}
