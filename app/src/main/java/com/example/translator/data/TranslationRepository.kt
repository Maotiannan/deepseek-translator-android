package com.example.translator.data

import com.example.translator.model.Language
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

interface TranslationRepository {
    fun streamTranslations(
        sourceLanguage: Language,
        text: String,
        config: ApiConfig
    ): Flow<TranslationUpdate>
}

class DeepSeekTranslationRepository(
    private val api: DeepSeekApi
) : TranslationRepository {

    override fun streamTranslations(
        sourceLanguage: Language,
        text: String,
        config: ApiConfig
    ): Flow<TranslationUpdate> = channelFlow {
        val targets = Language.otherLanguages(sourceLanguage)
        val jobs = targets.map { target ->
            launch {
                var aggregated = ""
                try {
                    api.streamTranslation(sourceLanguage, target, text, config).collect { chunk ->
                        aggregated += chunk
                        send(
                            TranslationUpdate(
                                language = target,
                                content = aggregated,
                                isFinalChunk = false
                            )
                        )
                    }
                } finally {
                    send(
                        TranslationUpdate(
                            language = target,
                            content = aggregated,
                            isFinalChunk = true
                        )
                    )
                }
            }
        }

        launch {
            jobs.joinAll()
            close()
        }

        awaitClose { jobs.forEach { it.cancel() } }
    }
}

data class TranslationUpdate(
    val language: Language,
    val content: String,
    val isFinalChunk: Boolean
)
