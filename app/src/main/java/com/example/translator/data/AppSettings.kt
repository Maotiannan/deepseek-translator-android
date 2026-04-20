package com.example.translator.data

import com.example.translator.model.Language

data class AppSettings(
    val apiKey: String = "",
    val baseUrl: String = "",
    val preferredSummaryLanguage: Language = Language.CHINESE
)

data class ApiConfig(
    val apiKey: String,
    val baseUrl: String,
    val model: String
)
