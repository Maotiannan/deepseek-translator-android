package com.example.translator.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.translator.BuildConfig
import com.example.translator.data.ApiConfig
import com.example.translator.data.DeepSeekApi
import com.example.translator.data.DeepSeekTranslationRepository
import com.example.translator.data.SettingsRepository
import com.example.translator.data.TranslationRepository
import com.example.translator.model.ConversationEntry
import com.example.translator.model.Language
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

class TranslatorViewModel(
    private val repository: TranslationRepository,
    private val settingsRepository: SettingsRepository,
    private val api: DeepSeekApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(TranslatorUiState())
    val uiState: StateFlow<TranslatorUiState> = _uiState.asStateFlow()

    private val _settingsUiState = MutableStateFlow(SettingsUiState())
    val settingsUiState: StateFlow<SettingsUiState> = _settingsUiState.asStateFlow()

    private val _conversationState = MutableStateFlow(ConversationUiState())
    val conversationState: StateFlow<ConversationUiState> = _conversationState.asStateFlow()

    private var streamingJob: Job? = null

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                val hasBundledApiKey = BuildConfig.DEEPSEEK_API_KEY.isNotBlank()
                val requiresApiKeySetup = settings.apiKey.isBlank() && !hasBundledApiKey
                val shouldPromptInitialSetup = requiresApiKeySetup && !_settingsUiState.value.hasShownInitialSetup
                _settingsUiState.update { state ->
                    state.copy(
                        apiKey = settings.apiKey,
                        baseUrl = settings.baseUrl,
                        model = settings.model,
                        preferredLanguage = settings.preferredSummaryLanguage,
                        editApiKey = if (state.isDialogVisible) state.editApiKey else settings.apiKey,
                        editBaseUrl = if (state.isDialogVisible) state.editBaseUrl else settings.baseUrl,
                        editModel = if (state.isDialogVisible) state.editModel else settings.model,
                        editPreferredLanguage = if (state.isDialogVisible) state.editPreferredLanguage else settings.preferredSummaryLanguage,
                        requiresApiKeySetup = requiresApiKeySetup,
                        hasBundledApiKey = hasBundledApiKey,
                        isDialogVisible = state.isDialogVisible || shouldPromptInitialSetup,
                        hasShownInitialSetup = state.hasShownInitialSetup || shouldPromptInitialSetup,
                        defaultModel = BuildConfig.DEEPSEEK_MODEL
                    )
                }
            }
        }
    }

    fun onTextChanged(language: Language, value: String) {
        _uiState.update { state ->
            state.copyWith(language, value)
                .copy(activeLanguage = language, errorMessage = null)
        }
    }

    fun translateActiveLanguage() {
        val state = _uiState.value
        val sourceLanguage = state.activeLanguage
        val text = state.textOf(sourceLanguage)

        streamingJob?.cancel()

        if (text.isBlank()) {
            _uiState.update {
                it.copy(
                    isTranslating = false,
                    errorMessage = "请输入内容后再点击“开始翻译”。"
                )
            }
            return
        }

        val config = runCatching { resolveApiConfig() }.getOrElse { error ->
            openSettings()
            _uiState.update {
                it.copy(
                    isTranslating = false,
                    errorMessage = error.message ?: "请先完成 API 配置。"
                )
            }
            return
        }

        streamingJob = viewModelScope.launch {
            _uiState.update {
                it.clearTargets(sourceLanguage)
                    .copy(isTranslating = true, errorMessage = null)
            }
            var translationSucceeded = false
            try {
                repository.streamTranslations(sourceLanguage, text, config).collect { update ->
                    _uiState.update { current ->
                        current.copyWith(update.language, update.content)
                    }
                }
                translationSucceeded = true
            } catch (c: CancellationException) {
                return@launch
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        isTranslating = false,
                        errorMessage = t.message ?: "翻译失败，请稍后重试。"
                    )
                }
            } finally {
                _uiState.update { it.copy(isTranslating = false) }
                if (translationSucceeded) {
                    handleConversationLogging(sourceLanguage)
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearAllTexts() {
        streamingJob?.cancel()
        _uiState.update {
            it.copy(
                chineseText = "",
                englishText = "",
                frenchText = "",
                isTranslating = false,
                errorMessage = null
            )
        }
    }

    fun openSettings() {
        _settingsUiState.update { state ->
            state.copy(
                isDialogVisible = true,
                editApiKey = state.apiKey,
                editBaseUrl = state.baseUrl,
                editModel = state.model,
                editPreferredLanguage = state.preferredLanguage,
                hasShownInitialSetup = true,
                validationMessage = null
            )
        }
    }

    fun dismissSettings() {
        _settingsUiState.update {
            it.copy(
                isDialogVisible = false,
                hasShownInitialSetup = true,
                validationMessage = null
            )
        }
    }

    fun onSettingsApiKeyChange(value: String) {
        _settingsUiState.update { it.copy(editApiKey = value, validationMessage = null) }
    }

    fun onSettingsBaseUrlChange(value: String) {
        _settingsUiState.update { it.copy(editBaseUrl = value, validationMessage = null) }
    }

    fun onSettingsModelChange(value: String) {
        _settingsUiState.update { it.copy(editModel = value, validationMessage = null) }
    }

    fun onSettingsPreferredLanguageChange(language: Language) {
        _settingsUiState.update { it.copy(editPreferredLanguage = language) }
    }

    fun saveSettings() {
        val drafts = _settingsUiState.value
        if (drafts.isSaving) return
        val apiKey = drafts.editApiKey.trim()
        if (apiKey.isBlank() && !drafts.hasBundledApiKey) {
            _settingsUiState.update {
                it.copy(validationMessage = "请先填写 DeepSeek API Key，保存后本机后续打开即可直接使用。")
            }
            return
        }

        viewModelScope.launch {
            _settingsUiState.update { it.copy(isSaving = true) }
            try {
                settingsRepository.updateSettings(
                    apiKey = apiKey,
                    baseUrl = drafts.editBaseUrl.trim(),
                    model = drafts.editModel.trim(),
                    summaryLanguage = drafts.editPreferredLanguage
                )
                _settingsUiState.update {
                    it.copy(
                        isSaving = false,
                        isDialogVisible = false,
                        validationMessage = null
                    )
                }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        errorMessage = "保存设置失败：${t.message ?: "未知错误"}"
                    )
                }
                _settingsUiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun enterConversationMode() {
        _conversationState.update { state ->
            state.copy(
                isActive = true,
                showNotice = !state.hasShownNotice
            )
        }
    }

    fun exitConversationMode() {
        _conversationState.update { it.copy(isActive = false) }
    }

    fun resetConversation() {
        _conversationState.update {
            it.copy(
                entries = emptyList(),
                insight = "",
                isSummarizing = false
            )
        }
    }

    fun acknowledgeConversationNotice() {
        _conversationState.update { it.copy(showNotice = false, hasShownNotice = true) }
    }

    private fun handleConversationLogging(sourceLanguage: Language) {
        if (!_conversationState.value.isActive) return
        val snapshot = _uiState.value
        val entry = ConversationEntry(
            sourceLanguage = sourceLanguage,
            sourceText = snapshot.textOf(sourceLanguage),
            translations = mapOf(
                Language.CHINESE to snapshot.chineseText,
                Language.ENGLISH to snapshot.englishText,
                Language.FRENCH to snapshot.frenchText
            )
        )
        val updatedEntries = _conversationState.value.entries + entry
        _conversationState.update { it.copy(entries = updatedEntries) }
        viewModelScope.launch { generateConversationInsight(updatedEntries) }
    }

    private suspend fun generateConversationInsight(entries: List<ConversationEntry>) {
        if (!_conversationState.value.isActive) return
        val config = runCatching { resolveApiConfig() }.getOrElse { error ->
            _uiState.update {
                it.copy(
                    errorMessage = error.message ?: "请先完成 API 配置。"
                )
            }
            _conversationState.update { it.copy(isSummarizing = false) }
            return
        }
        val preferred = _settingsUiState.value.preferredLanguage
        _conversationState.update { it.copy(isSummarizing = true) }
        try {
            val limitedEntries = entries.takeLast(8)
            val insight = withContext(Dispatchers.IO) {
                api.summarizeConversation(limitedEntries, preferred, config)
            }
            _conversationState.update {
                it.copy(insight = insight, isSummarizing = false)
            }
        } catch (t: Throwable) {
            _uiState.update {
                it.copy(
                    errorMessage = t.message ?: "生成对话理解失败。"
                )
            }
            _conversationState.update { it.copy(isSummarizing = false) }
        }
    }

    private fun resolveApiConfig(): ApiConfig {
        val settings = _settingsUiState.value
        val apiKey = settings.apiKey.ifBlank { BuildConfig.DEEPSEEK_API_KEY }
        val baseUrl = settings.baseUrl.ifBlank { BuildConfig.DEEPSEEK_BASE_URL }
        val model = settings.model.ifBlank { BuildConfig.DEEPSEEK_MODEL }
        check(apiKey.isNotBlank()) {
            "请先在设置中填写 DeepSeek API Key。保存后会保存在当前设备，本机后续打开即可直接使用。"
        }
        check(baseUrl.isNotBlank()) {
            "DeepSeek Base URL 未配置。"
        }
        check(model.isNotBlank()) {
            "DeepSeek Model 未配置。"
        }
        return ApiConfig(
            apiKey = apiKey,
            baseUrl = baseUrl,
            model = model
        )
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as Application
                val context = application.applicationContext
                val client = OkHttpClient.Builder().build()
                val settingsRepository = SettingsRepository(context)
                val api = DeepSeekApi(client)

                TranslatorViewModel(
                    repository = DeepSeekTranslationRepository(api),
                    settingsRepository = settingsRepository,
                    api = api
                )
            }
        }
    }
}

private fun TranslatorUiState.clearTargets(source: Language): TranslatorUiState {
    var updated = copy(isTranslating = false)
    Language.otherLanguages(source).forEach { language ->
        updated = updated.copyWith(language, "")
    }
    return updated
}
