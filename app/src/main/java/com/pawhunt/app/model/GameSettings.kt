package com.pawhunt.app.model

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "game_settings")

class GameSettingsRepository(private val context: Context) {

    companion object {
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val MUSIC_ENABLED = booleanPreferencesKey("music_enabled")
        val SPEED_MULTIPLIER = floatPreferencesKey("speed_multiplier")
        val PREY_COUNT = intPreferencesKey("prey_count")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val SELECTED_TOY_ID = intPreferencesKey("selected_toy_id")
        val SELECTED_BG_ID = intPreferencesKey("selected_bg_id")
        val IS_PRO = booleanPreferencesKey("is_pro")
        val GAME_EXIT_COUNT = intPreferencesKey("game_exit_count")
    }

    val soundEnabled: Flow<Boolean> = context.dataStore.data.map { it[SOUND_ENABLED] ?: true }
    val musicEnabled: Flow<Boolean> = context.dataStore.data.map { it[MUSIC_ENABLED] ?: true }
    val speedMultiplier: Flow<Float> = context.dataStore.data.map { it[SPEED_MULTIPLIER] ?: 1.0f }
    val preyCount: Flow<Int> = context.dataStore.data.map { it[PREY_COUNT] ?: 1 }
    val keepScreenOn: Flow<Boolean> = context.dataStore.data.map { it[KEEP_SCREEN_ON] ?: true }
    val hapticEnabled: Flow<Boolean> = context.dataStore.data.map { it[HAPTIC_ENABLED] ?: true }
    val selectedToyId: Flow<Int> = context.dataStore.data.map { it[SELECTED_TOY_ID] ?: 4 }
    val selectedBgId: Flow<Int> = context.dataStore.data.map { it[SELECTED_BG_ID] ?: 1 }
    val isPro: Flow<Boolean> = context.dataStore.data.map { it[IS_PRO] ?: false }
    val gameExitCount: Flow<Int> = context.dataStore.data.map { it[GAME_EXIT_COUNT] ?: 0 }

    suspend fun setSoundEnabled(value: Boolean) {
        context.dataStore.edit { it[SOUND_ENABLED] = value }
    }

    suspend fun setMusicEnabled(value: Boolean) {
        context.dataStore.edit { it[MUSIC_ENABLED] = value }
    }

    suspend fun setSpeedMultiplier(value: Float) {
        context.dataStore.edit { it[SPEED_MULTIPLIER] = value }
    }

    suspend fun setPreyCount(value: Int) {
        context.dataStore.edit { it[PREY_COUNT] = value.coerceIn(1, 3) }
    }

    suspend fun setKeepScreenOn(value: Boolean) {
        context.dataStore.edit { it[KEEP_SCREEN_ON] = value }
    }

    suspend fun setHapticEnabled(value: Boolean) {
        context.dataStore.edit { it[HAPTIC_ENABLED] = value }
    }

    suspend fun setSelectedToyId(value: Int) {
        context.dataStore.edit { it[SELECTED_TOY_ID] = value }
    }

    suspend fun setSelectedBgId(value: Int) {
        context.dataStore.edit { it[SELECTED_BG_ID] = value }
    }

    suspend fun setIsPro(value: Boolean) {
        context.dataStore.edit { it[IS_PRO] = value }
    }

    suspend fun incrementGameExitCount(): Int {
        var newCount = 0
        context.dataStore.edit { prefs ->
            val current = prefs[GAME_EXIT_COUNT] ?: 0
            newCount = current + 1
            prefs[GAME_EXIT_COUNT] = newCount
        }
        return newCount
    }
}
