package hu.speeder.huroutes.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
    private val LANGUAGE_ISO_ALPHA_2 = stringPreferencesKey("language_iso_2")

    /// Returns the language synchronously
    val languageIso2: String? get() {
        return runBlocking {
            context.dataStore.data
                .catch {
                    if (it is IOException) {
                        it.printStackTrace()
                        emit(emptyPreferences())
                    } else {
                        throw it
                    }
                }
                .map {
                    // On the first run of the app, we will use LinearLayoutManager by default
                    it[LANGUAGE_ISO_ALPHA_2]
                }
                .first()
        }
    }

    suspend fun saveLanguageToPreferencesStore(languageIso2: String?) {
        context.dataStore.edit { preferences ->
            if (languageIso2 == null)
                preferences.remove(LANGUAGE_ISO_ALPHA_2)
            else
                preferences[LANGUAGE_ISO_ALPHA_2] = languageIso2
        }
    }
}