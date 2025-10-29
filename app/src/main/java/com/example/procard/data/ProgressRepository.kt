package com.example.procard.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.procard.model.DayColor
import com.example.procard.model.ProgressSnapshot
import com.example.procard.model.ProgressStage
import com.example.procard.model.storageKey
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.progressDataStore by preferencesDataStore(name = "progress_preferences")

class ProgressRepository(private val context: Context) {

    private object Keys {
        val STAGE = stringPreferencesKey("stage")
        const val STATUS_PREFIX = "status_"
        const val WEIGHT_PREFIX = "weight_"
        const val NOTE_PREFIX = "note_"
    }

    private val dataStore get() = context.progressDataStore

    fun observe(): Flow<ProgressSnapshot> = dataStore.data.map { prefs -> toSnapshot(prefs) }

    suspend fun setStage(stage: ProgressStage) {
        dataStore.edit { prefs ->
            prefs[Keys.STAGE] = stage.name
        }
    }

    suspend fun updateDayStatus(date: LocalDate, status: DayColor) {
        val key = stringPreferencesKey(Keys.STATUS_PREFIX + date.storageKey())
        dataStore.edit { prefs -> prefs[key] = status.name }
    }

    suspend fun updateWeight(date: LocalDate, weight: Double?) {
        val key = doublePreferencesKey(Keys.WEIGHT_PREFIX + date.storageKey())
        dataStore.edit { prefs ->
            if (weight != null) {
                prefs[key] = weight
            } else {
                prefs.remove(key)
            }
        }
    }

    suspend fun updateNote(date: LocalDate, note: String?) {
        val key = stringPreferencesKey(Keys.NOTE_PREFIX + date.storageKey())
        dataStore.edit { prefs ->
            if (note.isNullOrBlank()) {
                prefs.remove(key)
            } else {
                prefs[key] = note.trim()
            }
        }
    }

    private fun toSnapshot(prefs: Preferences): ProgressSnapshot {
        val stage = ProgressStage.fromName(prefs[Keys.STAGE])
        val statuses = mutableMapOf<LocalDate, DayColor>()
        val weights = mutableMapOf<LocalDate, Double>()
        val notes = mutableMapOf<LocalDate, String>()

        prefs.asMap().forEach { (key, value) ->
            val name = key.name
            when {
                name.startsWith(Keys.STATUS_PREFIX) -> {
                    val date = runCatching { LocalDate.parse(name.removePrefix(Keys.STATUS_PREFIX)) }.getOrNull()
                    if (date != null) {
                        statuses[date] = DayColor.fromName(value as? String)
                    }
                }
                name.startsWith(Keys.WEIGHT_PREFIX) -> {
                    val date = runCatching { LocalDate.parse(name.removePrefix(Keys.WEIGHT_PREFIX)) }.getOrNull()
                    val weight = (value as? Double)
                    if (date != null && weight != null) {
                        weights[date] = weight
                    }
                }
                name.startsWith(Keys.NOTE_PREFIX) -> {
                    val date = runCatching { LocalDate.parse(name.removePrefix(Keys.NOTE_PREFIX)) }.getOrNull()
                    val note = value as? String
                    if (date != null && !note.isNullOrBlank()) {
                        notes[date] = note
                    }
                }
            }
        }

        return ProgressSnapshot(
            stage = stage,
            dayStatuses = statuses,
            weights = weights,
            notes = notes
        )
    }
}
