package com.example.translator.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.translator.model.Language
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "translator_settings"
)

class SettingsRepository(
    private val context: Context
) {

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            apiKey = prefs[API_KEY].orEmpty(),
            baseUrl = prefs[BASE_URL].orEmpty(),
            preferredSummaryLanguage = prefs[SUMMARY_LANGUAGE]
                ?.let { runCatching { Language.valueOf(it) }.getOrNull() }
                ?: Language.CHINESE
        )
    }

    suspend fun updateSettings(apiKey: String, baseUrl: String, summaryLanguage: Language) {
        context.settingsDataStore.edit { prefs ->
            prefs[API_KEY] = apiKey
            prefs[BASE_URL] = baseUrl
            prefs[SUMMARY_LANGUAGE] = summaryLanguage.name
        }
    }

    companion object {
        private val API_KEY = stringPreferencesKey("api_key")
        private val BASE_URL = stringPreferencesKey("base_url")
        private val SUMMARY_LANGUAGE = stringPreferencesKey("summary_language")
    }
}
