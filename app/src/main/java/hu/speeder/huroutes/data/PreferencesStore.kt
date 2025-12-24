package hu.speeder.huroutes.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.io.IOException

private const val LANGUAGE_PREFERENCES_NAME = "language_preferences"

// Create a DataStore instance using the preferencesDataStore delegate, with the Context as
// receiver.
private val Context.dataStore : DataStore<Preferences> by preferencesDataStore(
    name = LANGUAGE_PREFERENCES_NAME
)

class PreferencesStore(private val context: Context) {
    companion object {
        private val TAB_POSITION = stringPreferencesKey("tab_position")
        private val LANGUAGE_ISO_ALPHA_2 = stringPreferencesKey("language_iso_2")
    }

    /// Sets or returns the last tab ID synchronously
    var lastTabPosition: Int?
        get() {
            return runBlocking {
                context.dataStore.data
                    .catch { handleIOException(it) }
                    .map { it[TAB_POSITION]?.toIntOrNull() }
                    .first()
            }
        }
        set(tabPosition: Int?) {
            runBlocking {
                context.dataStore.edit { preferences ->
                    if (tabPosition == null)
                        preferences.remove(TAB_POSITION)
                    else
                        preferences[TAB_POSITION] = tabPosition.toString()
                }
            }
        }

    /// Sets or returns the language synchronously as 2-letter ISO
    var languageIso2: String?
        get() {
            return runBlocking {
                context.dataStore.data
                    .catch { handleIOException(it) }
                    .map { it[LANGUAGE_ISO_ALPHA_2] }
                    .first()
            }
        }
        set(languageIso2: String?) {
            runBlocking {
                context.dataStore.edit { preferences ->
                    if (languageIso2 == null)
                        preferences.remove(LANGUAGE_ISO_ALPHA_2)
                    else
                        preferences[LANGUAGE_ISO_ALPHA_2] = languageIso2
                }
            }
        }

    // Handle empty preferences silently
    private suspend fun FlowCollector<Preferences>.handleIOException(cause: Throwable) {
        if (cause is IOException) {
            cause.printStackTrace()
            emit(emptyPreferences())
        } else {
            throw cause
        }
    }

}