package com.example.translator.model

enum class Language(
    val displayName: String,
    val localeTag: String,
    val placeholder: String
) {
    CHINESE("简体中文", "zh-CN", "输入中文或粘贴文本"),
    ENGLISH("English", "en", "Type English text"),
    FRENCH("Français", "fr", "Saisir du texte français");

    companion object {
        fun otherLanguages(source: Language): List<Language> = values().filter { it != source }
    }
}
