package com.example.translator.ui

import com.example.translator.model.Language

data class SettingsUiState(
    val apiKey: String = "",
    val baseUrl: String = "",
    val model: String = "",
    val preferredLanguage: Language = Language.CHINESE,
    val editApiKey: String = "",
    val editBaseUrl: String = "",
    val editModel: String = "",
    val editPreferredLanguage: Language = Language.CHINESE,
    val isDialogVisible: Boolean = false,
    val isSaving: Boolean = false,
    val requiresApiKeySetup: Boolean = false,
    val hasBundledApiKey: Boolean = false,
    val hasShownInitialSetup: Boolean = false,
    val defaultModel: String = "",
    val validationMessage: String? = null
)
