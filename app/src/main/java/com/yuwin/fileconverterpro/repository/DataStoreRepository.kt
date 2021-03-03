package com.yuwin.fileconverterpro.repository

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.yuwin.fileconverterpro.misc.UiMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException


const val PREFERENCE_NAME = "app_preference"

val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(PREFERENCE_NAME)

class DataStoreRepository(val context: Application) {

    private object PreferencesKeys {
        val UiMode = intPreferencesKey("ui_mode")
    }


    suspend fun setUiMode(mode: UiMode) {
        context.appDataStore.edit { settings ->
            settings[PreferencesKeys.UiMode] = mode.ordinal
        }
    }

    val uiMode: Flow<Int> = context.appDataStore.data
        .catch { exception ->
            if(exception is IOException) {
                Log.d("settings", exception.message.toString())
                emit(emptyPreferences())
            }else {
                throw exception
            }
        }.map { preferences ->
            val uiMode = preferences[PreferencesKeys.UiMode] ?: 0
            uiMode
        }


}