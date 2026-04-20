package com.example.translator.data

import com.example.translator.model.ConversationEntry
import com.example.translator.model.Language
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSource
import java.io.IOException

class DeepSeekApi(
    private val client: OkHttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {

    fun streamTranslation(
        sourceLanguage: Language,
        targetLanguage: Language,
        text: String,
        config: ApiConfig
    ) = callbackFlow<String> {
        val payload = DeepSeekRequest(
            model = config.model,
            messages = listOf(
                DeepSeekMessage(
                    role = "system",
                    content = systemPrompt(targetLanguage)
                ),
                DeepSeekMessage(
                    role = "user",
                    content = buildUserPrompt(sourceLanguage, targetLanguage, text)
                )
            ),
            temperature = 0.2,
            stream = true
        )

        val requestBody = json.encodeToString(DeepSeekRequest.serializer(), payload)
            .toRequestBody(JSON_MEDIA_TYPE)

        val request = Request.Builder()
            .url(config.baseUrl)
            .header("Authorization", "Bearer ${config.apiKey}")
            .header("Content-Type", "application/json")
            .post(requestBody)
            .build()

        val call = client.newCall(request)
        val job = launch(Dispatchers.IO) {
            try {
                call.execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("DeepSeek error ${response.code} ${response.message}")
                    }
                    val body = response.body ?: throw IOException("DeepSeek response body is empty")
                    readStream(body.source())
                }
                close()
            } catch (t: Throwable) {
                close(t)
            }
        }

        awaitClose {
            call.cancel()
            job.cancel()
        }
    }

    suspend fun summarizeConversation(
        entries: List<ConversationEntry>,
        preferredLanguage: Language,
        config: ApiConfig
    ): String {
        val content = buildSummaryPrompt(entries, preferredLanguage)
        val payload = DeepSeekRequest(
            model = config.model,
            messages = listOf(
                DeepSeekMessage(
                    role = "system",
                    content = "你是一个对话理解助手，请用${preferredLanguage.displayName}（${preferredLanguage.localeTag}）概括本段对话的语气、情绪和潜台词，限制在300字符内。"
                ),
                DeepSeekMessage(role = "user", content = content)
            ),
            temperature = 0.4,
            stream = false
        )

        val requestBody = json.encodeToString(DeepSeekRequest.serializer(), payload)
            .toRequestBody(JSON_MEDIA_TYPE)

        val request = Request.Builder()
            .url(config.baseUrl)
            .header("Authorization", "Bearer ${config.apiKey}")
            .header("Content-Type", "application/json")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("DeepSeek summary error ${response.code} ${response.message}")
            }
            val body = response.body?.string() ?: ""
            val completion = json.decodeFromString(DeepSeekCompletionResponse.serializer(), body)
            val text = completion.choices.firstOrNull()?.message?.content.orEmpty()
            return text.take(300)
        }
    }

    private fun buildSummaryPrompt(
        entries: List<ConversationEntry>,
        preferredLanguage: Language
    ): String = buildString {
        appendLine("你是对话理解助手。")
        appendLine("规则：")
        appendLine("1. 仅根据每条记录的 speaker_label（中文表达者/英文表达者/法语表达者）理解对应说话者的意图。")
        appendLine("2. translation_for_preferred_language 仅用于辅助理解，不代表新的说话者。")
        appendLine("3. 不要凭空创造未出现的语言或角色。")
        appendLine("4. 输出全部内容使用${preferredLanguage.displayName}，不超过300个字符，突出语气、情绪和潜台词。")
        entries.forEachIndexed { index, entry ->
            appendLine("对话${index + 1} {")
            appendLine("  speaker_label: ${entry.sourceLanguage.speakerLabel()}")
            appendLine("  speaker_language: ${entry.sourceLanguage.displayName}")
            appendLine("  source_text: ${entry.sourceText}")
            entry.translations[preferredLanguage]?.takeIf { it.isNotBlank() }?.let { preferredText ->
                appendLine("  translation_for_preferred_language: $preferredText")
            }
            appendLine("}")
        }
        appendLine("请结合以上记录，生成一段总结。")
    }

    private fun ProducerScope<String>.readStream(source: BufferedSource) {
        while (true) {
            val rawLine = source.readUtf8Line() ?: break
            if (rawLine.isBlank() || !rawLine.startsWith("data:")) continue
            val payload = rawLine.removePrefix("data:").trim()
            if (payload == DONE_TOKEN) break

            val chunk = runCatching {
                json.decodeFromString(DeepSeekStreamChunk.serializer(), payload)
            }.getOrNull() ?: continue

            val delta = chunk.choices.firstOrNull()?.delta?.content.orEmpty()
            if (delta.isNotEmpty()) {
                trySend(delta)
            }
        }
    }

    private fun systemPrompt(targetLanguage: Language): String =
        "You are a translation engine. Translate strictly into ${targetLanguage.displayName} (${targetLanguage.localeTag}) without additional commentary."

    private fun buildUserPrompt(
        sourceLanguage: Language,
        targetLanguage: Language,
        text: String
    ): String = buildString {
        appendLine("Source language: ${sourceLanguage.displayName} (${sourceLanguage.localeTag}).")
        appendLine("Target language: ${targetLanguage.displayName} (${targetLanguage.localeTag}).")
        appendLine("Text: $text")
    }

    companion object {
        private const val DONE_TOKEN = "[DONE]"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

@Serializable
private data class DeepSeekRequest(
    val model: String,
    val messages: List<DeepSeekMessage>,
    val temperature: Double,
    val stream: Boolean
)

@Serializable
private data class DeepSeekMessage(
    val role: String,
    val content: String
)

@Serializable
private data class DeepSeekStreamChunk(
    val choices: List<DeepSeekChoice> = emptyList()
)

@Serializable
private data class DeepSeekChoice(
    val delta: DeepSeekDelta? = null
)

@Serializable
private data class DeepSeekDelta(
    val content: String? = null
)

@Serializable
private data class DeepSeekCompletionResponse(
    val choices: List<DeepSeekCompletionChoice> = emptyList()
)

@Serializable
private data class DeepSeekCompletionChoice(
    val message: DeepSeekMessage? = null
)

private fun Language.speakerLabel(): String = when (this) {
    Language.CHINESE -> "中文表达者"
    Language.ENGLISH -> "英文表达者"
    Language.FRENCH -> "法语表达者"
}
