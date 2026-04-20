package com.example.translator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.translator.model.ConversationEntry
import com.example.translator.model.Language
import com.example.translator.ui.ConversationUiState
import com.example.translator.ui.TranslatorUiState
import com.example.translator.ui.TranslatorViewModel
import com.example.translator.ui.SettingsUiState
import com.example.translator.ui.theme.TranslatorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TranslatorApp()
        }
    }
}

@Composable
fun TranslatorApp() {
    TranslatorTheme {
        val viewModel: TranslatorViewModel = viewModel(factory = TranslatorViewModel.Factory)
        val uiState by viewModel.uiState.collectAsState()
        val settingsUiState by viewModel.settingsUiState.collectAsState()
        val conversationUiState by viewModel.conversationState.collectAsState()

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (conversationUiState.isActive) {
                ConversationScreen(
                    uiState = uiState,
                    conversationState = conversationUiState,
                    settingsUiState = settingsUiState,
                    onValueChange = viewModel::onTextChanged,
                    onTranslate = viewModel::translateActiveLanguage,
                    onClearAll = viewModel::clearAllTexts,
                    onExitConversation = viewModel::exitConversationMode,
                    onResetConversation = viewModel::resetConversation,
                    onAcknowledgeNotice = viewModel::acknowledgeConversationNotice,
                    onSettingsClick = viewModel::openSettings,
                    onSettingsDismiss = viewModel::dismissSettings,
                    onSettingsApiKeyChange = viewModel::onSettingsApiKeyChange,
                    onSettingsBaseUrlChange = viewModel::onSettingsBaseUrlChange,
                    onSettingsPreferredLanguageChange = viewModel::onSettingsPreferredLanguageChange,
                    onSaveSettings = viewModel::saveSettings,
                    onDismissError = viewModel::dismissError
                )
            } else {
                TranslatorScreen(
                    uiState = uiState,
                    settingsUiState = settingsUiState,
                    onValueChange = viewModel::onTextChanged,
                    onDismissError = viewModel::dismissError,
                    onTranslate = viewModel::translateActiveLanguage,
                    onClearAll = viewModel::clearAllTexts,
                    onEnterConversation = viewModel::enterConversationMode,
                    onSettingsClick = viewModel::openSettings,
                    onSettingsDismiss = viewModel::dismissSettings,
                    onSettingsApiKeyChange = viewModel::onSettingsApiKeyChange,
                    onSettingsBaseUrlChange = viewModel::onSettingsBaseUrlChange,
                    onSettingsPreferredLanguageChange = viewModel::onSettingsPreferredLanguageChange,
                    onSaveSettings = viewModel::saveSettings
                )
            }
        }
    }
}

@Composable
fun TranslatorScreen(
    uiState: TranslatorUiState,
    settingsUiState: SettingsUiState,
    onValueChange: (Language, String) -> Unit,
    onDismissError: () -> Unit,
    onTranslate: () -> Unit,
    onClearAll: () -> Unit,
    onEnterConversation: () -> Unit,
    onSettingsClick: () -> Unit,
    onSettingsDismiss: () -> Unit,
    onSettingsApiKeyChange: (String) -> Unit,
    onSettingsBaseUrlChange: (String) -> Unit,
    onSettingsPreferredLanguageChange: (Language) -> Unit,
    onSaveSettings: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI 翻译",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                TextButton(onClick = onSettingsClick) {
                    Text(text = "设置")
                }
            }
            if (settingsUiState.requiresApiKeySetup ||
                (settingsUiState.hasBundledApiKey && settingsUiState.apiKey.isBlank())
            ) {
                ApiSetupCard(
                    hasBundledApiKey = settingsUiState.hasBundledApiKey,
                    onSettingsClick = onSettingsClick
                )
            }
            TranslationActions(
                isTranslating = uiState.isTranslating,
                isConfigReady = !settingsUiState.requiresApiKeySetup,
                onTranslate = onTranslate,
                onClearAll = onClearAll
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
        ) {
            if (uiState.isTranslating) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Language.CHINESE.renderField(uiState, onValueChange)
            Spacer(modifier = Modifier.height(16.dp))
            Language.ENGLISH.renderField(uiState, onValueChange)
            Spacer(modifier = Modifier.height(16.dp))
            Language.FRENCH.renderField(uiState, onValueChange)

            Spacer(modifier = Modifier.height(16.dp))
            uiState.errorMessage?.let { message ->
                ErrorMessageCard(
                    message = message,
                    onDismiss = onDismissError
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = onEnterConversation,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "对话碎碎念")
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        SettingsDialog(
            state = settingsUiState,
            onDismiss = onSettingsDismiss,
            onApiKeyChange = onSettingsApiKeyChange,
            onBaseUrlChange = onSettingsBaseUrlChange,
            onPreferredLanguageChange = onSettingsPreferredLanguageChange,
            onSave = onSaveSettings
        )
    }
}

@Composable
private fun Language.renderField(
    uiState: TranslatorUiState,
    onValueChange: (Language, String) -> Unit
) {
    val language = this
    val isActive = uiState.activeLanguage == this
    val clipboard = LocalClipboardManager.current
    val textValue = uiState.textOf(language)
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 6.dp else 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                )
                TextButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(textValue))
                    },
                    enabled = textValue.isNotBlank()
                ) {
                    Text(text = "复制")
                }
            }

            TextField(
                value = textValue,
                onValueChange = { onValueChange(language, it) },
                placeholder = { Text(placeholder) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                )
            )

            if (isActive && uiState.isTranslating) {
                Text(
                    text = "正在翻译成其他语言...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ErrorMessageCard(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "翻译出现问题",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onDismiss) {
                Text(text = "我知道了")
            }
        }
    }
}

@Composable
private fun ApiSetupCard(
    hasBundledApiKey: Boolean,
    onSettingsClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "先完成 API 配置",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = if (hasBundledApiKey) {
                    "当前仍可使用打包内默认配置，也可以在设置里保存你自己的 API Key。"
                } else {
                    "首次使用需要在应用内保存 DeepSeek API Key。保存一次后，这台设备后续直接打开即可使用。"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Button(
                onClick = onSettingsClick,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(text = "去设置")
            }
        }
    }
}

@Composable
private fun ConversationInsightCard(
    insight: String,
    isSummarizing: Boolean,
    preferredLanguage: Language
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "AI 碎碎念（${preferredLanguage.displayName}）",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            val body = when {
                isSummarizing -> "AI 正在理解这段对话..."
                insight.isBlank() -> "暂无对话理解，请继续翻译以便 AI 梳理语气和情绪。"
                else -> insight
            }
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun ConversationHistoryList(entries: List<ConversationEntry>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "对话记录",
            style = MaterialTheme.typography.titleMedium
        )
        if (entries.isEmpty()) {
            Text(
                text = "暂无记录，开始翻译即可自动添加。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            entries.reversed().forEach { entry ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${entry.sourceLanguage.displayName} 输入：${entry.sourceText}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Language.values().filter { it != entry.sourceLanguage }.forEach { lang ->
                            entry.translations[lang]?.takeIf { it.isNotBlank() }?.let { translated ->
                                Text(
                                    text = "${lang.displayName} 译文：$translated",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationNoticeBanner(onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "请确认是同一个场景的连续对话",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text(text = "我知道了")
            }
        }
    }
}

@Composable
private fun SettingsDialog(
    state: SettingsUiState,
    onDismiss: () -> Unit,
    onApiKeyChange: (String) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onPreferredLanguageChange: (Language) -> Unit,
    onSave: () -> Unit
) {
    if (!state.isDialogVisible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onSave, enabled = !state.isSaving) {
                Text(text = if (state.isSaving) "保存中..." else "保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = if (state.requiresApiKeySetup) "稍后" else "取消")
            }
        },
        title = { Text(text = if (state.requiresApiKeySetup) "首次配置" else "设置") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = when {
                        state.requiresApiKeySetup ->
                            "请先填写 DeepSeek API Key。配置会保存在当前设备，本机后续直接打开即可使用。"
                        state.hasBundledApiKey && state.apiKey.isBlank() ->
                            "当前仍在使用打包默认配置，也可以在这里保存你自己的 API Key。"
                        else ->
                            "API Key 会保存在当前设备。Base URL 留空时继续使用默认 DeepSeek 接口。"
                    },
                    style = MaterialTheme.typography.bodySmall
                )
                TextField(
                    value = state.editApiKey,
                    onValueChange = onApiKeyChange,
                    label = { Text("API Key") },
                    placeholder = { Text("请输入 DeepSeek API Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                TextField(
                    value = state.editBaseUrl,
                    onValueChange = onBaseUrlChange,
                    label = { Text("Base URL") },
                    placeholder = { Text("默认：https://api.deepseek.com/chat/completions") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                state.validationMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Text(
                    text = "碎碎念偏好语言",
                    style = MaterialTheme.typography.bodySmall
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Language.values().forEach { language ->
                        FilterChip(
                            selected = state.editPreferredLanguage == language,
                            onClick = { onPreferredLanguageChange(language) },
                            label = { Text(language.displayName) }
                        )
                    }
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun TranslatorPreview() {
    TranslatorTheme {
        TranslatorScreen(
            uiState = TranslatorUiState(
                chineseText = "你好，世界！",
                englishText = "Hello, world!",
                frenchText = "Bonjour le monde!",
                isTranslating = false
            ),
            settingsUiState = SettingsUiState(),
            onValueChange = { _, _ -> },
            onDismissError = {},
            onTranslate = {},
            onClearAll = {},
            onEnterConversation = {},
            onSettingsClick = {},
            onSettingsDismiss = {},
            onSettingsApiKeyChange = {},
            onSettingsBaseUrlChange = {},
            onSettingsPreferredLanguageChange = {},
            onSaveSettings = {}
        )
    }
}
@Composable
private fun TranslationActions(
    isTranslating: Boolean,
    isConfigReady: Boolean,
    onTranslate: () -> Unit,
    onClearAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onTranslate,
            enabled = !isTranslating && isConfigReady,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = when {
                    isTranslating -> "翻译中..."
                    isConfigReady -> "开始翻译"
                    else -> "先完成设置"
                }
            )
        }

        TextButton(
            onClick = onClearAll,
            enabled = !isTranslating,
            modifier = Modifier.weight(1f)
        ) {
            Text(text = "清空全部")
        }
    }
}
@Composable
fun ConversationScreen(
    uiState: TranslatorUiState,
    conversationState: ConversationUiState,
    settingsUiState: SettingsUiState,
    onValueChange: (Language, String) -> Unit,
    onTranslate: () -> Unit,
    onClearAll: () -> Unit,
    onExitConversation: () -> Unit,
    onResetConversation: () -> Unit,
    onAcknowledgeNotice: () -> Unit,
    onSettingsClick: () -> Unit,
    onSettingsDismiss: () -> Unit,
    onSettingsApiKeyChange: (String) -> Unit,
    onSettingsBaseUrlChange: (String) -> Unit,
    onSettingsPreferredLanguageChange: (Language) -> Unit,
    onSaveSettings: () -> Unit,
    onDismissError: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onExitConversation) {
                    Text(text = "退出")
                }
                Text(
                    text = "对话碎碎念",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onResetConversation) {
                        Text(text = "新建对话")
                    }
                    TextButton(onClick = onSettingsClick) {
                        Text(text = "设置")
                    }
                }
            }
            if (settingsUiState.requiresApiKeySetup ||
                (settingsUiState.hasBundledApiKey && settingsUiState.apiKey.isBlank())
            ) {
                ApiSetupCard(
                    hasBundledApiKey = settingsUiState.hasBundledApiKey,
                    onSettingsClick = onSettingsClick
                )
            }
            TranslationActions(
                isTranslating = uiState.isTranslating,
                isConfigReady = !settingsUiState.requiresApiKeySetup,
                onTranslate = onTranslate,
                onClearAll = onClearAll
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (conversationState.showNotice) {
                ConversationNoticeBanner(onDismiss = onAcknowledgeNotice)
            }
            if (uiState.isTranslating) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Language.CHINESE.renderField(uiState, onValueChange)
            Language.ENGLISH.renderField(uiState, onValueChange)
            Language.FRENCH.renderField(uiState, onValueChange)

            ConversationInsightCard(
                insight = conversationState.insight,
                isSummarizing = conversationState.isSummarizing,
                preferredLanguage = settingsUiState.preferredLanguage
            )

            ConversationHistoryList(entries = conversationState.entries)

            uiState.errorMessage?.let { message ->
                ErrorMessageCard(
                    message = message,
                    onDismiss = onDismissError
                )
            }
        }

        SettingsDialog(
            state = settingsUiState,
            onDismiss = onSettingsDismiss,
            onApiKeyChange = onSettingsApiKeyChange,
            onBaseUrlChange = onSettingsBaseUrlChange,
            onPreferredLanguageChange = onSettingsPreferredLanguageChange,
            onSave = onSaveSettings
        )
    }
}
